package com.celnet.syncro.objects.instantValues;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.instantvalues.RegistroS29;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSProfileGeneric;
import gurux.dlms.objects.GXDLMSRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstantaneousValuesReader {

    /*
     * Scalers reales de este contador (leídos de las tramas DLMS capturadas):
     *
     *   Voltaje    (1.0.3x.7.0.255)  → 0F 00  → scaler =  0  unidad deciV  → raw/10  = V
     *   Corriente  (1.0.x1.7.0.255)  → 0F FF  → scaler = -1  unidad deciA  → raw*0.1 = A
     *   FP         (1.0.x3.7.0.255)  → 0F FF  → scaler = -3  adimensional  → raw*1e-3
     *   Potencia   (1.0.x1-4.7.0.255)→ 0F FD  → scaler = -3  W             → raw*1e-3 = kW
     *   Energía    (1.0.x.8.0.255)   → 0F 00  → scaler =  0  Wh            → raw/1000 = kWh
     *
     * getScaler() de la librería Gurux devuelve siempre 0 para este contador,
     * por eso parseamos el scaler manualmente del atributo 3 (array de 2 bytes:
     * byte[0]=scaler con signo, byte[1]=unidad).
     */

    public static String leerValores(GXDLMSReader reader) throws Exception {

        AppLogger.i("InstantValues", "Leyendo valores instantáneos...");
        StringBuilder sb = new StringBuilder();

        // ── Timestamp ─────────────────────────────────────────────────────────
        sb.append("------------------------------\n");
        try {
            gurux.dlms.objects.GXDLMSClock clock =
                    new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
            reader.read(clock, 2);
            gurux.dlms.GXDateTime dt = clock.getTime();
            java.util.Calendar c = dt.getMeterCalendar();
            sb.append(String.format("Timestamp : %04d/%02d/%02d %02d:%02d:%02d.000S%n",
                    c.get(java.util.Calendar.YEAR),
                    c.get(java.util.Calendar.MONTH) + 1,
                    c.get(java.util.Calendar.DAY_OF_MONTH),
                    c.get(java.util.Calendar.HOUR_OF_DAY),
                    c.get(java.util.Calendar.MINUTE),
                    c.get(java.util.Calendar.SECOND)));
        } catch (Exception e) {
            sb.append("Timestamp : N/A\n");
            AppLogger.w("InstantValues", "No se pudo leer el timestamp: " + e.getMessage());
        }

        // ── Voltajes, corrientes, FP ──────────────────────────────────────────
        double v1 = 0, v2 = 0, v3 = 0;
        double a1 = 0, a2 = 0, a3 = 0;
        double fp1 = 0, fp2 = 0, fp3 = 0, fpTotal = 0;

        try { v1 = readRawValue("1.0.32.7.0.255", reader); } catch (Exception ignored) {}
        try { v2 = readRawValue("1.0.52.7.0.255", reader); } catch (Exception ignored) {}
        try { v3 = readRawValue("1.0.72.7.0.255", reader); } catch (Exception ignored) {}

        try { a1 = readScaledValue("1.0.31.7.0.255", reader); } catch (Exception ignored) {}
        try { a2 = readScaledValue("1.0.51.7.0.255", reader); } catch (Exception ignored) {}
        try { a3 = readScaledValue("1.0.71.7.0.255", reader); } catch (Exception ignored) {}

        try { fp1 = readScaledValue("1.0.33.7.0.255", reader); } catch (Exception ignored) {}
        try { fp2 = readScaledValue("1.0.53.7.0.255", reader); } catch (Exception ignored) {}
        try { fp3 = readScaledValue("1.0.73.7.0.255", reader); } catch (Exception ignored) {}
        try { fpTotal = readScaledValue("1.0.13.7.0.255", reader); } catch (Exception e) {
            fpTotal = (fp1 + fp2 + fp3) / 3.0;
            AppLogger.w("InstantValues", "FP total calculado como media de fases: " + fpTotal);
        }

        sb.append("Valores Tensión       Corriente        FP :\n");
        sb.append(String.format("Fase 1 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v1, a1, fp1));
        sb.append(String.format("Fase 2 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v2, a2, fp2));
        sb.append(String.format("Fase 3 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v3, a3, fp3));
        sb.append(String.format("FP (sum of all phases: +P/S) : %,.3f%n", fpTotal));

        // ── Relaciones ────────────────────────────────────────────────────────
        double vPrim = 0, vSec = 0, aPrim = 0, aSec = 0;
        try { vPrim = readDataValue("1.0.0.4.3.255", reader); } catch (Exception e) {
            try { vPrim = readDataValue("0.0.4.3.0.255", reader); } catch (Exception ignored) {}
        }
        try { vSec = readDataValue("1.0.0.4.6.255", reader); } catch (Exception e) {
            try { vSec = readDataValue("0.0.4.6.0.255", reader); } catch (Exception ignored) {}
        }
        try { aPrim = readDataValue("1.0.0.4.2.255", reader); } catch (Exception e) {
            try { aPrim = readDataValue("0.0.4.2.0.255", reader); } catch (Exception ignored) {}
        }
        try { aSec = readDataValue("1.0.0.4.5.255", reader); } catch (Exception e) {
            try { aSec = readDataValue("0.0.4.5.0.255", reader); } catch (Exception ignored) {}
        }

        double vRatio = (vSec != 0) ? vPrim / vSec : 1.0;
        double aRatio = (aSec != 0) ? aPrim / aSec : 1.0;

        sb.append("Relación de Transformacion de Tensión : [deciVolts]/[deciVolts] \n");
        sb.append(String.format("Prim/sec  (%.0f / %.0f) = %,.3f%n", vPrim, vSec, vRatio));
        sb.append("Relación de Transformacion Corriente : [deciAmps]/[deciAmps]\n");
        sb.append(String.format("Prim/sec (%.0f / %.0f) = %,.3f%n", aPrim, aSec, aRatio));

        // ── Potencias por fase (en kW) ────────────────────────────────────────
        double p1p=0, p2p=0, p3p=0, p1m=0, p2m=0, p3m=0;
        double q1p=0, q2p=0, q3p=0, q1m=0, q2m=0, q3m=0;

        try { p1p = readRawValue("1.0.21.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p2p = readRawValue("1.0.41.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p3p = readRawValue("1.0.61.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        try { p1m = readRawValue("1.0.22.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p2m = readRawValue("1.0.42.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p3m = readRawValue("1.0.62.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        try { q1p = readRawValue("1.0.23.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q2p = readRawValue("1.0.43.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q3p = readRawValue("1.0.63.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        try { q1m = readRawValue("1.0.24.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q2m = readRawValue("1.0.44.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q3m = readRawValue("1.0.64.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        sb.append("Valores       P+ [Kw]    P- [Kw]    Q+ [Kvar]  Q- [Kvar]  :\n");
        sb.append(String.format("Fase 1 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p1p, p1m, q1p, q1m));
        sb.append(String.format("Fase 2 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p2p, p2m, q2p, q2m));
        sb.append(String.format("Fase 3 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p3p, p3m, q3p, q3m));
        sb.append(String.format("Total  :       %,.3f      %,.3f      %,.3f      %,.3f%n",
                p1p+p2p+p3p, p1m+p2m+p3m, q1p+q2p+q3p, q1m+q2m+q3m));

        // ── Energías (en kWh) ─────────────────────────────────────────────────
        double eaPlus=0, eaMinus=0, eq1=0, eq2=0, eq3=0, eq4=0;

        try { eaPlus  = readRawValue("1.0.1.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eaMinus = readRawValue("1.0.2.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq1 = readRawValue("1.0.5.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq2 = readRawValue("1.0.6.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq3 = readRawValue("1.0.7.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq4 = readRawValue("1.0.8.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}

        sb.append(String.format("Activa Importada   :   %,.3f  [Kwh]%n", eaPlus));
        sb.append(String.format("Activa Exportada   :   %,.3f  [Kwh]%n", eaMinus));
        sb.append(String.format("Reactiva Q1        :   %,.3f  [KVArh]%n", eq1));
        sb.append(String.format("Reactiva Q2        :   %,.3f  [KVArh]%n", eq2));
        sb.append(String.format("Reactiva Q3        :   %,.3f  [KVArh]%n", eq3));
        sb.append(String.format("Reactiva Q4        :   %,.3f  [KVArh]%n", eq4));

        AppLogger.i("InstantValues", "Lectura de valores instantáneos completada.");
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double readRawValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 3);
        reader.read(reg, 2);
        Object value = reg.getValue();
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static double readScaledValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 3);
        reader.read(reg, 2);

        Object value = reg.getValue();
        double raw = 0;
        if (value instanceof Number) {
            raw = ((Number) value).doubleValue();
        } else if (value != null) {
            raw = Double.parseDouble(value.toString());
        }

        int scalerInt = (int) reg.getScaler();
        if (scalerInt > 127) scalerInt -= 256;

        AppLogger.d("InstantValues", String.format(
                "OBIS %s  raw=%.0f  scalerRaw=%.0f  scalerSigned=%d  result=%f",
                obis, raw, reg.getScaler(), scalerInt, raw * Math.pow(10, scalerInt)));

        return raw * Math.pow(10, scalerInt);
    }

    private static double readDataValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSData data = new GXDLMSData(obis);
        reader.read(data, 2);
        Object value = data.getValue();
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    // ── S29: buffer de valores instantáneos (Profile Generic) ──────────────────

    /*
     * class_id = 7 (Profile Generic), OBIS = 1-0:99.1.7.255, atributo = 2,
     * sin selector de rango (buffer completo). El cliente encadena
     * Get-request-next hasta el último bloque; cada bloque es una fila.
     *
     * Antes de leer el atributo 2 hace falta leer el atributo 3
     * (capture_objects), o Gurux lanza "Read capture objects first."
     *
     * El orden de las 31 columnas (COLUMNAS_S29) viene directamente de la
     * documentación oficial del fabricante — ver capture_objects más abajo.
     * Se devuelve TODO el buffer (histórico completo, p.ej. últimas 48h)
     * como una lista de RegistroS29 para que la UI lo pinte en un
     * RecyclerView con adapter, igual que se hace con CurvaFila.
     */
    private static final String OBIS_BUFFER_S29 = "1.0.99.1.7.255";

    public static List<RegistroS29> leerValoresS29(GXDLMSReader reader) throws Exception {
        AppLogger.i("InstantValues", "Leyendo valores instantáneos S29...");

        GXDLMSProfileGeneric buffer = new GXDLMSProfileGeneric(OBIS_BUFFER_S29);

        // Gurux necesita conocer la estructura de columnas (capture_objects)
        // ANTES de poder interpretar el buffer, o lanza
        // "Read capture objects first." — mismo patrón que LoadProfileReader
        // usa con reader.read(lp1, 3) antes de pedir las filas.
        reader.read(buffer, 3);

        if (buffer.getCaptureObjects() == null || buffer.getCaptureObjects().isEmpty()) {
            throw new Exception("El contador no devolvió capture_objects para el buffer S29 "
                    + "(OBIS " + OBIS_BUFFER_S29 + "). Sin ellos no se puede interpretar el buffer.");
        }

        AppLogger.i("InstantValues", "Buffer S29: " + buffer.getCaptureObjects().size() + " columnas definidas.");

        // Buffer completo (atributo 2), sin selector de rango.
        Object value = reader.read(buffer, 2);

        if (!(value instanceof Object[])) {
            throw new Exception("Formato de buffer S29 inesperado: " + value);
        }

        Object[] filas = (Object[]) value;
        AppLogger.i("InstantValues", "Buffer S29: " + filas.length + " filas recibidas.");

        if (filas.length == 0) {
            throw new Exception("El contador no devolvió filas en el buffer S29.");
        }

        List<RegistroS29> registros = new ArrayList<>(filas.length);
        int filaIndex = 0;
        for (Object filaObj : filas) {
            filaIndex++;
            if (!(filaObj instanceof Object[])) {
                AppLogger.w("InstantValues", "Fila S29 #" + filaIndex + " con formato inesperado: " + filaObj);
                continue;
            }
            Object[] fila = (Object[]) filaObj;

            if (fila.length != COLUMNAS_S29.length) {
                AppLogger.w("InstantValues", "Fila S29 #" + filaIndex + ": se esperaban " + COLUMNAS_S29.length
                        + " columnas y llegaron " + fila.length + ". Se parseará lo que se pueda.");
            }

            registros.add(parseFilaS29(fila));
        }

        AppLogger.i("InstantValues", "Lectura de valores instantáneos S29 completada: "
                + registros.size() + " registros parseados.");

        return registros;
    }

    // Orden exacto de capture_objects según documentación oficial
    // "Instantaneous Values Profile (S29)" — OBIS 1-0:99.1.7.255.
    private static final String[] COLUMNAS_S29 = {
            "clock",
            "Voltage L1", "Current L1",
            "Voltage L2", "Current L2",
            "Voltage L3", "Current L3",
            "Current (suma 3 fases)", "Neutral current", "I dif",
            "Active power P (signed)", "Active power P L1", "Active power P L2", "Active power P L3",
            "Reactive power Q (signed)", "Reactive power Q+ L1", "Reactive power Q+ L2", "Reactive power Q+ L3",
            "Power Factor", "Power Factor L1", "Power Factor L2", "Power Factor L3",
            "Phase sequence",
            "FI-U1 angle", "FI-U2 angle", "FI-U3 angle",
            "FI-I1 angle", "FI-I2 angle", "FI-I3 angle",
            "FI-IN angle", "FI-Idif angle"
    };

    /**
     * Convierte una fila cruda del buffer (Object[] con el orden de
     * {@link #COLUMNAS_S29}) en un {@link RegistroS29} con todos los
     * valores ya escalados y formateados como String, replicando
     * exactamente el formato de referencia:
     *   - Tensión: x0.1, 1 decimal ("215.9")
     *   - Corriente: x0.1, 1 decimal ("1.9")
     *   - Potencia activa/reactiva (P/Q): SIN escalar, sin decimales ("394")
     *   - Factor de potencia: x1e-3, 3 decimales ("0.957")
     *   - Ángulos: x1e-3, 3 decimales ("343.233")
     *   - Secuencia de fases: texto crudo
     *   - Columnas null (fase no disponible): cadena vacía ""
     *   - Locale.US en todos los formatos (punto decimal, no coma)
     */
    private static RegistroS29 parseFilaS29(Object[] fila) {

        // ── Timestamp (columna 0, clock) ─────────────────────────────────────
        String fecha = "N/A";
        try {
            Object clockValue = fila[0];
            if (clockValue instanceof gurux.dlms.GXDateTime) {
                java.util.Calendar c = ((gurux.dlms.GXDateTime) clockValue).getMeterCalendar();
                fecha = String.format(Locale.US, "%04d/%02d/%02d %02d:%02d:%02d.000S",
                        c.get(java.util.Calendar.YEAR),
                        c.get(java.util.Calendar.MONTH) + 1,
                        c.get(java.util.Calendar.DAY_OF_MONTH),
                        c.get(java.util.Calendar.HOUR_OF_DAY),
                        c.get(java.util.Calendar.MINUTE),
                        c.get(java.util.Calendar.SECOND));
            } else {
                fecha = String.valueOf(clockValue);
            }
        } catch (Exception ignored) {}

        return new RegistroS29(
                fecha,
                // Tensión: x0.1, 1 decimal
                formatEscalado(fila, 1, 0.1, "%.1f"),
                formatEscalado(fila, 3, 0.1, "%.1f"),
                formatEscalado(fila, 5, 0.1, "%.1f"),
                // Corriente: x0.1, 1 decimal (antes %.2f, corregido a %.1f)
                formatEscalado(fila, 2, 0.1, "%.1f"),
                formatEscalado(fila, 4, 0.1, "%.1f"),
                formatEscalado(fila, 6, 0.1, "%.1f"),
                formatEscalado(fila, 7, 0.1, "%.1f"),
                formatEscalado(fila, 8, 0.1, "%.1f"),
                formatEscalado(fila, 9, 0.1, "%.1f"),
                // Potencia activa/reactiva: SIN escalar, sin decimales
                // (antes se dividía x1e-3 y se mostraba con 3 decimales, incorrecto)
                formatEscalado(fila, 10, 1.0, "%.0f"),
                formatEscalado(fila, 11, 1.0, "%.0f"),
                formatEscalado(fila, 12, 1.0, "%.0f"),
                formatEscalado(fila, 13, 1.0, "%.0f"),
                formatEscalado(fila, 14, 1.0, "%.0f"),
                formatEscalado(fila, 15, 1.0, "%.0f"),
                formatEscalado(fila, 16, 1.0, "%.0f"),
                formatEscalado(fila, 17, 1.0, "%.0f"),
                // Factor de potencia: x1e-3, 3 decimales
                formatEscalado(fila, 18, 1e-3, "%.3f"),
                formatEscalado(fila, 19, 1e-3, "%.3f"),
                formatEscalado(fila, 20, 1e-3, "%.3f"),
                formatEscalado(fila, 21, 1e-3, "%.3f"),
                // Secuencia de fases: texto crudo, sin escalar
                textoOVacio(fila, 22),
                // Ángulos: x1e-3, 3 decimales (antes texto crudo sin escalar, incorrecto)
                formatEscalado(fila, 23, 1e-3, "%.3f"),
                formatEscalado(fila, 24, 1e-3, "%.3f"),
                formatEscalado(fila, 25, 1e-3, "%.3f"),
                formatEscalado(fila, 26, 1e-3, "%.3f"),
                formatEscalado(fila, 27, 1e-3, "%.3f"),
                formatEscalado(fila, 28, 1e-3, "%.3f"),
                formatEscalado(fila, 29, 1e-3, "%.3f"),
                formatEscalado(fila, 30, 1e-3, "%.3f"));
    }
    /**
     * Formatea la columna [indice] de la fila aplicando el factor de escala
     * y el patrón indicados, con Locale.US. Si la columna no existe o su
     * valor crudo es null, devuelve cadena vacía en vez de "0.0".
     */
    private static String formatEscalado(Object[] fila, int indice, double factor, String patron) {
        if (indice >= fila.length || fila[indice] == null) {
            return "";
        }
        double numero = numeroEn(fila, indice) * factor;
        return String.format(Locale.US, patron, numero);
    }

    /**
     * Devuelve el valor crudo de la columna [indice] como texto, o cadena
     * vacía si la columna no existe o es null (en vez del texto "null").
     * Usado para columnas sin escalado conocido (secuencia de fases, ángulos).
     */
    private static String textoOVacio(Object[] fila, int indice) {
        if (indice >= fila.length || fila[indice] == null) {
            return "";
        }
        return String.valueOf(fila[indice]);
    }

    private static double numeroEn(Object[] fila, int indice) {
        if (indice >= fila.length) return 0.0;
        Object v = fila[indice];
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }
}