package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;

public class PrimeChannelReader {

    private static final String OBIS_CHANNEL_SELECTION = "0.0.94.34.23.255";
    private static final String OBIS_ACTIVE_CHANNEL     = "0.0.94.34.24.255";

    // Dual stack version movida aquí desde PrimeSecurityReader.
    private static final String OBIS_DUAL_STACK_VERSION = "0.0.94.34.30.255";

    public static class Resultado {
        public final boolean[] channelSelection; // índice 0 = Channel 1 .. índice 7 = Channel 8
        public final boolean[] activeChannel;
        public final int dualStackVersionCode;
        public final String dualStackVersion;

        public Resultado(boolean[] channelSelection, boolean[] activeChannel,
                         int dualStackVersionCode, String dualStackVersion) {
            this.channelSelection = channelSelection;
            this.activeChannel = activeChannel;
            this.dualStackVersionCode = dualStackVersionCode;
            this.dualStackVersion = dualStackVersion;
        }
    }

    public static Resultado leerCanalPrime(GXDLMSReader reader) throws Exception {
        AppLogger.i("PrimeChannel", "Leyendo canal PRIME...");

        boolean[] channelSelection = leerBits(reader, OBIS_CHANNEL_SELECTION, "Channel selection");
        AppLogger.i("PrimeChannel", "Reading Prime 1.4 Channel selection : OK");

        boolean[] activeChannel = leerBits(reader, OBIS_ACTIVE_CHANNEL, "Active Channel");
        AppLogger.i("PrimeChannel", "Reading Prime 1.4 Active Channel : OK");

        GXDLMSData dualStack = new GXDLMSData(OBIS_DUAL_STACK_VERSION);
        reader.read(dualStack, 2);
        int dualStackCode = toInt(dualStack.getValue());
        String dualStackTexto = parseDualStackVersion(dualStackCode);
        AppLogger.i("PrimeChannel", "Reading Dual stack Prime version : OK");

        AppLogger.i("PrimeChannel", "Lectura de canal PRIME completada.");
        return new Resultado(channelSelection, activeChannel, dualStackCode, dualStackTexto);
    }

    private static boolean[] leerBits(GXDLMSReader reader, String obis, String nombreLog) throws Exception {
        GXDLMSData data = new GXDLMSData(obis);
        reader.read(data, 2);
        Object value = data.getValue();

        boolean[] resultado = new boolean[8];

        if (value instanceof gurux.dlms.GXBitString) {
            String texto = value.toString();
            for (int i = 0; i < Math.min(8, texto.length()); i++) {
                resultado[i] = texto.charAt(i) == '1';
            }
        } else if (value instanceof String) {
            String texto = (String) value;
            for (int i = 0; i < Math.min(8, texto.length()); i++) {
                resultado[i] = texto.charAt(i) == '1';
            }
        } else if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (bytes.length > 0) {
                int b = bytes[0] & 0xFF;
                for (int i = 0; i < 8; i++) {
                    resultado[i] = (b & (0x80 >> i)) != 0;
                }
            }
        } else if (value instanceof boolean[]) {
            boolean[] bits = (boolean[]) value;
            System.arraycopy(bits, 0, resultado, 0, Math.min(8, bits.length));
        } else {
            AppLogger.w("PrimeChannel", nombreLog + ": tipo de valor inesperado ("
                    + (value != null ? value.getClass().getName() : "null") + "): " + value);
        }

        return resultado;
    }

    private static String parseDualStackVersion(int v) {
        switch (v) {
            case 1: return "(1) Communications in Prime 1.3.6";
            case 2: return "(2) Communications in Prime 1.4";
            case 3: return "(3) Dynamic communications 1.3.6 or 1.4";
            default: return "(" + v + ") Valor no mapeado";
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}