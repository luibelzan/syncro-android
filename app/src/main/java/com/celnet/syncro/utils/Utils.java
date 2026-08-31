package com.celnet.syncro.utils;

import static android.content.ContentValues.TAG;
import static gurux.dlms.objects.enums.ControlState.CONNECTED;
import static gurux.dlms.objects.enums.ControlState.DISCONNECTED;
import static gurux.dlms.objects.enums.ControlState.READY_FOR_RECONNECTION;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.models.UploadResult;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import gurux.dlms.GXArray;
import gurux.dlms.GXDLMSAccessItem;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDateTime;
import gurux.dlms.GXReplyData;
import gurux.dlms.GXStructure;
import gurux.dlms.GXUInt32;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSDisconnectControl;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;
import gurux.dlms.objects.enums.ControlState;
import gurux.dlms.secure.GXDLMSSecureClient;

public class Utils {

    public static void moverABackup(File file, Context context) {

        File downloadsFolder =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);

        File backupFolder =
                new File(downloadsFolder, "Syncro/Backup");

        if (!backupFolder.exists()) {
            boolean created = backupFolder.mkdirs();

            if (created) {
                AppLogger.i("Syncro", "Carpeta Backup creada correctamente");
            } else {
                AppLogger.e("Syncro", "No se pudo crear la carpeta Backup");
            }
        }

        File destino = new File(backupFolder, file.getName());

        try (
                FileInputStream in = new FileInputStream(file);
                FileOutputStream out = new FileOutputStream(destino)
        ) {

            byte[] buffer = new byte[4096];
            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            out.flush();

            boolean deleted = file.delete();

            if (deleted) {
                AppLogger.i("Syncro", "Archivo movido a Backup correctamente: " + file.getName());
            } else {
                AppLogger.e("Syncro", "Archivo copiado pero no se pudo eliminar el original: " + file.getName());
            }

        } catch (Exception e) {
            AppLogger.e("Syncro", "Error moviendo archivo a Backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void gestionarArchivoTrasEnvio(File file, String afterSend, Context context) {

        if ("Eliminar del dispositivo".equals(afterSend)) {

            boolean deleted = file.delete();

            if (deleted) {
                AppLogger.i("Syncro", "Archivo eliminado: " + file.getName());
            } else {
                AppLogger.e("Syncro", "No se pudo eliminar: " + file.getName());
            }
        }
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    // =========================
    // PARSEAR FECHAS XML
    // =========================
    public static String convertirFecha(String fechaEntrada) {

        if (fechaEntrada == null || fechaEntrada.isEmpty()) {
            return "";
        }

        if (fechaEntrada.equals("FFFFFFFFFFFFFFFFFW")) {
            return fechaEntrada;
        }

        DateTimeFormatter outputFormatter =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

        DateTimeFormatter[] formatos = new DateTimeFormatter[] {

                // 2026/01/03 00:00:00.000W
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS'W'"),

                // 2026/08/24 14:00:00.000S
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS'S'"),

                // 2026/03/11 04:30:00
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),

                // 4/27/26 12:00:00 AM
                DateTimeFormatter.ofPattern("M/d/yy h:mm:ss a"),

                // 18/5/26 0:00:00
                DateTimeFormatter.ofPattern("d/M/yy H:mm:ss"),

                // 2026/18/05 00:00:00.000W
                DateTimeFormatter.ofPattern("yyyy/dd/MM HH:mm:ss.SSS'W'")

        };

        for (DateTimeFormatter formatter : formatos) {

            try {

                LocalDateTime fecha =
                        LocalDateTime.parse(fechaEntrada, formatter);

                return fecha.format(outputFormatter) + "S";

            } catch (DateTimeParseException e) {
                // probar siguiente formato
            }
        }

        throw new IllegalArgumentException(
                "Formato de fecha no soportado: " + fechaEntrada);
    }

    // =========================
// SUBIDA SFTP
// =========================
    public static UploadResult subirArchivoSFTPConDetalle(Context context, File file) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "ftp_config",
                    Context.MODE_PRIVATE
            );

            String host = prefs.getString("dir", "");
            int port = Integer.parseInt(prefs.getString("port", "22"));
            String user = prefs.getString("user", "");
            String pass = prefs.getString("pass", "");
            String folder = prefs.getString("folder", "/");

            AppLogger.i("Utils", "SFTP: conectando a " + host + ":" + port
                    + " usuario=" + user + " carpeta=" + folder);

            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);

            session.setPassword(pass);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(10000);

            Channel channel = session.openChannel("sftp");
            channel.connect();

            ChannelSftp sftp = (ChannelSftp) channel;

            sftp.cd(folder);
            sftp.put(file.getAbsolutePath(), file.getName());

            sftp.exit();
            session.disconnect();

            AppLogger.i("Utils", "SFTP: archivo subido correctamente: " + file.getName());
            return UploadResult.ok();

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            AppLogger.e("Utils", "SFTP: error al subir " + file.getName() + " -> " + msg);
            e.printStackTrace();
            return UploadResult.error(msg);
        }
    }

    public static boolean subirArchivoSFTP(Context context, File file) {
        return subirArchivoSFTPConDetalle(context, file).success;
    }

    // =========================
