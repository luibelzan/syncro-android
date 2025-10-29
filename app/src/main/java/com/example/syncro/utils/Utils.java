package com.example.syncro.utils;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSClock;
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

    public static void readSerialNumber(GXDLMSSecureClient2 client) throws Exception {
        System.out.println("Leyendo número de serie...");

        GXDLMSData serialNumber = new GXDLMSData("0.0.96.1.0.255");
        Object value = client.read(serialNumber, 2);  // Atributo 2 = valor

        // Handle the value
        if (value instanceof byte[]) {
            // Try converting to ASCII string
            String serialStr = new String((byte[]) value).trim(); // Trim to remove padding
            if (serialStr.isEmpty() || serialStr.contains("\0")) {
                // If not a valid ASCII string, show as hex
                serialStr = Utils.bytesToHex((byte[]) value);
                System.out.println("Serial number (hex): " + serialStr);
            } else {
                //serialStr = Utils.bytesToHex((byte[]) value);
                System.out.println("Serial number: " + serialStr);
            }
        } else if (value != null) {
            System.out.println("Serial number: " + value.toString());
        } else {
            System.out.println("No serial number received or value is null.");
        }
    }

    public static void readDate(GXDLMSReader reader) throws Exception {
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
    }


    public static void test(Context context) throws Exception {
        DLMSConnection connection = null;
        try {
            connection = DLMSConnection.initializeConnection2(context);
            //GXDLMSSecureClient2 client = connection.client; // Getter correcto

            Utils.readDate(connection.reader);
            //Log.i("DLMS", "Serial: " + serial);

            //return serial; // Éxito

        } catch (Exception e) {
            Log.e("DLMS", "Error en conexión DLMS", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            DLMSConnection.closeConnection(connection.reader, connection.serial);
        }
    }
}
