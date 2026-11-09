package com.celnet.syncro.export;

import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

import com.celnet.syncro.IecFtpActivity;
import com.celnet.syncro.IecStgActivity;
import com.celnet.syncro.models.UploadResult;
import com.celnet.syncro.utils.AppLogger;
import com.celnet.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Exportación de informes IEC-870 a Excel: genera el .xlsx (nombre con el id del contador) en
 * Descargas/Syncro/Reports/IEC y, según la configuración STG de IEC (afterGenerate/afterSend),
 * lo envía por FTP con la configuración FTP de IEC.
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

    private static final String SOLO_GUARDAR = "Solo guardar para enviar al FTP mas tarde";
    private static final String DEFECTO_AFTER_GENERATE = "Guardar e intentar enviar al FTP inmediatamente";
    private static final String DEFECTO_AFTER_SEND = "Mover a la carpeta de backup del dispositivo";

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
                String ruta = "Descargas/Syncro/Reports/IEC/" + fichero.getName();

                // Configuración STG de IEC: qué hacer al generar y tras enviar
                IecStgActivity.Config stg = IecStgActivity.cargar(activity);
                String afterGenerate = stg.afterGenerate != null ? stg.afterGenerate : DEFECTO_AFTER_GENERATE;
                String afterSend = stg.afterSend != null ? stg.afterSend : DEFECTO_AFTER_SEND;

                AppLogger.i("IecExport", "Guardado " + ruta + " | afterGenerate='" + afterGenerate
                        + "' afterSend='" + afterSend + "'");

                if (SOLO_GUARDAR.equals(afterGenerate)) {
                    AppLogger.i("IecExport", "Solo guardar: no se envía por FTP");
                    activity.runOnUiThread(() -> Toast.makeText(activity,
                            "Informe guardado para envío posterior en " + ruta,
                            Toast.LENGTH_LONG).show());
                    return;
                }

                // Subida con la configuración FTP de IEC (no la de DLMS)
                IecFtpActivity.Config ftp = IecFtpActivity.cargar(activity);
                AppLogger.i("IecExport", "Enviando por FTP IEC: protocolo='" + ftp.protocolo
                        + "' host='" + ftp.dir + "' puerto=" + ftp.port + " usuario='" + ftp.user
                        + "' carpeta='" + ftp.folder + "'");
                UploadResult res = Utils.subirArchivoSegunProtocolo(
                        activity, fichero, ftp.protocolo, IecFtpActivity.PREFS_NAME);
                AppLogger.i("IecExport", res.success ? "Envío correcto"
                        : "Envío fallido: " + res.errorMessage);
                if (res.success) {
                    Utils.gestionarArchivoTrasEnvio(fichero, afterSend, activity);
                }
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        res.success
                                ? "Informe guardado y enviado correctamente (" + fichero.getName() + ")"
                                : "Informe guardado en " + ruta + ", pero falló el envío: " + res.errorMessage,
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