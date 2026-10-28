package com.celnet.syncro.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor mínimo de .xlsx (una hoja) sin dependencias: solo java.util.zip. Texto como
 * cadenas en línea, números y fechas como números; la primera fila lleva el estilo de cabecera
 * (negrita blanca sobre azul, centrada, con borde) y las fechas el formato
 * {@code yyyy-mm-dd h:mm:ss}.
 */
public class MiniXlsx {

    private static final int STYLE_HEADER = 1;
    private static final int STYLE_DATE = 2;

    private final String sheetName;
    private final double[] widths;
    private final StringBuilder rows = new StringBuilder(1 << 16);
    private int rowCount = 0;
    private int maxCols = 0;

    /** @param widths anchos de columna (en caracteres) de la A en adelante; 0 = el de por defecto */
    public MiniXlsx(String sheetName, double[] widths) {
        this.sheetName = sheetName;
        this.widths = widths;
    }

    /** Añade la fila de cabecera. */
    public void addHeader(String... titulos) {
        beginRow();
        for (int i = 0; i < titulos.length; i++) {
            appendString(i, titulos[i], STYLE_HEADER);
        }
        endRow(titulos.length);
    }

    /**
     * Añade una fila de datos. Cada celda puede ser: {@link String} (texto), {@link Number}
     * (número) o {@link ExcelDate} (fecha y hora).
     */
    public void addRow(Object... celdas) {
        beginRow();
        for (int i = 0; i < celdas.length; i++) {
            Object v = celdas[i];
            if (v == null) continue;
            if (v instanceof ExcelDate) {
                rows.append("<c r=\"").append(ref(i, rowCount + 1)).append("\" s=\"").append(STYLE_DATE)
                        .append("\"><v>").append(((ExcelDate) v).serial).append("</v></c>");
            } else if (v instanceof Number) {
                rows.append("<c r=\"").append(ref(i, rowCount + 1)).append("\"><v>")
                        .append(v).append("</v></c>");
            } else {
                appendString(i, v.toString(), 0);
            }
        }
        endRow(celdas.length);
    }

    /** Fecha y hora local (sin zona), convertida al número de serie de Excel. */
    public static final class ExcelDate {
        final String serial;

        public ExcelDate(int y, int mo, int d, int h, int mi, int s) {
            long dias = diasDesdeCivil(y, mo, d) - diasDesdeCivil(1899, 12, 30);
            double frac = (h * 3600.0 + mi * 60.0 + s) / 86400.0;
            this.serial = frac == 0 ? Long.toString(dias) : Double.toString(dias + frac);
        }

        /** Desde "yyyy/MM/dd HH:mm:ss..." (lo que sigue a los segundos, como la letra S/W, se ignora). */
        public static ExcelDate fromText(String t) {
            int seg = t.length() >= 19 && t.charAt(16) == ':' ? Integer.parseInt(t.substring(17, 19)) : 0;
            return new ExcelDate(
                    Integer.parseInt(t.substring(0, 4)), Integer.parseInt(t.substring(5, 7)),
                    Integer.parseInt(t.substring(8, 10)), Integer.parseInt(t.substring(11, 13)),
                    Integer.parseInt(t.substring(14, 16)), seg);
        }

        /** Días desde 1970-01-01 (algoritmo de H. Hinnant). */
        private static long diasDesdeCivil(int y, int m, int d) {
            y -= m <= 2 ? 1 : 0;
            long era = (y >= 0 ? y : y - 399) / 400;
            long yoe = y - era * 400;
            long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
            long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
            return era * 146097 + doe - 719468;
        }
    }

    /** Escribe el libro completo. No cierra {@code out}. */
    public void writeTo(OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        entry(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                        + "</Types>");
        entry(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                        + "</Relationships>");
        entry(zip, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                        + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                        + "<sheets><sheet name=\"" + esc(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                        + "</workbook>");
        entry(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                        + "</Relationships>");
        entry(zip, "xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"yyyy\\-mm\\-dd\\ h:mm:ss\"/></numFmts>"
                        + "<fonts count=\"2\">"
                        + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                        + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                        + "</fonts>"
                        + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
                        + "<fill><patternFill patternType=\"gray125\"/></fill>"
                        + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF4472C4\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                        + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
                        + "<border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/><diagonal/></border></borders>"
                        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                        + "<cellXfs count=\"3\">"
                        + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
                        + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                        + "</cellXfs>"
                        + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                        + "</styleSheet>");

        StringBuilder sh = new StringBuilder(rows.length() + 1024);
        sh.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                .append("<dimension ref=\"A1:").append(ref(Math.max(maxCols, 1) - 1, Math.max(rowCount, 1))).append("\"/>");
        if (widths != null && widths.length > 0) {
            sh.append("<cols>");
            for (int i = 0; i < widths.length; i++) {
                if (widths[i] > 0) {
                    sh.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                            .append("\" width=\"").append(widths[i]).append("\" customWidth=\"1\"/>");
                }
            }
            sh.append("</cols>");
        }
        sh.append("<sheetData>").append(rows).append("</sheetData></worksheet>");
        entry(zip, "xl/worksheets/sheet1.xml", sh.toString());
        zip.finish();
        zip.flush();
    }

    // ------------------------------------------------------------------

    private void beginRow() {
        rows.append("<row r=\"").append(rowCount + 1).append("\">");
    }

    private void endRow(int cols) {
        rows.append("</row>");
        rowCount++;
        if (cols > maxCols) maxCols = cols;
    }

    private void appendString(int col, String s, int style) {
        rows.append("<c r=\"").append(ref(col, rowCount + 1)).append("\" t=\"inlineStr\"");
        if (style != 0) rows.append(" s=\"").append(style).append("\"");
        rows.append("><is><t xml:space=\"preserve\">").append(esc(s)).append("</t></is></c>");
    }

    /** Referencia de celda: columna 0 = A. */
    static String ref(int col, int row) {
        StringBuilder c = new StringBuilder();
        for (int n = col; n >= 0; n = n / 26 - 1) {
            c.insert(0, (char) ('A' + n % 26));
        }
        return c.append(row).toString();
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                default:
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') b.append(ch);
            }
        }
        return b.toString();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}