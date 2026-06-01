package com.example.syncro.objects.params;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.utils.AppLogger;
import com.example.syncro.utils.Utils;

import gurux.dlms.objects.GXDLMSData;

public class SerialNumberReader {

    public static String readSerialNumer(GXDLMSReader reader) throws Exception {
        String result = null;
        try {
            AppLogger.i("Syncro", "Leyendo numero de serie");
            GXDLMSData serialNumber = new GXDLMSData("0.0.96.1.0.255");
            Object value = reader.read(serialNumber, 2);

            if (value instanceof byte[]) {
                String serialStr = new String((byte[]) value).trim();

                if (serialStr.isEmpty() || serialStr.contains("\0")) {
                    serialStr = Utils.bytesToHex((byte[]) value);
                }

                result = serialStr;

            } else if (value != null) {
                result = value.toString();
            } else {
                result = null;
            }

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al leer el Serial Number: " + e.getMessage());
            throw e;
        }
        return result;
    }

}