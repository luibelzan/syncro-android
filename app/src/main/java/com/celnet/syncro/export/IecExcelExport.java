package com.celnet.syncro.export;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Exportación de informes IEC-870 a Excel: pide el CUPS (recordando el último), genera el .xlsx en
 * Descargas/Syncro/Reports y avisa. No se envía por FTP.
 */
public final class IecExcelExport {

    public interface Generador {
        void generar(OutputStream out, String cups) throws IOException;
    }

    public interface Nombre {
        /** Nombre del fichero (con .xlsx) para el CUPS indicado. */
        String de(String cups);
    }

    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private IecExcelExport() {
    }

    public static void exportar(Activity activity, String titulo, Nombre nombre, Generador generador) {
        SharedPreferences prefs = activity.getSharedPreferences("ftp_config", Activity.MODE_PRIVATE);

        EditText editCups = new EditText(activity);
        editCups.setHint("CUPS / nombre del punto de medida");
        editCups.setSingleLine(true);
        editCups.setText(prefs.getString("iec_cups", ""));
        editCups.setSelectAllOnFocus(true);
        int margen = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        LinearLayout contenedor = new LinearLayout(activity);
        contenedor.setPadding(margen, margen / 2, margen, 0);
        contenedor.addView(editCups, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(activity)
                .setTitle(titulo)
                .setMessage("Indique el CUPS que irá en la primera columna del informe.")
                .setView(contenedor)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Exportar", (d, w) -> {
                    String cups = editCups.getText().toString().trim();
                    if (cups.isEmpty()) {
                        Toast.makeText(activity, "El CUPS no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.edit().putString("iec_cups", cups).apply();

                    new Thread(() -> {
                        try {
                            File carpeta = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS), "Syncro/Reports");
                            if (!carpeta.exists()) carpeta.mkdirs();
                            File fichero = new File(carpeta, nombre.de(cups));

                            try (FileOutputStream fos = new FileOutputStream(fichero)) {
                                generador.generar(fos, cups);
                            }
                            activity.runOnUiThread(() -> Toast.makeText(activity,
                                    "Informe guardado en Descargas/Syncro/Reports/" + fichero.getName(),
                                    Toast.LENGTH_LONG).show());
                        } catch (Exception e) {
                            e.printStackTrace();
                            activity.runOnUiThread(() -> Toast.makeText(activity,
                                    "Error al generar el Excel: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .show();
    }

    /** CUPS apto para nombre de fichero. */
    public static String limpiar(String cups) {
        return cups.replaceAll("[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñÜü\\-]+", "_");
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