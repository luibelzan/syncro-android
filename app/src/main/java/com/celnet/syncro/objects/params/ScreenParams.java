package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class ScreenParams {

    private static final String OBIS_OUTPUT_LED = "0.0.96.3.2.255";

    public static class ScreenParamsResult {
        public int tiempoDesplazamiento = -1;   // segundos
        public String modoDesplazamiento = "N/A";
        public int outputLed = -1;              // 0-5, o -1 si no se pudo leer
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

        // --- Output Led ---
        try {
            AppLogger.i("ScreenParams", "Reading Output Led");
            GXDLMSData outputLedObj = new GXDLMSData(OBIS_OUTPUT_LED);
            reader.read(outputLedObj, 2);
            Object val = outputLedObj.getValue();
            if (val instanceof Number) {
                result.outputLed = ((Number) val).intValue();
            } else if (val instanceof byte[]) {
                byte[] b = (byte[]) val;
                if (b.length >= 1) {
                    result.outputLed = b[0] & 0xFF;
                }
            }
        } catch (Exception e) {
            AppLogger.e("ScreenParams", "Error reading Output Led: " + e.getMessage());
        }

        return result;
    }

    public static String descripcionOutputLed(int valor) {
        switch (valor) {
            case 0: return "Active Energy: OFF with no load and ON (blinking) with load";
            case 1: return "Reactive Energy OFF with no load and ON (blinking) with load";
            case 2: return "Active Energy: ON with no load and ON (blinking) with load";
            case 3: return "Reactive Energy: ON with no load and ON (blinking) with load";
            case 4: return "ON all LEDs on (meters with 2 LEDs)";
            case 5: return "OFF all LEDs off (meters with 2 LEDs)";
            default: return "N/A";
        }
    }

    public static class ProgramarResult {
        public boolean modoOk = true;
        public boolean tiempoOk = true;
        public boolean outputLedOk = true;
        public String errorOutputLed = null;

        public boolean todoOk() {
            return modoOk && tiempoOk && outputLedOk;
        }
    }

    public static ProgramarResult programarParametrosPantalla(GXDLMSReader reader,
                                                              String modo,
                                                              Integer tiempo,
                                                              Integer outputLed) {
        ProgramarResult result = new ProgramarResult();

        if (modo != null) {
            try {
                AppLogger.i("ScreenParams", "Writing scrolling Mode : " + modo);
                GXDLMSData modoObj = new GXDLMSData("0.0.96.1.7.255");
                modoObj.setDataType(2, DataType.OCTET_STRING);
                modoObj.setValue(new byte[]{(byte) modo.charAt(0)});
                reader.writeObject(modoObj, 2);
                AppLogger.i("ScreenParams", "Scrolling Mode data updated!");
            } catch (Exception e) {
                AppLogger.e("ScreenParams", "Error escribiendo modo: " + e.getMessage());
                result.modoOk = false;
            }
        }

        if (tiempo != null) {
            try {
                AppLogger.i("ScreenParams", "Writing scrolling time : " + tiempo);
                GXDLMSRegister tiempoObj = new GXDLMSRegister("0.0.94.34.110.255");
                tiempoObj.setDataType(2, DataType.UINT32);
                tiempoObj.setValue((long) (int) tiempo);
                reader.writeObject(tiempoObj, 2);
                AppLogger.i("ScreenParams", "Scrolling time data updated!");
            } catch (Exception e) {
                AppLogger.e("ScreenParams", "Error escribiendo tiempo: " + e.getMessage());
                result.tiempoOk = false;
            }
        }

        if (outputLed != null) {
            try {
                AppLogger.i("ScreenParams", "Writing Output Led");
                GXDLMSData outputLedObj = new GXDLMSData(OBIS_OUTPUT_LED);
                outputLedObj.setDataType(2, DataType.UINT8);
                outputLedObj.setValue((short) (int) outputLed);
                reader.writeObject(outputLedObj, 2);
                AppLogger.i("ScreenParams", "Output Led Set to : " + outputLed);
            } catch (Exception e) {
                AppLogger.e("ScreenParams", "Error Writing Output Led: " + e.getMessage());
                result.outputLedOk = false;
                result.errorOutputLed = "El contador ha rechazado este valor de Output Led "
                        + "(puede que no aplique a este modelo, p.ej. opciones para contadores de 2 LEDs).";
            }
        }

        return result;
    }

    public static boolean programarOutputLed(GXDLMSReader reader, int opcion) {
        try {
            AppLogger.i("ScreenParams", "Writing Output Led");
            GXDLMSData outputLedObj = new GXDLMSData(OBIS_OUTPUT_LED);
            outputLedObj.setDataType(2, DataType.UINT8);
            outputLedObj.setValue((short) opcion);
            reader.writeObject(outputLedObj, 2);
            AppLogger.i("ScreenParams", "Output Led Set to : " + opcion);
            return true;
        } catch (Exception e) {
            AppLogger.e("ScreenParams", "Error Writing Output Led: " + e.getMessage());
            return false;
        }
    }
}