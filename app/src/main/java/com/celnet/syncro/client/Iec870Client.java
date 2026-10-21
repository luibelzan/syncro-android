package com.celnet.syncro.client;

import com.celnet.syncro.models.iec870.cierres.Iec870BillingRecord;
import com.celnet.syncro.models.iec870.curvas.Iec870LoadProfileRow;
import com.celnet.syncro.models.iec870.eventos.Iec870Event;
import com.celnet.syncro.models.iec870.eventos.Iec870Events;
import com.celnet.syncro.models.iec870.instantValues.Iec870InstantValues;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Cliente IEC 60870-5-102 (REE) sobre un par de streams (Bluetooth SPP o TCP).
 * Independiente de Android: solo usa InputStream/OutputStream.
 *
 * Secuencia (idéntica a la del software funcional):
 *   estado enlace -> reposición -> estado enlace -> sondeo clase 2 ->
 *   clave (ASDU 183) -> [lecturas] -> fin de sesión (ASDU 187)
 */
public class Iec870Client {

    public interface Trace {
        void line(String text);
    }

    public enum Periodo {
        HORARIA(11),
        CUARTOHORARIA(12);

        public final int registro;

        Periodo(int registro) {
            this.registro = registro;
        }
    }

    // Tipos de ASDU
    private static final int TYPE_READ_ID = 100;
    private static final int TYPE_ID_RESPONSE = 71;
    private static final int TYPE_READ_INCREMENTAL = 123;
    private static final int TYPE_INCREMENTAL = 11;
    private static final int TYPE_READ_EVENTS = 102;
    private static final int TYPE_EVENT = 1;
    // Ampliación del protocolo (ASDU privados): configuración del registrador
    // (156/157) y valores instantáneos (162/163)
    private static final int TYPE_READ_RM2 = 156;
    private static final int TYPE_RM2 = 157;
    private static final int TYPE_READ_INSTANT = 162;
    private static final int TYPE_INSTANT = 163;
    private static final int OBJ_TRAFO_RATIO = 0xC6;   // 198: relación de transformación
    private static final int OBJ_TOTALS = 0xC0;        // 192: totalizadores de energía
    private static final int OBJ_POWERS = 0xC1;        // 193: potencias
    private static final int OBJ_VOLT_CURR = 0xC2;     // 194: tensiones e intensidades
    private static final int LEN_TOTALS = 29;          // 6 x uint32 + TimeA
    private static final int LEN_POWERS = 37;          // 4 x 8 octetos + TimeA
    private static final int LEN_VOLT_CURR = 26;       // 3 x 7 octetos + TimeA
    // Tarificación: lectura de valores memorizados (134) y respuesta (136)
    private static final int TYPE_READ_BILLING_STORED = 134;
    private static final int TYPE_BILLING_STORED = 136;
    // Valores en curso: lectura (133) y respuesta (135), mismo registro de 63 octetos
    private static final int TYPE_READ_BILLING_CURRENT = 133;
    private static final int TYPE_BILLING_CURRENT = 135;
    private static final int LEN_BILLING = 63;
    private static final int BILLING_FIRST_ADDRESS = 0x14; // tarifa 0 (total)
    private static final int TYPE_AUTH = 183;
    private static final int TYPE_END_SESSION = 187;

    private final InputStream in;
    private final OutputStream out;
    private final int linkAddress;
    private final int measuringPoint;
    private final long password;

    private int fcb = 0;
    private Trace trace;

    private long frameTimeoutMs = 10_000;
    private long contentTimeoutMs = 120_000;
    private int maxEmptyPolls = 200;

