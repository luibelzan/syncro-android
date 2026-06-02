package com.example.syncro.objects.ids;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.MeterInfo;
import com.example.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;

public class MeterInfoReader {

    public static MeterInfo leerIdentificadores(GXDLMSReader reader) {
        MeterInfo info = new MeterInfo();

        try {
            AppLogger.i("MeterInfo", "Leyendo identificadores del contador...");

            GXDLMSData serialObj = new GXDLMSData("0.0.96.1.0.255");
            reader.read(serialObj, 2);
            info.serial = convertirValor(serialObj.getValue());

            GXDLMSData equipoObj = new GXDLMSData("0.0.96.1.1.255");
            reader.read(equipoObj, 2);
            info.equipo = convertirValor(equipoObj.getValue());

            GXDLMSData tipoObj = new GXDLMSData("0.0.96.1.2.255");
            reader.read(tipoObj, 2);
            info.tipo = convertirValor(tipoObj.getValue());

            GXDLMSData firmwareObj = new GXDLMSData("1.0.0.2.0.255");
            reader.read(firmwareObj, 2);
            info.firmware = convertirValor(firmwareObj.getValue());

            AppLogger.i("MeterInfo", "Lectura completada correctamente.");

        } catch (Exception e) {
            AppLogger.e("MeterInfo", "Error leyendo identificadores: " + e.getMessage());
        }

        return info;
    }

    private static String convertirValor(Object value) {
        if (value == null) return "-";

        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;

            String str = new String(bytes).trim();

            if (str.matches("[\\p{Print}]+")) {
                return str;
            }

            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02X ", b));
            }
            return hex.toString().trim();
        }

        return value.toString();
    }
}