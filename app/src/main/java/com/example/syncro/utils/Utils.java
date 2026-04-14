package com.example.syncro.utils;

import static android.content.ContentValues.TAG;
import static gurux.dlms.objects.enums.ControlState.CONNECTED;
import static gurux.dlms.objects.enums.ControlState.DISCONNECTED;
import static gurux.dlms.objects.enums.ControlState.READY_FOR_RECONNECTION;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;


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
    // SUBIDA SFTP
    // =========================
    public static void subirArchivoSFTP(Context context, File file) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("ftp_config", Context.MODE_PRIVATE);

                String host = prefs.getString("dir", "");
                int port = Integer.parseInt(prefs.getString("port", "22"));
                String user = prefs.getString("user", "");
                String pass = prefs.getString("pass", "");
                String folder = prefs.getString("folder", "/");

                com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
                com.jcraft.jsch.Session session = jsch.getSession(user, host, port);

                session.setPassword(pass);

                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect(10000);

                com.jcraft.jsch.Channel channel = session.openChannel("sftp");
                channel.connect();

                com.jcraft.jsch.ChannelSftp sftp = (com.jcraft.jsch.ChannelSftp) channel;

                sftp.cd(folder);
                sftp.put(file.getAbsolutePath(), file.getName());

                sftp.exit();
                session.disconnect();

                // UI Thread
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Subido por SFTP", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error SFTP: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // =========================
    // SUBIDA FTP
    // =========================
    public static void subirArchivoFTP(Context context, File file) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("ftp_config", Context.MODE_PRIVATE);

                String host = prefs.getString("dir", "");
                int port = Integer.parseInt(prefs.getString("port", "21"));
                String user = prefs.getString("user", "");
                String pass = prefs.getString("pass", "");
                String folder = prefs.getString("folder", "/folder");

                org.apache.commons.net.ftp.FTPClient ftp = new org.apache.commons.net.ftp.FTPClient();

                ftp.connect(host, port);
                ftp.login(user, pass);

                ftp.enterLocalPassiveMode(); // 🔥 CLAVE
                ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

                ftp.changeWorkingDirectory(folder);

                FileInputStream fis = new FileInputStream(file);

                boolean success = ftp.storeFile(file.getName(), fis);

                fis.close();
                ftp.logout();
                ftp.disconnect();

                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(context, "Subido por FTP", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Error FTP", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error FTP: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // =========================
    // SUBIDA FTPS
    // =========================
    public static void subirArchivoFTPS(Context context, File file) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("ftp_config", Context.MODE_PRIVATE);

                String host = prefs.getString("dir", "");
                int port = Integer.parseInt(prefs.getString("port", "21"));
                String user = prefs.getString("user", "");
                String pass = prefs.getString("pass", "");
                String folder = prefs.getString("folder", "/folder");

                org.apache.commons.net.ftp.FTPSClient ftps =
                        new org.apache.commons.net.ftp.FTPSClient();

                ftps.connect(host, port);
                ftps.login(user, pass);

                ftps.enterLocalPassiveMode();
                ftps.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

                ftps.execPBSZ(0);
                ftps.execPROT("P");

                ftps.changeWorkingDirectory(folder);

                FileInputStream fis = new FileInputStream(file);

                boolean success = ftps.storeFile(file.getName(), fis);

                fis.close();
                ftps.logout();
                ftps.disconnect();

                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(context, "Subido por FTPS", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Error FTPS", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error FTPS: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
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

    public static void readSerialNumer(GXDLMSReader reader) throws Exception {

        try {
            System.out.println("Leyendo numero de serie");
            GXDLMSData serialNumber = new GXDLMSData("0.0.96.1.0.255");
            Object value = reader.read(serialNumber, 2); // Attribute 2 is the time value

            // Handle the value
            if (value instanceof byte[]) {
                // Try converting to ASCII string
                String serialStr = new String((byte[]) value).trim(); // Trim to remove padding
                if (serialStr.isEmpty() || serialStr.contains("\0")) {
                    // If not a valid ASCII string, show as hex
                    serialStr = Utils.bytesToHex((byte[]) value);
                    System.out.println("Serial number (hex): " + serialStr);
                } else {
                    System.out.println("Serial number: " + serialStr);
                }
            } else if (value != null) {
                System.out.println("Serial number: " + value.toString());
            } else {
                System.out.println("No serial number received or value is null.");
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer el Serial Number", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }

    public static void readDate(GXDLMSReader reader) throws Exception {
        try {
            System.out.println("Leyendo fecha y hora del equipo");
            GXDLMSClock clock = new GXDLMSClock("0.0.1.0.0.255");
            Object value = reader.read(clock, 2); // Attribute 2 is the time value

            // Handle the value
            if (value instanceof GXDateTime) {
                GXDateTime dateTime = (GXDateTime) value;
                System.out.println("Current meter date/time: " + dateTime.toString());
            } else {
                System.out.println("Unexpected value format: " + value);
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Fecha y hora del equipo", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }

    public static void readStandarEventLog(GXDLMSReader reader) throws Exception {
        try {
            System.out.println("Leyendo perfil de cierres mensuales...\n");

            GXDLMSProfileGeneric profile = new GXDLMSProfileGeneric("0.0.99.98.0.255");

            // Leer columnas del perfil (capture objects)
            reader.read(profile, 3);
            System.out.println("Objetos capturados:");
            for (java.util.Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry : profile.getCaptureObjects()) {
                GXDLMSObject obj = entry.getKey();
                System.out.println(" - " + obj.getLogicalName() + " (" + obj.getObjectType() + ")");
            }

            // Leer el buffer (cierres mensuales)
            reader.read(profile, 2);
            Object buffer = profile.getBuffer();

            // Procesar los datos recibidos
            if (buffer instanceof Object[]) {
                Object[] entries = (Object[]) buffer;
                System.out.println("\nSe recibieron " + entries.length + " registros del perfil mensual.\n");

                for (Object entry : entries) {
                    if (entry instanceof Object[]) {
                        Object[] row = (Object[]) entry;
                        System.out.println("---- Registro ----");
                        for (Object value : row) {
                            if (value instanceof GXDateTime) {
                                GXDateTime dt = (GXDateTime) value;
                                System.out.println("Fecha/Hora: " + dt.toFormatString());
                            } else {
                                System.out.println("Valor: " + value);
                            }
                        }
                    } else {
                        System.out.println("Entrada inesperada: " + entry);
                    }
                }

                System.out.println("\nEstructura del perfil:");
                int index = 1;
                for (Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry : profile.getCaptureObjects()) {
                    GXDLMSObject obj = entry.getKey();
                    GXDLMSCaptureObject attr = entry.getValue();
                    System.out.printf(" %d) %s (%s) atributo %d%n",
                            index++, obj.getLogicalName(), obj.getObjectType(), attr.getAttributeIndex());
                }

            } else {
                System.out.println("Tipo de buffer inesperado: " + buffer.getClass());
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Standar Event Log", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }

    public static void readBillingDataContract1(GXDLMSReader reader) throws Exception {
        try {
            System.out.println("Leyendo perfil de facturación (Data Billing) del contrato 1...\n");

            // Objeto del perfil de facturación del contrato 1
            GXDLMSProfileGeneric billingProfile = new GXDLMSProfileGeneric("0.0.98.1.1.255");

            // 1️⃣ Leer las columnas del perfil
            reader.read(billingProfile, 3);
            System.out.println("Objetos capturados:");
            for (Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry : billingProfile.getCaptureObjects()) {
                GXDLMSObject obj = entry.getKey();
                GXDLMSCaptureObject attr = entry.getValue();
                System.out.printf(" - %s (%s), atributo %d%n",
                        obj.getLogicalName(), obj.getObjectType(), attr.getAttributeIndex());
            }

            // 2️⃣ Leer el buffer completo
            reader.read(billingProfile, 2);
            Object buffer = billingProfile.getBuffer();

            // 3️⃣ Procesar registros
            if (buffer instanceof Object[]) {
                Object[] entries = (Object[]) buffer;
                System.out.println("\nSe recibieron " + entries.length + " registros del perfil de facturación.\n");

                for (Object entry : entries) {
                    if (entry instanceof Object[]) {
                        Object[] row = (Object[]) entry;
                        System.out.println("---- Registro de Facturación ----");

                        for (int i = 0; i < row.length && i < billingProfile.getCaptureObjects().size(); i++) {
                            Object value = row[i];
                            GXDLMSObject obj = billingProfile.getCaptureObjects().get(i).getKey();
                            GXDLMSCaptureObject attr = billingProfile.getCaptureObjects().get(i).getValue();

                            String obis = obj.getLogicalName();
                            System.out.printf("OBIS %s (Atributo %d) -> ", obis, attr.getAttributeIndex());

                            // Manejo según tipo de dato
                            if (value == null) {
                                System.out.println("Valor: <nulo>");
                            } else if (value instanceof GXDateTime) {
                                GXDateTime dt = (GXDateTime) value;
                                System.out.println("Fecha/Hora: " + dt.toFormatString());
                            } else if (value instanceof byte[]) {
                                byte[] bytes = (byte[]) value;
                                // Intentar mostrar texto si es legible, sino hex
                                String text = new String(bytes, StandardCharsets.UTF_8).trim();
                                boolean legible = text.chars().allMatch(ch -> ch >= 32 && ch <= 126);
                                if (legible && !text.isEmpty()) {
                                    System.out.println("Valor (texto): " + text);
                                } else {
                                    System.out.println("Valor (hex): " + HexFormat.of().formatHex(bytes));
                                }
                            } else {
                                System.out.println("Valor: " + value);
                            }
                        }
                    } else {
                        System.out.println("Entrada inesperada: " + entry);
                    }
                }
            } else {
                System.out.println("Tipo de buffer inesperado: " + buffer.getClass());
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Billing Data Contract 1", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }

    public static void controlDisconnectMode(GXDLMSReader reader, GXDLMSSecureClient2 client, boolean connect) throws Exception {
        try {
            System.out.println("\n=== CONTROL DE CONEXIÓN / DESCONEXIÓN ===");

            GXDLMSDisconnectControl dc = new GXDLMSDisconnectControl("0.0.96.3.10.255");

            // Leer atributos básicos
            reader.read(dc, 2); // output_state
            reader.read(dc, 3); // control_state
            reader.read(dc, 4); // control_mode

            System.out.println("Estado inicial del relé:");
            System.out.println(" - Output State (físico): " + (dc.getOutputState() ? "Conectado" : "Desconectado"));
            System.out.println(" - Control State (lógico): " + controlStateToString(dc.getControlState()));
            System.out.println(" - Control Mode: " + dc.getControlMode());

            int methodId = connect ? 2 : 1;
            String action = connect ? "reconectar" : "desconectar";
            System.out.println("\nIntentando " + action + "...");

            // Crear solicitud con parámetros específicos
            byte[] parameters = new byte[] { 0x01, 0x0F, 0x00 }; // Parámetros usados en el software funcional
            byte[][] data = client.method(dc, methodId, parameters, DataType.OCTET_STRING);

            GXReplyData reply = new GXReplyData();

            for (byte[] frame : data) {
                reply.clear();
                reader.readDLMSPacket(frame, reply);
                if (reply.getError() != 0) {
                    throw new RuntimeException("Error DLMS en acción: " + reply.getError());
                }
            }
            // Leer nuevamente el estado
            reader.read(dc, 2);
            reader.read(dc, 3);

            System.out.println("\nNuevo estado del relé:");
            System.out.println(" - Output State: " + (dc.getOutputState() ? "Conectado" : "Desconectado"));
            System.out.println(" - Control State: " + controlStateToString(dc.getControlState()));
        } catch (Exception e) {
            Log.e("DLMS", "Error al conectar/desconectar", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }

    private static String controlStateToString(ControlState state) {
        if (state == null) {
            return "Desconocido (null)";
        }

        switch (state) {
            case DISCONNECTED:
                return "Desconectado";
            case CONNECTED:
                return "Conectado";
            case READY_FOR_RECONNECTION:
                return "Listo para reconexión manual";
            default:
                return "Desconocido (" + state + ")";
        }
    }

    public static void syncClock(GXDLMSReader reader, GXDLMSSecureClient2 client) throws Exception {
        try {
            System.out.println("\n=== Sincronizacion de fecha y hora ===");
            GXDLMSClock clock = new GXDLMSClock();
            clock.setLogicalName("0.0.1.0.0.255");

            Object tzCounterObj = reader.read(clock, 3); // Attribute 3 = TimeZone
            int tzCounter = (tzCounterObj instanceof Number) ? ((Number) tzCounterObj).intValue() : 0;
            //readDate(this);
            System.out.println("TimeZone del contador (minutos respecto a UTC): " + tzCounter);

        /*
        // 3️⃣ Leer TimeZone del PC
        TimeZone tzLocal = TimeZone.getDefault();
        int tzLocalOffset = tzLocal.getOffset(System.currentTimeMillis()) / 60000; // minutos
        System.out.println("Zona horaria PC (minutos respecto a UTC): " + tzLocalOffset);

        // 4️⃣ Intentar ajustar TimeZone del contador al del PC
        boolean tzUpdated = false;
        clock.setTimeZone(120); // Gurux invierte el signo
        byte[][] tzWrite = client.write(clock, 3);
        GXReplyData tzReply = new GXReplyData();
        for (byte[] frame : tzWrite) {
            tzReply.clear();
            reader.readDLMSPacket(frame, tzReply);
            if (tzReply.getError() != 0) {
                throw new RuntimeException("Error escribiendo TimeZone: " + tzReply.getError());
            }
        }

        // Verificar si el TimeZone se actualizó
        Object newTzObj = reader.read(clock, 3);
        int newTzValue = (newTzObj instanceof Number) ? ((Number) newTzObj).intValue() : 0;
        if (newTzValue == tzLocalOffset) { // Gurux invierte el signo, así que comparamos con -tzLocalOffset
            System.out.println("TimeZone del contador actualizado a: " + newTzObj);
            tzUpdated = true;
        } else {
            System.out.println(
                    "⚠️ No se pudo actualizar el TimeZone del contador. Usando TimeZone actual: " + newTzValue);
        }

        // 5️⃣ Calcular hora ajustada
        long nowMillis = System.currentTimeMillis();
        long adjustedMillis;
        if (tzUpdated) {
            // Si el TimeZone se actualizó, escribir la hora en UTC
            adjustedMillis = nowMillis - (tzLocalOffset * 60L * 1000L); // Convertir a UTC
        } else {
            // Si el TimeZone no se actualizó, ajustar la hora para que el contador muestre
            // la hora correcta con su TimeZone actual
            adjustedMillis = nowMillis + ((tzCounter - tzLocalOffset) * 60L * 1000L);
        }
        Date adjustedTime = new Date(adjustedMillis);
        */
            Date now = new Date();

            // 6️⃣ Escribir hora ajustada
            clock.setTime(now);
            byte[][] timeWrite = client.write(clock, 2);
            GXReplyData timeReply = new GXReplyData();
            for (byte[] frame : timeWrite) {
                timeReply.clear();
                reader.readDLMSPacket(frame, timeReply);
                if (timeReply.getError() != 0) {
                    throw new RuntimeException("Error escribiendo hora: " + timeReply.getError());
                }
            }

            // 7️⃣ Verificación
            Object newTime = reader.read(clock, 2);
            Object finalTz = reader.read(clock, 3);
            //int finalTzValue = (finalTz instanceof Number) ? ((Number) finalTz).intValue() : 0;
            System.out.println("Nueva hora del contador: " + newTime);
            System.out.println("Nuevo TimeZone del contador: " + finalTz);
            //System.out.println("Hora del PC: " + new Date());
            System.out.println("✅ Hora del contador sincronizada correctamente.");
        } catch (Exception e) {
            Log.e("DLMS", "Error al sincronizar la fecha y hora del equipo", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }


}
