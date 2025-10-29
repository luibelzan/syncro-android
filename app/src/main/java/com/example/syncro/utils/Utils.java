package com.example.syncro.utils;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;

import gurux.dlms.objects.GXDLMSData;

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

    public static String readSerialNumber(GXDLMSSecureClient2 client) throws Exception {
        System.out.println("Leyendo número de serie...");

        GXDLMSData serialNumber = new GXDLMSData("0.0.96.1.0.255");
        Object value = client.read(serialNumber, 2);  // Atributo 2 = valor

        if (value == null) {
            System.out.println("No se recibió valor.");
            return null;
        }

        if (value instanceof byte[]) {
            byte[] data = (byte[]) value;
            String ascii = new String(data).trim();

            // Si es texto legible (sin \0 ni caracteres raros)
            if (!ascii.isEmpty() && !ascii.matches(".*[\\x00-\\x1F].*")) {
                System.out.println("Serial number (ASCII): " + ascii);
                return ascii;
            } else {
                String hex = bytesToHex(data);
                System.out.println("Serial number (HEX): " + hex);
                return hex;
            }
        } else {
            String str = value.toString();
            System.out.println("Serial number: " + str);
            return str;
        }
    }

    public static String test(Context context) throws Exception {
        DLMSConnection connection = null;
        try {
            connection = DLMSConnection.initializeConnection(context);
            GXDLMSSecureClient2 client = connection.client; // Getter correcto

            String serial = Utils.readSerialNumber(client);
            Log.i("DLMS", "Serial: " + serial);

            return serial; // Éxito

        } catch (Exception e) {
            Log.e("DLMS", "Error en conexión DLMS", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                    Log.i("DLMS", "Conexión cerrada correctamente.");
                } catch (Exception closeEx) {
                    Log.w("DLMS", "Error al cerrar conexión", closeEx);
                }
            }
        }
    }
}
