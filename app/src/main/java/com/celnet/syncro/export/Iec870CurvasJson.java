package com.celnet.syncro.export;

import com.celnet.syncro.models.iec870.curvas.Iec870LoadProfileRow;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Curvas de carga IEC-870 (horarias o cuartihorarias) en JSON ("Curves"), con la estructura del
 * informe de referencia. Cada registro lleva sus 6 totales (AI, AE, R1..R4), el byte de calidad
 * de cada uno en decimal, la fecha, la estación (S verano / W invierno) y el tipo de periodo.
 */
public final class Iec870CurvasJson {

    public static final String HORARIA = "Hourly";
    public static final String CUARTIHORARIA = "Quarterly";

    private Iec870CurvasJson() {}

    public static void generar(OutputStream out, String contador, List<Iec870LoadProfileRow> filas,
                               String periodo, Iec870CierresJson.Puerto puerto) throws IOException {
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        out.write(construir(contador, filas, periodo, puerto, ahora, uuid)
                .getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * @param periodo "Hourly" o "Quarterly"; si es null o desconocido se deduce de la distancia
     *                entre los dos primeros registros (60 min = Hourly, si no Quarterly).
     */
    public static String construir(String contador, List<Iec870LoadProfileRow> filas, String periodo,
                                   Iec870CierresJson.Puerto p, String readDate, String uuid) {
        String tipo = HORARIA.equals(periodo) || CUARTIHORARIA.equals(periodo)
                ? periodo : deducir(filas);
        String num = contador == null ? "" : contador;
        StringBuilder sb = new StringBuilder(filas.size() * 330 + 600);
        sb.append("{\n  \"Curves\": [\n");

        for (int i = 0; i < filas.size(); i++) {
            Iec870LoadProfileRow f = filas.get(i);
            sb.append("    {\n");
            n(sb, "AE", f.ae);
            n(sb, "AI", f.ai);
            n(sb, "R1", f.r1);
            n(sb, "R2", f.r2);
            n(sb, "R3", f.r3);
            n(sb, "R4", f.r4);
            n(sb, "AEQual", f.calidades[2]);
            n(sb, "AIQual", f.calidades[1]);
            s(sb, "Fh", fecha(f.fechaHora));
            s(sb, "Magnitude", "KW");
            s(sb, "Meter_Num", num);
            n(sb, "R1Qual", f.calidades[3]);
            n(sb, "R2Qual", f.calidades[4]);
            n(sb, "R3Qual", f.calidades[5]);
            n(sb, "R4Qual", f.calidades[6]);
            s(sb, "Season", f.verano ? "S" : "W");
            sb.append("      \"Type\": \"").append(tipo).append("\"\n");
            sb.append(i < filas.size() - 1 ? "    },\n" : "    }\n");
        }

        sb.append("  ],\n");
        sb.append("  \"Measurement\": \"USBSensor\",\n");
        sb.append("  \"Version\": \"").append(Iec870CierresJson.VERSION).append("\",\n");
        sb.append("  \"Origin\": \"TM\",\n");
        sb.append("  \"Periodicity\": \"").append(tipo).append("\",\n");
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

    private static String deducir(List<Iec870LoadProfileRow> filas) {
        if (filas.size() >= 2) {
            try {
                SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
                long a = f.parse(filas.get(0).fechaHora.substring(0, 19)).getTime();
                long b = f.parse(filas.get(1).fechaHora.substring(0, 19)).getTime();
                if (Math.abs(b - a) >= 45 * 60_000L) return HORARIA;
            } catch (Exception ignored) {
                // se queda con Quarterly
            }
        }
        return CUARTIHORARIA;
    }

    /** "yyyy/MM/dd HH:mm:ss" (+ letra S/W) -> "yyyy-MM-dd HH:mm:ss". */
    static String fecha(String t) {
        if (t == null || t.length() < 19) return "";
        return t.substring(0, 19).replace('/', '-');
    }

    private static void n(StringBuilder sb, String k, long v) {
        sb.append("      \"").append(k).append("\": ").append(v).append(",\n");
    }

    private static void s(StringBuilder sb, String k, String v) {
        sb.append("      \"").append(k).append("\": \"").append(esc(v)).append("\",\n");
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