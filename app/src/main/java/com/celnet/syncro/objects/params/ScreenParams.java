package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class ScreenParams {

    public static class ScreenParamsResult {
        public int tiempoDesplazamiento = -1;   // segundos
        public String modoDesplazamiento = "N/A";
    }

    /**
     * Lee los parámetros de pantalla del medidor:
     *  - Tiempo de desplazamiento (scroll interval): OBIS 0.0.94.34.110.255 attr 2 → UINT32
     *  - Modo de desplazamiento (scroll mode):       OBIS 0.0.96.1.7.255    attr 2 → OCTET_STRING (1 byte ASCII)
     */
    public static ScreenParamsResult leerParametrosPantalla(GXDLMSReader reader) {
        ScreenParamsResult result = new ScreenParamsResult();

        // --- Tiempo de desplazamiento ---
        try {
            AppLogger.i("ScreenParams", "Reading Scroll info");
            GXDLMSRegister scrollObj = new GXDLMSRegister("0.0.94.34.110.255");
            reader.read(scrollObj, 2);
            Object val = scrollObj.getValue();  // devuelve Number directamente
            if (val instanceof Number) {
                result.tiempoDesplazamiento = ((Number) val).intValue();
            } else if (val instanceof byte[]) {
                byte[] b = (byte[]) val;
                int tiempo = 0;
                for (byte x : b) tiempo = (tiempo << 8) | (x & 0xFF);
                result.tiempoDesplazamiento = tiempo;
            }
            //AppLogger.i("ScreenParams", "Scroll info OK → " + result.tiempoDesplazamiento + " s");

        } catch (Exception e) {
            AppLogger.e("ScreenParams", "Error reading Scroll info: " + e.getMessage());
        }

        // --- Modo de desplazamiento ---
        try {
            AppLogger.i("ScreenParams", "Reading Meter Led info");
            GXDLMSData modoObj = new GXDLMSData("0.0.96.1.7.255");
            reader.read(modoObj, 2);

            Object val = modoObj.getValue();
            if (val instanceof byte[]) {
                byte[] b = (byte[]) val;
                if (b.length >= 1) {
                    result.modoDesplazamiento = String.valueOf((char) (b[0] & 0xFF));
                }
            } else if (val instanceof String) {
                String s = (String) val;
                result.modoDesplazamiento = s.isEmpty() ? "N/A" : String.valueOf(s.charAt(0));
            } else if (val instanceof Number) {
                // Por si Gurux lo decodifica directamente como entero
                result.modoDesplazamiento = String.valueOf((char) ((Number) val).intValue());
            }
            //AppLogger.i("ScreenParams", "Meter Led info OK → " + result.modoDesplazamiento);

        } catch (Exception e) {
            AppLogger.e("ScreenParams", "Error reading Meter Led info: " + e.getMessage());
        }

        return result;
    }

    public static boolean programarParametrosPantalla(GXDLMSReader reader,
                                                      String modo,
                                                      int tiempo) {
        try {
            // --- Escribir modo ---
            AppLogger.i("ScreenParams", "Writing scrolling Mode : " + modo);
            GXDLMSData modoObj = new GXDLMSData("0.0.96.1.7.255");
            modoObj.setDataType(2, DataType.OCTET_STRING);
            modoObj.setValue(new byte[]{(byte) modo.charAt(0)});
            reader.writeObject(modoObj, 2);
            AppLogger.i("ScreenParams", "Scrolling Mode data updated!");

            // --- Escribir tiempo ---
            AppLogger.i("ScreenParams", "Writing scrolling time : " + tiempo);
            GXDLMSRegister tiempoObj = new GXDLMSRegister("0.0.94.34.110.255");
            tiempoObj.setDataType(2, DataType.UINT32);
            tiempoObj.setValue((long) tiempo);
            reader.writeObject(tiempoObj, 2);
            AppLogger.i("ScreenParams", "Scrolling time data updated!");

            return true;

        } catch (Exception e) {
            AppLogger.e("ScreenParams", "Error programando parámetros: " + e.getMessage());
            return false;
        }
    }
}