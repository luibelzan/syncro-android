package com.example.syncro.objects;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.utils.Utils;

import gurux.dlms.objects.GXDLMSData;

public class SerialNumberReader {

    public static void readSerialNumer(Context context) throws Exception {
        DLMSConnection con = DLMSConnection.initializeConnection2(context);
        GXDLMSReader reader = con.reader;
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
        } finally {
            DLMSConnection.closeConnection(con.reader, con.serial);
        }
    }

}
