package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.prime.ConstellationCoding;
import com.celnet.syncro.models.prime.PrimeSecurityInfo;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class PrimeSecurityReader {

    // macMin/macMax movidos aquí desde PrimeChannelReader.
    private static final String OBIS_MAC_MIN_BAND_SEARCH_TIME = "0.0.94.34.32.255";
    private static final String OBIS_MAC_MAX_BAND_SEARCH_TIME = "0.0.94.34.33.255";

    public static PrimeSecurityInfo leerSeguridadPrime(GXDLMSReader reader) throws Exception {
        AppLogger.w("Syncro", "Leyendo Seguridad PRIME actual");

        PrimeSecurityInfo info = new PrimeSecurityInfo();

        // 1. Prime 1.4 Constellation Coding (bit-string, 16 bits)
        GXDLMSData constellationCoding = new GXDLMSData("0.0.94.34.26.255");
        reader.read(constellationCoding, 2);
        info.setConstellationCoding(parseConstellationCoding(constellationCoding.getValue()));
        AppLogger.i("PrimeSecurity", "Reading Prime 1.4 constellationCoding : OK");

        // 2. Prime 1.4 SARSize (integer)
        GXDLMSData sarSize = new GXDLMSData("0.0.94.34.27.255");
        reader.read(sarSize, 2);
        info.setSarSize(parseSarSize(toInt(sarSize.getValue())));
        AppLogger.i("PrimeSecurity", "Reading Prime 1.4 SARSize : OK");

        // 3. Prime 1.4 ARQ Enable/Disable (boolean)
        GXDLMSData arq = new GXDLMSData("0.0.94.34.28.255");
        reader.read(arq, 2);
        info.setArqEnabled(parseBoolean(arq.getValue()));
        AppLogger.i("PrimeSecurity", "Reading Prime 1.4 ARQ Enable/Disable : OK");

        // 4. macMinBandSearchTime / macMaxBandSearchTime (movidos desde Canal PRIME)
        info.setMacMin(leerEntero(reader, OBIS_MAC_MIN_BAND_SEARCH_TIME));
        AppLogger.i("PrimeSecurity", "Reading Prime 1.4 macMinBandSearchTime : OK");

        info.setMacMax(leerEntero(reader, OBIS_MAC_MAX_BAND_SEARCH_TIME));
        AppLogger.i("PrimeSecurity", "Reading Prime 1.4 macMaxBandSearchTime : OK");

        return info;
    }

    // -------------------- Parsing helpers --------------------

    private static ConstellationCoding parseConstellationCoding(Object value) {
        boolean[] bits = extractBits(value, 16);
        ConstellationCoding cc = new ConstellationCoding();
        cc.dbpsk   = bits[0];
        cc.res1    = bits[1];
        cc.dqpsk   = bits[2];
        cc.d8psk   = bits[3];
        cc.res2    = bits[4];
        cc.dbpskC  = bits[5];
        cc.dqpskC  = bits[6];
        cc.d8pskC  = bits[7];
        cc.res3    = bits[8];
        cc.res4    = bits[9];
        cc.res5    = bits[10];
        cc.res6    = bits[11];
        cc.rDbpsk  = bits[12];
        cc.rDqpsk  = bits[13];
        cc.res7    = bits[14];
        cc.res8    = bits[15];
        return cc;
    }

    private static boolean[] extractBits(Object value, int bitCount) {
        byte[] raw;
        if (value instanceof gurux.dlms.GXBitString) {
            raw = ((gurux.dlms.GXBitString) value).getValue();
        } else if (value instanceof byte[]) {
            raw = (byte[]) value;
        } else if (value instanceof String) {
            String s = (String) value;
            boolean[] fromString = new boolean[bitCount];
            for (int i = 0; i < bitCount && i < s.length(); i++) {
                fromString[i] = s.charAt(i) == '1';
            }
            return fromString;
        } else {
            throw new IllegalArgumentException("Tipo inesperado para bit-string: "
                    + (value == null ? "null" : value.getClass()));
        }

        boolean[] bits = new boolean[bitCount];
        for (int i = 0; i < bitCount; i++) {
            int byteIndex = i / 8;
            int bitInByte = 7 - (i % 8);
            if (byteIndex < raw.length) {
                bits[i] = ((raw[byteIndex] >> bitInByte) & 1) == 1;
            }
        }
        return bits;
    }

    private static String parseSarSize(int v) {
        switch (v) {
            case 0: return "(0) Not mandated by BN";
            default: return "(" + v + ") Valor no mapeado";
        }
    }

    private static boolean parseBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        throw new IllegalArgumentException("Tipo inesperado para boolean: "
                + (value == null ? "null" : value.getClass()));
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        throw new IllegalArgumentException("Tipo inesperado para entero: "
                + (value == null ? "null" : value.getClass()));
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