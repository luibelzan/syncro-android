package com.celnet.syncro.export;

import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Exportación de informes IEC-870 a Excel: genera el .xlsx (nombre con el id del contador) en
 * Descargas/Syncro/Reports/IEC y avisa. No se envía por FTP.
 */
public final class IecExcelExport {

    public interface Generador {
        void generar(OutputStream out) throws IOException;
    }

    public interface Nombre {
        /** Nombre del fichero (con .xlsx) para el id del contador indicado. */
        String de(String contador);
    }

    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private IecExcelExport() {
    }

    /** Genera el .xlsx directamente, sin pedir datos; el nombre lleva el id del contador (cnt). */
    public static void exportar(Activity activity, String contador, Nombre nombre, Generador generador) {
        String id = contador != null && !contador.trim().isEmpty() ? contador.trim() : "Contador";
        new Thread(() -> {
            try {
                File carpeta = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "Syncro/Reports/IEC");
                if (!carpeta.exists()) carpeta.mkdirs();
                File fichero = new File(carpeta, nombre.de(id));

                try (FileOutputStream fos = new FileOutputStream(fichero)) {
                    generador.generar(fos);
                }
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        "Informe guardado en Descargas/Syncro/Reports/IEC/" + fichero.getName(),
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        "Error al generar el Excel: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** Texto apto para nombre de fichero. */
    public static String limpiar(String texto) {
        return texto.replaceAll("[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñÜü\\-]+", "_");
    }

    /** "Mes_Año" si las dos fechas ("yyyy/MM/dd HH:mm...") caen en el mismo mes; si no, "dd-MM-yyyy_dd-MM-yyyy". */
    public static String periodo(String desde, String hasta) {
        int[] a = ymd(desde);
        int[] b = ymd(hasta);
        if (a[0] == b[0] && a[1] == b[1]) {
            return MESES[a[1] - 1] + "_" + a[0];
        }
        return String.format(java.util.Locale.US, "%02d-%02d-%04d_%02d-%02d-%04d",
                a[2], a[1], a[0], b[2], b[1], b[0]);
    }

    /** {año, mes, día} de "yyyy/MM/dd HH:mm:ss..." un minuto antes (el fin de un periodo pertenece al anterior). */
    public static int[] fechaMenosUnMinuto(String t) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(t.substring(0, 4)), Integer.parseInt(t.substring(5, 7)) - 1,
                Integer.parseInt(t.substring(8, 10)), Integer.parseInt(t.substring(11, 13)),
                Integer.parseInt(t.substring(14, 16)), 0);
        c.add(Calendar.MINUTE, -1);
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)};
    }

    private static int[] ymd(String t) {
        return new int[]{Integer.parseInt(t.substring(0, 4)), Integer.parseInt(t.substring(5, 7)),
                Integer.parseInt(t.substring(8, 10))};
    }
}