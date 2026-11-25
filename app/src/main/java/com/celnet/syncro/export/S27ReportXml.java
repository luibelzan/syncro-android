package com.celnet.syncro.export;

import com.celnet.syncro.models.cierres.CierreEnCursoFila;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el XML del informe S27 (cierres en curso) en el formato del concentrador:
 * namespace http://stgdc/ws/S27, un bloque S27 por contrato y periodo (Pt 0..6) con
 * Mx (maxímetro) y Fx (fecha del maxímetro).
 */
public final class S27ReportXml {

    public static final String VERSION = "4.0";

    private static final DateTimeFormatter ENTRADA = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'000'");

    private S27ReportXml() {}

    public static String generar(List<CierreEnCursoFila> datos, String cntId, String cncId, ZoneId zona) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Report xmlns=\"http://stgdc/ws/S27\" IdRpt=\"S27\" IdPet=\"0\" Version=\"")
                .append(VERSION).append("\">\n");
        sb.append("    <Cnc Id=\"").append(esc(cncId)).append("\">\n");
        sb.append("        <Cnt Id=\"").append(esc(cntId)).append("\">\n");

        for (CierreEnCursoFila f : datos) {
            LocalDateTime fh = parsear(f.fecha);
            if (fh == null) fh = LocalDateTime.now(zona);
            String fhTxt = formatear(fh, zona);
            // Sin maxímetro, el concentrador espera el primer cuarto de hora del mes.
            String fxVacio = formatear(LocalDate.of(fh.getYear(), fh.getMonth(), 1).atTime(0, 15), zona);

            for (int p = 0; p < 7; p++) {
                LocalDateTime fx = (f.maxDates != null && p < f.maxDates.length) ? parsear(f.maxDates[p]) : null;
                String fxTxt = fx != null ? formatear(fx, zona) : fxVacio;

                sb.append("            <S27 Fh=\"").append(fhTxt)
                        .append("\" Ctr=\"").append(f.contrato)
                        .append("\" Pt=\"").append(p)
                        .append("\" Mx=\"").append(f.maxDemand[p])
                        .append("\" Fx=\"").append(fxTxt).append("\">\n");
                sb.append("                <Value AIa=\"").append(f.aPlus[p])
                        .append("\" AEa=\"").append(f.aMinus[p])
                        .append("\" R1a=\"").append(f.qi[p])
                        .append("\" R2a=\"").append(f.qii[p])
                        .append("\" R3a=\"").append(f.qiii[p])
                        .append("\" R4a=\"").append(f.qiv[p])
                        .append("\"></Value>\n");
                sb.append("            </S27>\n");
            }
        }

        sb.append("        </Cnt>\n");
        sb.append("    </Cnc>\n");
        sb.append("</Report>");
        return sb.toString();
    }

    private static LocalDateTime parsear(String s) {
        if (s == null) return null;
        try {
            return LocalDateTime.parse(s.trim(), ENTRADA);
        } catch (Exception e) {
            return null;   // "N/A" u otro formato
        }
    }

    /** yyyyMMddHHmmssSSS + W (invierno) o S (verano) según la zona horaria. */
    private static String formatear(LocalDateTime t, ZoneId zona) {
        boolean verano = zona.getRules().isDaylightSavings(t.atZone(zona).toInstant());
        return t.format(SALIDA) + (verano ? "S" : "W");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}