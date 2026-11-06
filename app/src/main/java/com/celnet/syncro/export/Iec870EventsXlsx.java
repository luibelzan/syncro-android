package com.celnet.syncro.export;

import com.celnet.syncro.models.iec870.eventos.Iec870EventFila;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** Informe de eventos IEC-870 en Excel (hoja "Eventos"): Contador, Fecha, SPA, SPQ, SPI y Descripción. */
public final class Iec870EventsXlsx {

    public static final String HOJA = "Eventos";

    private Iec870EventsXlsx() {
    }

    public static void generar(OutputStream out, String contador, List<Iec870EventFila> filas)
            throws IOException {
        MiniXlsx x = new MiniXlsx(HOJA, new double[]{10, 21, 8, 8, 8, 60});
        x.addHeader("Contador", "Fecha", "SPA", "SPQ", "SPI", "Descripcion");
        for (Iec870EventFila f : filas) {
            x.addRow(contador, fecha(f.fh), f.spa, f.spq, f.spi, f.descripcion);
        }
        x.writeTo(out);
    }

    /** "d/M/yy H:mm:ss" -> "yyyy/MM/dd HH:mm:ss" (formato que entiende MiniXlsx y periodo()). */
    public static String normalizar(String fh) {
        if (fh == null) return null;
        String[] dt = fh.trim().split(" ");
        if (dt.length < 2) return null;
        String[] d = dt[0].split("/");
        String[] t = dt[1].split(":");
        if (d.length != 3 || t.length < 2) return null;
        try {
            int y = Integer.parseInt(d[2]);
            if (y < 100) y += 2000;
            return String.format(java.util.Locale.US, "%04d/%02d/%02d %02d:%02d:%02d",
                    y, Integer.parseInt(d[1]), Integer.parseInt(d[0]),
                    Integer.parseInt(t[0]), Integer.parseInt(t[1]),
                    t.length > 2 ? Integer.parseInt(t[2]) : 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static MiniXlsx.ExcelDate fecha(String fh) {
        String n = normalizar(fh);
        return n == null ? null : MiniXlsx.ExcelDate.fromText(n);
    }
}