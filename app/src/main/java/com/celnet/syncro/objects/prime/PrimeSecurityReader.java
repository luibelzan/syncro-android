package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.prime.ConstellationCoding;
import com.celnet.syncro.models.prime.PrimeSecurityInfo;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;

public class PrimeSecurityReader {

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

        // 4. Prime 1.4 Dualstack version (enum)
        GXDLMSData dualStack = new GXDLMSData("0.0.94.34.30.255");
        reader.read(dualStack, 2);
        info.setDualStackVersion(parseDualStackVersion(toInt(dualStack.getValue())));
        AppLogger.i("PrimeSecurity", "Reading Prime 1.4 Dualstackversion : OK");

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

    /**
     * Extrae bits de un valor bit-string devuelto por Gurux.
     * Soporta tanto gurux.dlms.GXBitString como byte[] "en crudo",
     * por si la versión de la librería expone el valor de forma distinta.
     */
    private static boolean[] extractBits(Object value, int bitCount) {
        byte[] raw;
        if (value instanceof gurux.dlms.GXBitString) {
            raw = ((gurux.dlms.GXBitString) value).getValue();
        } else if (value instanceof byte[]) {
            raw = (byte[]) value;
        } else if (value instanceof String) {
            // Algunas versiones devuelven directamente "1110111000001100"
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
            int bitInByte = 7 - (i % 8); // MSB primero, como en el log (EE 0C)
            if (byteIndex < raw.length) {
                bits[i] = ((raw[byteIndex] >> bitInByte) & 1) == 1;
            }
        }
        return bits;
    }

    private static String parseSarSize(int v) {
        switch (v) {
            case 0: return "(0) Not mandated by BN";
            // TODO: completar con el resto de valores si el fabricante los documenta
            default: return "(" + v + ") Valor no mapeado";
        }
    }

    private static String parseDualStackVersion(int v) {
        switch (v) {
            case 3: return "(3) Dynamic communications 1.3.6 or 1.4";
            // TODO: completar con el resto de valores si el fabricante los documenta
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
}