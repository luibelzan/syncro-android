package com.celnet.syncro.export;

import com.celnet.syncro.models.iec870.cierres.Iec870BillingRecord;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cierres memorizados IEC-870 en JSON ("Billings", tipo Monthly), con la estructura del informe
 * de referencia: una entrada por contrato y periodo tarifario (0 = total, 1..6).
 *
 * - Contratos 1 y 2 (consumo): AI*, R1* (inductiva) y R4* (capacitiva).
 * - Contrato 3 (generación): AE*, R3* (inductiva) y R2* (capacitiva).
 *   La dirección sin datos va con ceros y sin campo de calidad, como en el informe de referencia.
 */
public final class Iec870CierresJson {

    public static final String VERSION = "3.9.0.1";

    /** Parámetros de conexión que se reflejan en "PortConfig". */
    public static final class Puerto {
        final int baud, dataBits, dirPm, linkAddr;
        final long password;
        final String parity, stopBits;

        public Puerto(int baud, int dataBits, String parity, String stopBits,
                      int linkAddr, int dirPm, long password) {
            this.baud = baud;
            this.dataBits = dataBits;
            this.parity = parity;
            this.stopBits = stopBits;
            this.linkAddr = linkAddr;
            this.dirPm = dirPm;
            this.password = password;
        }
    }

    private Iec870CierresJson() {}

    public static void generar(OutputStream out, String contador,
                               List<Iec870BillingRecord> registros, Puerto puerto) throws IOException {
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        out.write(construir(contador, registros, puerto, ahora, uuid).getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** Separado de generar() para poder probarlo con fecha y UUID fijos. */
    public static String construir(String contador, List<Iec870BillingRecord> registros,
                                   Puerto p, String readDate, String uuid) {
        String num = contador == null ? "" : contador;
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"Billings\": [\n");

        for (int i = 0; i < registros.size(); i++) {
            Iec870BillingRecord r = registros.get(i);
            boolean exportacion = r.contrato == 3;
            String desde = fecha(r.inicio);
            String fMax = (r.maximetro == 0 || fecha(r.maximetroFecha).isEmpty())
                    ? desde : fecha(r.maximetroFecha);

            sb.append("    {\n");
            if (exportacion) {
                num(sb, "AEa", r.activaAbs);
                num(sb, "AEi", r.activaInc);
                num(sb, "AEQual", r.activaCal);
                num(sb, "AIa", 0);
                num(sb, "AIi", 0);
            } else {
                num(sb, "AEa", 0);
                num(sb, "AEi", 0);
                num(sb, "AIa", r.activaAbs);
                num(sb, "AIi", r.activaInc);
                num(sb, "AIQual", r.activaCal);
            }
            num(sb, "AI_MAX", r.maximetro);
            str(sb, "AI_Fh", fMax);
            num(sb, "AI_Max_Qual", r.maximetroCal);
            num(sb, "Contract", r.contrato);
            str(sb, "Fh_From", desde);
            str(sb, "Fh_To", fecha(r.fin));
            num(sb, "Excess", r.exceso);
            num(sb, "ExcessQual", r.excesoCal);
            str(sb, "Magnitude", "KW");
            str(sb, "Magnitude_Max", "KW");
            str(sb, "Meter_Num", num);
            num(sb, "Period", r.tarifa);
            if (exportacion) {
                num(sb, "R2a", r.reactivaCapAbs);
                num(sb, "R2i", r.reactivaCapInc);
                num(sb, "R2Qual", r.reactivaCapCal);
                num(sb, "R3a", r.reactivaIndAbs);
                num(sb, "R3i", r.reactivaIndInc);
                num(sb, "R3Qual", r.reactivaIndCal);
            } else {
                num(sb, "R1a", r.reactivaIndAbs);
                num(sb, "R1i", r.reactivaIndInc);
                num(sb, "R1Qual", r.reactivaIndCal);
                num(sb, "R4a", r.reactivaCapAbs);
                num(sb, "R4i", r.reactivaCapInc);
                num(sb, "R4Qual", r.reactivaCapCal);
            }
            str(sb, "routeId", "Route_RAW");
            sb.append("      \"Type\": \"Monthly\"\n");
            sb.append(i < registros.size() - 1 ? "    },\n" : "    }\n");
        }

        sb.append("  ],\n");
        sb.append("  \"Measurement\": \"USBSensor\",\n");
        sb.append("  \"Version\": \"").append(VERSION).append("\",\n");
        sb.append("  \"Origin\": \"TM\",\n");
        sb.append("  \"Periodicity\": \"Monthly\",\n");
        sb.append("  \"PortConfig\": {\n");
        sb.append("    \"baudRate\": ").append(p.baud).append(",\n");
        sb.append("    \"dataBits\": ").append(p.dataBits).append(",\n");
        sb.append("    \"dirPm\": ").append(p.dirPm).append(",\n");
        sb.append("    \"linkAddr\": ").append(p.linkAddr).append(",\n");
        sb.append("    \"parity\": \"").append(esc(p.parity)).append("\",\n");
        sb.append("    \"password\": ").append(p.password).append(",\n");
        sb.append("    \"portName\": \"tm\",\n");
        sb.append("    \"stopBits\": \"").append(esc(p.stopBits)).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"Read_date\": \"").append(readDate).append("\",\n");
        sb.append("  \"Route_date\": \"\",\n");
        sb.append("  \"Route_Id\": \"Route_RAW\",\n");
        sb.append("  \"UUID\": \"").append(uuid).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    private static void num(StringBuilder sb, String k, long v) {
        sb.append("      \"").append(k).append("\": ").append(v).append(",\n");
    }

    private static void str(StringBuilder sb, String k, String v) {
        sb.append("      \"").append(k).append("\": \"").append(esc(v)).append("\",\n");
    }

    /** "yyyy/MM/dd HH:mm:ss" (+ letra S/W) -> "yyyy-MM-dd HH:mm:ss"; "" si no hay fecha. */
    static String fecha(String t) {
        if (t == null || t.length() < 19) return "";
        return t.substring(0, 19).replace('/', '-');
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.toString();
    }
}