// SUBIDA FTP
// =========================
    public static UploadResult subirArchivoFTPConDetalle(Context context, File file) {

        try {
            SharedPreferences prefs =
                    context.getSharedPreferences("ftp_config", Context.MODE_PRIVATE);

            String host = prefs.getString("dir", "");
            int port = Integer.parseInt(prefs.getString("port", "21"));
            String user = prefs.getString("user", "");
            String pass = prefs.getString("pass", "");
            String folder = prefs.getString("folder", "/folder");

            AppLogger.i("Utils", "FTP: conectando a " + host + ":" + port
                    + " usuario=" + user + " carpeta=" + folder);

            FTPClient ftp = new FTPClient();

            ftp.connect(host, port);

            int replyCode = ftp.getReplyCode();
            AppLogger.i("Utils", "FTP: connect reply=" + replyCode + " " + ftp.getReplyString());

            if (!ftp.login(user, pass)) {
                String msg = "Login rechazado (código " + ftp.getReplyCode() + "): " + ftp.getReplyString();
                AppLogger.e("Utils", "FTP: " + msg);
                ftp.disconnect();
                return UploadResult.error(msg);
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            if (!ftp.changeWorkingDirectory(folder)) {
                String msg = "No se pudo acceder a la carpeta '" + folder
                        + "' (código " + ftp.getReplyCode() + "): " + ftp.getReplyString();
                AppLogger.e("Utils", "FTP: " + msg);
                ftp.logout();
                ftp.disconnect();
                return UploadResult.error(msg);
            }

            FileInputStream fis = new FileInputStream(file);

            boolean success = ftp.storeFile(file.getName(), fis);

            fis.close();

            String replyMsg = ftp.getReplyString();
            ftp.logout();
            ftp.disconnect();

            if (success) {
                AppLogger.i("Utils", "FTP: archivo subido correctamente: " + file.getName());
                return UploadResult.ok();
            } else {
                String msg = "storeFile falló (código " + ftp.getReplyCode() + "): " + replyMsg;
                AppLogger.e("Utils", "FTP: " + msg);
                return UploadResult.error(msg);
            }

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            AppLogger.e("Utils", "FTP: error al subir " + file.getName() + " -> " + msg);
            e.printStackTrace();
            return UploadResult.error(msg);
        }
    }

    public static boolean subirArchivoFTP(Context context, File file) {
        return subirArchivoFTPConDetalle(context, file).success;
    }

    // =========================
// SUBIDA FTPS
// =========================
    public static UploadResult subirArchivoFTPSConDetalle(Context context, File file) {

        try {
            SharedPreferences prefs =
                    context.getSharedPreferences("ftp_config", Context.MODE_PRIVATE);

            String host = prefs.getString("dir", "");
            int port = Integer.parseInt(prefs.getString("port", "21"));
            String user = prefs.getString("user", "");
            String pass = prefs.getString("pass", "");
            String folder = prefs.getString("folder", "/folder");

            AppLogger.i("Utils", "FTPS: conectando a " + host + ":" + port
                    + " usuario=" + user + " carpeta=" + folder);

            FTPSClient ftps = new FTPSClient();

            ftps.connect(host, port);

            int replyCode = ftps.getReplyCode();
            AppLogger.i("Utils", "FTPS: connect reply=" + replyCode + " " + ftps.getReplyString());

            if (!ftps.login(user, pass)) {
                String msg = "Login rechazado (código " + ftps.getReplyCode() + "): " + ftps.getReplyString();
                AppLogger.e("Utils", "FTPS: " + msg);
                ftps.disconnect();
                return UploadResult.error(msg);
            }

            ftps.enterLocalPassiveMode();
            ftps.setFileType(FTP.BINARY_FILE_TYPE);

            ftps.execPBSZ(0);
            ftps.execPROT("P");

            if (!ftps.changeWorkingDirectory(folder)) {
                String msg = "No se pudo acceder a la carpeta '" + folder
                        + "' (código " + ftps.getReplyCode() + "): " + ftps.getReplyString();
                AppLogger.e("Utils", "FTPS: " + msg);
                ftps.logout();
                ftps.disconnect();
                return UploadResult.error(msg);
            }

            FileInputStream fis = new FileInputStream(file);

            boolean success = ftps.storeFile(file.getName(), fis);

            fis.close();

            String replyMsg = ftps.getReplyString();
            ftps.logout();
            ftps.disconnect();

            if (success) {
                AppLogger.i("Utils", "FTPS: archivo subido correctamente: " + file.getName());
                return UploadResult.ok();
            } else {
                String msg = "storeFile falló (código " + ftps.getReplyCode() + "): " + replyMsg;
                AppLogger.e("Utils", "FTPS: " + msg);
                return UploadResult.error(msg);
            }

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            AppLogger.e("Utils", "FTPS: error al subir " + file.getName() + " -> " + msg);
            e.printStackTrace();
            return UploadResult.error(msg);
        }
    }

    public static boolean subirArchivoFTPS(Context context, File file) {
        return subirArchivoFTPSConDetalle(context, file).success;
    }

    private void guardarXML(Context context, String xmlContenido, String nombreArchivo) {
        try {
            File file = new File(context.getFilesDir(), nombreArchivo);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(xmlContenido.getBytes());
            fos.close();
            Toast.makeText(context, "Archivo guardado en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error guardando el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
