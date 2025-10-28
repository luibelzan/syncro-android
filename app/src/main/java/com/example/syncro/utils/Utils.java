package com.example.syncro.utils;

import com.example.syncro.client.GXDLMSReader;

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

    public static void readSerialNumer(GXDLMSReader reader) throws Exception {
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
    }
}