    public Iec870Client(InputStream in, OutputStream out,
                        int linkAddress, int measuringPoint, long password) {
        this.in = in;
        this.out = out;
        this.linkAddress = linkAddress;
        this.measuringPoint = measuringPoint;
        this.password = password;
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    public void setFrameTimeoutMs(long ms) {
        this.frameTimeoutMs = ms;
    }

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

    public void openSession() throws Iec870Exception {
        log("Solicitando Estado de Enlace...(dirección : " + linkAddress + ")");
        requestLinkState();
        log("Contador Encontrado - \"Remote link reposition\"");
        sendFixedExpectAck(Iec870Frame.controlByte(0, false, Iec870Frame.CF_REPOSITION));
        fcb = 0;
        requestLinkState();
        // El software funcional hace un sondeo de clase 2 antes de la clave
        // (respuesta "sin datos"); además fija la paridad inicial del FCB.
        try {
            poll();
        } catch (Iec870Exception ignored) {
        }

        log("Enviando clave de acceso");
        byte[] data = new byte[]{
                (byte) password, (byte) (password >> 8), (byte) (password >> 16), (byte) (password >> 24)
        };
        sendVariableExpectAck(TYPE_AUTH, 1, Iec870Frame.CAUSE_ACTIVATION, 0, data);
        Iec870Frame resp = awaitCause(Iec870Frame.CAUSE_ACTIVATION_CONFIRM);
        if (resp.negative) {
            throw new Iec870Exception("Clave de acceso rechazada por el contador");
        }
        log("Clave de acceso OK");
        log("Sesión abierta");
    }

    public void closeSession() {
        log("Cerrando session IEC");
        try {
            sendVariableExpectAck(TYPE_END_SESSION, 0, Iec870Frame.CAUSE_ACTIVATION, 0, new byte[0]);
            awaitCause(Iec870Frame.CAUSE_ACTIVATION_CONFIRM);
        } catch (Iec870Exception e) {
            log("Error cerrando sesión: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Lecturas
    // ------------------------------------------------------------------

    /** Lee el identificador del equipo y devuelve p. ej. "C50991687". */
    public String readSerial() throws Iec870Exception {
        log("Leyendo Serial...");
        sendVariableExpectAck(TYPE_READ_ID, 0, Iec870Frame.CAUSE_REQUESTED, 0, new byte[0]);
        Iec870Frame r = awaitCause(Iec870Frame.CAUSE_REQUESTED);
        if (r.type != TYPE_ID_RESPONSE || r.data.length < 6) {
            throw new Iec870Exception("Respuesta de identificación inesperada (tipo " + r.type + ")");
        }
        int fabricante = r.data[1] & 0xFF;
        long equipo = (r.data[2] & 0xFFL) | ((r.data[3] & 0xFFL) << 8)
                | ((r.data[4] & 0xFFL) << 16) | ((r.data[5] & 0xFFL) << 24);
        return String.valueOf((char) fabricante) + equipo;
    }

    /**
     * Lee la curva de carga (totales integrados incrementales, ASDU 123).
     * Fechas "yyyy/MM/dd"; se pide de las 00:00 de inicio a las 00:00 de fin.
     */
    public List<Iec870LoadProfileRow> readLoadProfile(String fechaInicio, String fechaFin,
                                                      Periodo periodo) throws Iec870Exception {
        byte[] ini = timeA(fechaInicio);
        byte[] fin = timeA(fechaFin);

        byte[] data = new byte[12];
        data[0] = 1;  // primer total integrado
        data[1] = 6;  // último total integrado
        System.arraycopy(ini, 0, data, 2, 5);
        System.arraycopy(fin, 0, data, 7, 5);

        log("Leyendo curvas...");
        sendVariableExpectAck(TYPE_READ_INCREMENTAL, 1, Iec870Frame.CAUSE_ACTIVATION,
                periodo.registro, data);
        if (awaitConfirmation().cause == Iec870Frame.CAUSE_NO_DATA) {
            drain();
            log("Datos no disponibles");
            return new ArrayList<>();
        }

        List<Iec870LoadProfileRow> rows = new ArrayList<>();
        for (Iec870Frame f : collectData()) {
            if (f.type == TYPE_INCREMENTAL) {
                rows.add(parseIncremental(f));
            }
        }
        log("Load Profile successfully read");
        return rows;
    }

    /**
     * Lee los eventos de un registro (ASDU 102). Para "Arranques y tensión bajo
     * límites" el registro es {@link Iec870Events#REG_ARRANQUES_TENSION} (52).
     */
    public List<Iec870Event> readEvents(int registro, String fechaInicio, String fechaFin)
            throws Iec870Exception {
        byte[] ini = timeA(fechaInicio);
        byte[] fin = timeA(fechaFin);

        byte[] data = new byte[10];
        System.arraycopy(ini, 0, data, 0, 5);
        System.arraycopy(fin, 0, data, 5, 5);

        log("Leyendo Eventos (registro " + registro + ")...");
        sendVariableExpectAck(TYPE_READ_EVENTS, 0, Iec870Frame.CAUSE_ACTIVATION, registro, data);
        if (awaitConfirmation().cause == Iec870Frame.CAUSE_NO_DATA) {
            drain();
            log("Datos no disponibles");
            return new ArrayList<>();
        }

        List<Iec870Event> eventos = new ArrayList<>();
        for (Iec870Frame f : collectData()) {
            if (f.type == TYPE_EVENT) {
                parseEvents(f, eventos);
            }
        }
        log("Evento/s recuperado/s");
        return eventos;
    }

    /**
     * Lee los valores instantáneos: relación de transformación (ASDU 156, objeto
     * 0xC6) y totalizadores, potencias y tensiones/intensidades (ASDU 162,
     * objetos 0xC0, 0xC1 y 0xC2). Cada petición se responde con una sola trama
     * de causa 5 (sin confirmación ni "activación terminada").
     * El campo {@code serie} lo rellena quien llama.
     */
    public Iec870InstantValues readInstantValues() throws Iec870Exception {
        Iec870InstantValues v = new Iec870InstantValues();

        log("Leyendo...");
        sendVariableExpectAck(TYPE_READ_RM2, 1, Iec870Frame.CAUSE_REQUESTED, 0,
                new byte[]{(byte) OBJ_TRAFO_RATIO});
        parseTrafoRatio(awaitCause(Iec870Frame.CAUSE_REQUESTED), v);
        log("Leidos datos de RT");

        sendVariableExpectAck(TYPE_READ_INSTANT, 3, Iec870Frame.CAUSE_REQUESTED, 0,
                new byte[]{(byte) OBJ_TOTALS, (byte) OBJ_POWERS, (byte) OBJ_VOLT_CURR});
        parseInstant(awaitCause(Iec870Frame.CAUSE_REQUESTED), v);
        log("Valores instantáneos recuperados");
        return v;
    }

    private void parseTrafoRatio(Iec870Frame f, Iec870InstantValues v) throws Iec870Exception {
        byte[] d = f.data;
        if (f.negative || f.type != TYPE_RM2 || d.length < 17
                || (d[0] & 0xFF) != OBJ_TRAFO_RATIO) {
            throw new Iec870Exception("Respuesta de relación de transformación inesperada: "
                    + Iec870Frame.hex(f.raw));
        }
        v.tensionPrimariaDv = le(d, 1, 4);
        v.tensionSecundariaDv = le(d, 5, 4);
        v.corrientePrimariaDa = le(d, 9, 4);
        v.corrienteSecundariaDa = le(d, 13, 4);
    }

    private void parseInstant(Iec870Frame f, Iec870InstantValues v) throws Iec870Exception {
        byte[] d = f.data;
        if (f.negative || f.type != TYPE_INSTANT) {
            throw new Iec870Exception("Respuesta de valores instantáneos inesperada: "
                    + Iec870Frame.hex(f.raw));
        }
        boolean totales = false, potencias = false, ivs = false;
        int pos = 0;
        for (int i = 0; i < f.qualifier; i++) {
            if (pos >= d.length) {
                throw new Iec870Exception("ASDU 163 truncado: " + Iec870Frame.hex(f.raw));
            }
            int id = d[pos++] & 0xFF;
            int need;
            switch (id) {
                case OBJ_TOTALS: need = LEN_TOTALS; break;
                case OBJ_POWERS: need = LEN_POWERS; break;
                case OBJ_VOLT_CURR: need = LEN_VOLT_CURR; break;
                default:
                    throw new Iec870Exception("Objeto de valores instantáneos desconocido: 0x"
                            + Integer.toHexString(id).toUpperCase());
            }
            if (pos + need > d.length) {
                throw new Iec870Exception("ASDU 163 truncado: " + Iec870Frame.hex(f.raw));
            }

            if (id == OBJ_TOTALS) {
                // 6 x uint32: bits 0..26 valor, bit 27 = no válido
                for (int k = 0; k < 6; k++) {
                    long raw = le(d, pos + k * 4, 4);
                    v.energia[k] = raw & 0x07FFFFFFL;
                    v.energiaInvalida[k] = (raw & 0x08000000L) != 0;
                }
                v.fechaHora = timeAText(d, pos + 24);
                totales = true;
            } else if (id == OBJ_POWERS) {
                // Total, fase 1, 2 y 3. Cada grupo (8 octetos, campos de bits LSB primero):
                // activa 24 b (kW) | reactiva 24 b (kVAr) | FP 10 b (milésimas)
                // | exportada 1 b | cuadrante Q3/Q4 1 b | 3 b sin uso | no válido 1 b
                for (int k = 0; k < 4; k++) {
                    int o = pos + k * 8;
                    long w = le(d, o + 6, 2);
                    v.potenciaActiva[k] = le(d, o, 3);
                    v.potenciaReactiva[k] = le(d, o + 3, 3);
                    v.factorPotenciaMilesimas[k] = (int) (w & 0x3FF);
                    v.exportada[k] = (w & 0x0400) != 0;
                    v.cuadrante34[k] = (w & 0x0800) != 0;
                    v.potenciaInvalida[k] = (w & 0x8000) != 0;
                }
                potencias = true;
            } else {
                // Fases 1, 2 y 3, 7 octetos cada una: intensidad 24 b (dA)
                // | tensión 30 b (dV) | 1 b sin uso | no válido 1 b
                for (int k = 0; k < 3; k++) {
                    int o = pos + k * 7;
                    long w = le(d, o + 3, 4);
                    v.corrienteDa[k] = le(d, o, 3);
                    v.tensionDv[k] = w & 0x3FFFFFFFL;
                    v.ivInvalido[k] = (w & 0x80000000L) != 0;
                }
                if (v.fechaHora.isEmpty()) {
                    v.fechaHora = timeAText(d, pos + 21);
                }
                ivs = true;
            }
            pos += need;
        }
        if (!totales || !potencias || !ivs) {
            throw new Iec870Exception("El contador no devolvió todos los valores instantáneos: "
                    + Iec870Frame.hex(f.raw));
        }
    }

    /** Entero sin signo little-endian de n octetos. */
    private static long le(byte[] d, int off, int n) {
        long v = 0;
        for (int i = n - 1; i >= 0; i--) {
            v = (v << 8) | (d[off + i] & 0xFFL);
        }
        return v;
    }

    /**
     * TimeA (5 octetos) como "yyyy/MM/dd HH:mm:00X". La letra sigue al software
     * funcional: SU=0 -> "S" y SU=1 -> "W" (así lo imprime su informe).
     */
    private static String timeAText(byte[] d, int off) {
        int minuto = d[off] & 0x3F;
        int hora = d[off + 1] & 0x1F;
        boolean su = (d[off + 1] & 0x80) != 0;
        int dia = d[off + 2] & 0x1F;
        if (dia == 0) dia = 1;
        int mes = d[off + 3] & 0x0F;
        if (mes == 0) mes = 1;
        int anio = 2000 + (d[off + 4] & 0x7F);
        return String.format(Locale.US, "%04d/%02d/%02d %02d:%02d:00%s",
                anio, mes, dia, hora, minuto, su ? "W" : "S");
    }

    /**
     * Lee los valores memorizados de tarificación de un contrato (1..3): ASDU 134
     * con registro 134 + (contrato - 1) y rango de fechas. El contador devuelve un
     * ASDU 136 por cada tarifa (0 = total, 1..6) de cada cierre del rango.
     * Fechas "yyyy/MM/dd"; el software funcional pide desde las 01:00 de la fecha
     * de inicio hasta las 00:00 de la de fin.
     */
    public List<Iec870BillingRecord> readStoredBilling(int contrato, String fechaInicio,
                                                       String fechaFin) throws Iec870Exception {
        if (contrato < 1 || contrato > 3) {
            throw new Iec870Exception("Contrato inválido: " + contrato);
        }
        byte[] ini = timeA(fechaInicio);
        byte[] fin = timeA(fechaFin);
        ini[1] = 1; // hora = 1, como la petición del software funcional

        byte[] data = new byte[10];
        System.arraycopy(ini, 0, data, 0, 5);
        System.arraycopy(fin, 0, data, 5, 5);

        log("Leyendo Contrato " + contrato);
        sendVariableExpectAck(TYPE_READ_BILLING_STORED, 1, Iec870Frame.CAUSE_ACTIVATION,
                133 + contrato, data);
        if (awaitConfirmation().cause == Iec870Frame.CAUSE_NO_DATA) {
            drain();
            log("Datos no disponibles");
            return new ArrayList<>();
        }

        List<Iec870BillingRecord> registros = new ArrayList<>();
        for (Iec870Frame f : collectData()) {
            if (f.type == TYPE_BILLING_STORED) {
                parseBilling(f, contrato, registros);
            }
        }
        log("Registros de cierre recuperados");
        return registros;
    }

    /**
     * Lee los valores de tarificación en curso de un contrato (1..3): ASDU 133 sin
     * datos, registro 133 + contrato. Responde con un ASDU 135 por tarifa; el
     * periodo va desde el último cierre hasta ahora.
     */
    public List<Iec870BillingRecord> readCurrentBilling(int contrato) throws Iec870Exception {
        if (contrato < 1 || contrato > 3) {
            throw new Iec870Exception("Contrato inválido: " + contrato);
        }
        log("Leyendo Contrato " + contrato);
        sendVariableExpectAck(TYPE_READ_BILLING_CURRENT, 0, Iec870Frame.CAUSE_ACTIVATION,
                133 + contrato, new byte[0]);
        if (awaitConfirmation().cause == Iec870Frame.CAUSE_NO_DATA) {
            drain();
            log("Datos no disponibles");
            return new ArrayList<>();
        }

        List<Iec870BillingRecord> registros = new ArrayList<>();
        for (Iec870Frame f : collectData()) {
            if (f.type == TYPE_BILLING_CURRENT) {
                parseBilling(f, contrato, registros);
            }
        }
        log("Registros de cierre recuperados");
        return registros;
    }

    private void parseBilling(Iec870Frame f, int contrato, List<Iec870BillingRecord> out)
            throws Iec870Exception {
        byte[] d = f.data;
        int n = Math.max(f.qualifier, 1);
        if (d.length < n * LEN_BILLING) {
            throw new Iec870Exception("ASDU 136 truncado: " + Iec870Frame.hex(f.raw));
        }
        for (int i = 0; i < n; i++) {
            int p = i * LEN_BILLING;
            Iec870BillingRecord r = new Iec870BillingRecord();
            r.contrato = contrato;
            r.tarifa = (d[p] & 0xFF) - BILLING_FIRST_ADDRESS;
            r.activaAbs = le(d, p + 1, 4);
            r.activaInc = le(d, p + 5, 4);
            r.activaCal = d[p + 9] & 0xFF;
            r.reactivaIndAbs = le(d, p + 10, 4);
            r.reactivaIndInc = le(d, p + 14, 4);
            r.reactivaIndCal = d[p + 18] & 0xFF;
            r.reactivaCapAbs = le(d, p + 19, 4);
            r.reactivaCapInc = le(d, p + 23, 4);
            r.reactivaCapCal = d[p + 27] & 0xFF;
            r.reservado7 = le(d, p + 28, 4);
            r.reservado7Cal = d[p + 32] & 0xFF;
            r.reservado8 = le(d, p + 33, 4);
            r.reservado8Cal = d[p + 37] & 0xFF;
            r.maximetro = le(d, p + 38, 4);
            r.maximetroFecha = timeAText(d, p + 42);
            r.maximetroCal = d[p + 47] & 0xFF;
            r.exceso = le(d, p + 48, 4);
            r.excesoCal = d[p + 52] & 0xFF;
            r.inicio = timeAText(d, p + 53);
            r.fin = timeAText(d, p + 58);
            out.add(r);
        }
    }

    /**
     * Sondea y devuelve las tramas de datos hasta la "activación terminada"
     * (causa 0x0A), y hace los dos sondeos finales del software funcional.
     */
    private List<Iec870Frame> collectData() throws Iec870Exception {
        List<Iec870Frame> frames = new ArrayList<>();
        long lastContent = System.currentTimeMillis();
        int empty = 0;

        while (true) {
            Iec870Frame f = poll();
            if (!f.variable) {
                empty++;
                if (empty > maxEmptyPolls
                        || System.currentTimeMillis() - lastContent > contentTimeoutMs) {
                    throw new Iec870Exception("Tiempo agotado esperando datos del contador");
                }
                continue;
            }
            empty = 0;
            lastContent = System.currentTimeMillis();
            checkErrorCause(f);

            if (f.cause == Iec870Frame.CAUSE_ACTIVATION_END) {
                break;
            }
            frames.add(f); // causa 5: datos; seguir sondeando
        }

        drain();
        return frames;
    }

    /**
     * Como el software funcional: al menos un sondeo final, y los que hagan falta
     * para que la siguiente petición (cierre de sesión) salga con FCB = 0.
     */
    private void drain() {
        do {
            try {
                poll();
            } catch (Iec870Exception ignored) {
            }
        } while (fcb != 1);
    }

    /**
     * Espera la confirmación de activación (causa 7) o la respuesta "datos no
     * disponibles" (causa 13, el contador no tiene datos en ese rango).
     */
    private Iec870Frame awaitConfirmation() throws Iec870Exception {
        long last = System.currentTimeMillis();
        int empty = 0;
        while (true) {
            Iec870Frame f = poll();
            if (!f.variable) {
                empty++;
                if (empty > maxEmptyPolls || System.currentTimeMillis() - last > contentTimeoutMs) {
                    throw new Iec870Exception("Tiempo agotado esperando respuesta del contador");
                }
                continue;
            }
            checkErrorCause(f);
            if (f.cause == Iec870Frame.CAUSE_ACTIVATION_CONFIRM
                    || f.cause == Iec870Frame.CAUSE_NO_DATA) {
                return f;
            }
            last = System.currentTimeMillis();
        }
    }

    private void parseEvents(Iec870Frame f, List<Iec870Event> out) throws Iec870Exception {
        byte[] d = f.data;
        int n = f.qualifier;
        if (d.length < n * 9) {
            throw new Iec870Exception("ASDU 1 truncado: " + Iec870Frame.hex(f.raw));
        }
        for (int i = 0; i < n; i++) {
            int p = i * 9;
            int spa = d[p] & 0xFF;
            int spqi = d[p + 1] & 0xFF;
            int spq = (spqi & 0xF7) >> 1;
            int spi = spqi & 0x01;

            int msSeg = (d[p + 2] & 0xFF) | ((d[p + 3] & 0xFF) << 8);
            int seg = (msSeg >> 10) & 0x3F;
            int min = d[p + 4] & 0x3F;
            int hora = d[p + 5] & 0x1F;
            boolean verano = (d[p + 5] & 0x80) != 0;
            int dia = d[p + 6] & 0x1F;
            if (dia == 0) dia = 1;
            int mes = d[p + 7] & 0x0F;
            if (mes == 0) mes = 1;
            int anio = d[p + 8] & 0x7F;

            // Mismo formato que los eventos DLMS: "d/M/yy H:mm:ss"
            String fh = String.format("%d/%d/%02d %d:%02d:%02d", dia, mes, anio, hora, min, seg);
            out.add(new Iec870Event(fh, verano, spa, spq, spi,
                    Iec870Events.descripcion(spa, spq, spi)));
        }
    }

    // ------------------------------------------------------------------
    // Análisis de ASDU 11 (totales integrados incrementales)
    // ------------------------------------------------------------------

    private Iec870LoadProfileRow parseIncremental(Iec870Frame f) throws Iec870Exception {
        int n = f.qualifier;
        byte[] d = f.data;
        if (d.length < n * 6 + 5) {
            throw new Iec870Exception("ASDU 11 truncado: " + Iec870Frame.hex(f.raw));
        }
        long[] val = new long[9];
        int calidad = -1;
        boolean invalido = false;
        for (int i = 0; i < n; i++) {
            int p = i * 6;
            int addr = d[p] & 0xFF;
            long total = (d[p + 1] & 0xFFL) | ((d[p + 2] & 0xFFL) << 8)
                    | ((d[p + 3] & 0xFFL) << 16) | ((d[p + 4] & 0xFFL) << 24);
            int q = d[p + 5] & 0xFF;
            if (addr >= 1 && addr <= 8) val[addr] = total;
            if (calidad < 0) calidad = q;
            if ((q & 0x80) != 0) invalido = true;
        }
        int t = n * 6;
        int minuto = d[t] & 0x3F;
        int hora = d[t + 1] & 0x1F;
        boolean verano = (d[t + 1] & 0x80) != 0;
        int dia = d[t + 2] & 0x1F;
        if (dia == 0) dia = 1;
        int mes = d[t + 3] & 0x0F;
        if (mes == 0) mes = 1;
        int anio = 2000 + (d[t + 4] & 0x7F);

        String fh = String.format("%04d/%02d/%02d %02d:%02d:00%s",
                anio, mes, dia, hora, minuto, verano ? "S" : "W");
        return new Iec870LoadProfileRow(fh, verano, val[1], val[2], val[3], val[4],
                val[5], val[6], Math.max(calidad, 0), invalido);
    }

    /** Tiempo tipo A (5 octetos) a las 00:00 de "yyyy/MM/dd". SU=0 como el software funcional. */
    static byte[] timeA(String fecha) throws Iec870Exception {
        String[] p = fecha.trim().split("[/\\-]");
        if (p.length != 3) {
            throw new Iec870Exception("Fecha inválida (se esperaba yyyy/MM/dd): " + fecha);
        }
        int y, m, d;
        try {
            y = Integer.parseInt(p[0]);
            m = Integer.parseInt(p[1]);
            d = Integer.parseInt(p[2]);
        } catch (NumberFormatException e) {
            throw new Iec870Exception("Fecha inválida: " + fecha);
        }
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d);
        int dow = c.get(Calendar.DAY_OF_WEEK);       // domingo = 1
        int iso = (dow == Calendar.SUNDAY) ? 7 : dow - 1; // lunes = 1 .. domingo = 7
        return new byte[]{
                0,                                    // minuto, TIS, IV
                0,                                    // hora, SU
                (byte) ((iso << 5) | (d & 0x1F)),     // día del mes + día de la semana
                (byte) (m & 0x0F),                    // mes (ETI/PTI = 0)
                (byte) ((y % 100) & 0x7F)             // año
        };
    }

    // ------------------------------------------------------------------
    // Capa de enlace
    // ------------------------------------------------------------------

    private void requestLinkState() throws Iec870Exception {
        send(Iec870Frame.buildFixed(
                Iec870Frame.controlByte(0, false, Iec870Frame.CF_LINK_STATE), linkAddress));
        Iec870Frame r = readFrame();
        if (r.variable) {
            throw new Iec870Exception("Respuesta de estado de enlace inesperada");
        }
    }

    private void sendFixedExpectAck(int control) throws Iec870Exception {
        send(Iec870Frame.buildFixed(control, linkAddress));
        readFrame();
    }

    private int nextFcb() {
        fcb = (fcb + 1) & 1;
        return fcb;
    }

    private void sendVariableExpectAck(int type, int qualifier, int cause, int register,
                                       byte[] data) throws Iec870Exception {
        int control = Iec870Frame.controlByte(nextFcb(), true, Iec870Frame.CF_USER_DATA);
        send(Iec870Frame.buildVariable(control, linkAddress, type, qualifier, cause,
                measuringPoint, register, data));
        Iec870Frame ack = readFrame();
        if (ack.variable || ack.functionCode != 0) {
            throw new Iec870Exception("El contador no confirma la petición (trama: "
                    + Iec870Frame.hex(ack.raw) + ")");
        }
    }

    /** Sondeo de datos de clase 2. */
    private Iec870Frame poll() throws Iec870Exception {
        int control = Iec870Frame.controlByte(nextFcb(), true, Iec870Frame.CF_REQUEST_CLASS2);
        send(Iec870Frame.buildFixed(control, linkAddress));
        return readFrame();
    }

    /** Sondea hasta recibir una trama variable con la causa esperada. */
    private Iec870Frame awaitCause(int expected) throws Iec870Exception {
        long last = System.currentTimeMillis();
        int empty = 0;
        while (true) {
            Iec870Frame f = poll();
            if (!f.variable) {
                empty++;
                if (empty > maxEmptyPolls || System.currentTimeMillis() - last > contentTimeoutMs) {
                    throw new Iec870Exception("Tiempo agotado esperando respuesta del contador");
                }
                continue;
            }
            checkErrorCause(f);
            if (f.cause == expected) return f;
            last = System.currentTimeMillis();
        }
    }

    private void checkErrorCause(Iec870Frame f) throws Iec870Exception {
        switch (f.cause) {
            case Iec870Frame.CAUSE_TYPE_UNAVAILABLE:
                throw new Iec870Exception("Tipo de ASDU no disponible en este contador");
            case Iec870Frame.CAUSE_DIRECTION_UNKNOWN:
                throw new Iec870Exception("Dirección de ASDU desconocida");
            case Iec870Frame.CAUSE_OBJECT_UNAVAILABLE:
                throw new Iec870Exception("Objeto de información no disponible");
            case Iec870Frame.CAUSE_PERIOD_UNAVAILABLE:
                throw new Iec870Exception("Periodo de integración no disponible");
            default:
        }
    }

    // ------------------------------------------------------------------
    // Entrada/salida
    // ------------------------------------------------------------------

    private void send(byte[] frame) throws Iec870Exception {
        try {
            log(">> " + Iec870Frame.hex(frame));
            out.write(frame);
            out.flush();
        } catch (IOException e) {
            throw new Iec870Exception("Error enviando datos: " + e.getMessage(), e);
        }
    }

    private Iec870Frame readFrame() throws Iec870Exception {
        long deadline = System.currentTimeMillis() + frameTimeoutMs;
        int b;
        do {
            b = readByte(deadline);
        } while (b == 0xFF);

        byte[] raw;
        Iec870Frame f;
        if (b == Iec870Frame.START_FIXED) {
            raw = new byte[6];
            raw[0] = (byte) b;
            for (int i = 1; i < 6; i++) raw[i] = (byte) readByte(deadline);
            f = Iec870Frame.parseFixed(raw);
        } else if (b == Iec870Frame.START_VARIABLE) {
            int len = readByte(deadline);
            int len2 = readByte(deadline);
            if (len != len2) throw new Iec870Exception("Longitud incoherente en trama variable");
            raw = new byte[len + 6];
            raw[0] = (byte) b;
            raw[1] = (byte) len;
            raw[2] = (byte) len2;
            for (int i = 3; i < raw.length; i++) raw[i] = (byte) readByte(deadline);
            f = Iec870Frame.parseVariable(raw);
        } else {
            throw new Iec870Exception("Byte de inicio inesperado: 0x" + Integer.toHexString(b));
        }
        log("<< " + Iec870Frame.hex(raw));
        return f;
    }

    private int readByte(long deadline) throws Iec870Exception {
        try {
            while (true) {
                if (in.available() > 0) {
                    int b = in.read();
                    if (b < 0) throw new Iec870Exception("Conexión cerrada por el contador/sonda");
                    return b;
                }
                if (System.currentTimeMillis() > deadline) {
                    throw new Iec870Exception("Tiempo de espera agotado (sin respuesta del contador)");
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new Iec870Exception("Operación interrumpida");
                }
            }
        } catch (IOException e) {
            throw new Iec870Exception("Error leyendo datos: " + e.getMessage(), e);
        }
    }

    private void log(String s) {
        if (trace != null) trace.line(s);
    }
}