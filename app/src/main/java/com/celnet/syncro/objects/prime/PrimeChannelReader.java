package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class PrimeChannelReader {

    // OBIS confirmados byte a byte contra el log de referencia:
    //   Channel selection : TX class=1(Data) obis=0.0.94.34.23.255 -> RX 04 08 80 (bit-string)
    //   Active Channel     : TX class=1(Data) obis=0.0.94.34.24.255 -> RX 04 08 80 (bit-string)
    //   macMinBandSearchTime : TX class=3(Register) obis=0.0.94.34.32.255 -> RX 12 00 0A (=10)
    //   macMaxBandSearchTime : TX class=3(Register) obis=0.0.94.34.33.255 -> RX 12 02 58 (=600)
    private static final String OBIS_CHANNEL_SELECTION         = "0.0.94.34.23.255";
    private static final String OBIS_ACTIVE_CHANNEL             = "0.0.94.34.24.255";
    private static final String OBIS_MAC_MIN_BAND_SEARCH_TIME   = "0.0.94.34.32.255";
    private static final String OBIS_MAC_MAX_BAND_SEARCH_TIME   = "0.0.94.34.33.255";

    /** Resultado en crudo de la lectura, para que la UI decida cómo pintarlo (texto, ticks, etc.). */
    public static class Resultado {
        public final boolean[] channelSelection; // índice 0 = Channel 1 .. índice 7 = Channel 8
        public final boolean[] activeChannel;
        public final int macMin;
        public final int macMax;

        public Resultado(boolean[] channelSelection, boolean[] activeChannel, int macMin, int macMax) {
            this.channelSelection = channelSelection;
            this.activeChannel = activeChannel;
            this.macMin = macMin;
            this.macMax = macMax;
        }
    }

    /**
     * Lee el canal PRIME configurado, el canal activo y los tiempos de búsqueda
     * de banda. Devuelve los datos en crudo (la UI decide el formato de presentación).
     */
    public static Resultado leerCanalPrime(GXDLMSReader reader) throws Exception {
        AppLogger.i("PrimeChannel", "Leyendo canal PRIME...");

        boolean[] channelSelection = leerBits(reader, OBIS_CHANNEL_SELECTION, "Channel selection");
        AppLogger.i("PrimeChannel", "Reading Prime 1.4 Channel selection : OK");

        boolean[] activeChannel = leerBits(reader, OBIS_ACTIVE_CHANNEL, "Active Channel");
        AppLogger.i("PrimeChannel", "Reading Prime 1.4 Active Channel : OK");

        int macMin = leerEntero(reader, OBIS_MAC_MIN_BAND_SEARCH_TIME);
        AppLogger.i("PrimeChannel", "Reading Prime 1.4 macMinBandSearchTime : OK");

        int macMax = leerEntero(reader, OBIS_MAC_MAX_BAND_SEARCH_TIME);
        AppLogger.i("PrimeChannel", "Reading Prime 1.4 macMaxBandSearchTime : OK");

        AppLogger.i("PrimeChannel", "Lectura de canal PRIME completada.");
        return new Resultado(channelSelection, activeChannel, macMin, macMax);
    }

    /**
     * Lee un objeto Data cuyo valor es un bit-string de 8 bits (CH1..CH8, MSB
     * primero — confirmado por trama: 04 08 80 -> CH1=1, resto 0).
     *
     * ⚠️ El tipo Java exacto que devuelve Gurux para un bit-string puede variar
     * según la versión de la librería (gurux.dlms.GXBitString, String de '0'/'1',
     * byte[] o boolean[]). Cubro los casos más habituales; si tu versión de Gurux
     * devuelve algo distinto, este método registrará un aviso en el log con el
     * tipo real recibido para poder añadir ese caso.
     */
    private static boolean[] leerBits(GXDLMSReader reader, String obis, String nombreLog) throws Exception {
        GXDLMSData data = new GXDLMSData(obis);
        reader.read(data, 2);
        Object value = data.getValue();

        boolean[] resultado = new boolean[8];

        if (value instanceof gurux.dlms.GXBitString) {
            String texto = value.toString(); // p.ej. "10000000"
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

    private static int leerEntero(GXDLMSReader reader, String obis) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 2);
        Object value = reg.getValue();
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}