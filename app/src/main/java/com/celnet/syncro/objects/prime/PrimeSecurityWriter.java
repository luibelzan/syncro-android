package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.prime.ConstellationCoding;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.GXBitString;
import gurux.dlms.GXUInt16;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class PrimeSecurityWriter {

    private static final String OBIS_CONSTELLATION_CODING = "0.0.94.34.26.255";

    // macMin/macMax movidos aquí desde PrimeChannelWriter.
    private static final String OBIS_MAC_MIN_BAND_SEARCH_TIME = "0.0.94.34.32.255";
    private static final String OBIS_MAC_MAX_BAND_SEARCH_TIME = "0.0.94.34.33.255";

    /**
     * Programa la seguridad PRIME 1.4 del contador: constellation/coding y
     * los tiempos de búsqueda de banda (macMin/macMax). La versión de Dual
     * Stack se programa ahora desde PrimeChannelWriter, junto con la
     * selección de canal.
     */
    public static void programarSeguridadPrime(GXDLMSReader reader,
                                               ConstellationCoding constellation,
                                               boolean escribirConstellationCoding,
                                               boolean escribirMacMin, int macMin,
                                               boolean escribirMacMax, int macMax) throws Exception {

        if (escribirConstellationCoding) {
            GXDLMSData constellationObj = new GXDLMSData(OBIS_CONSTELLATION_CODING);
            constellationObj.setValue(buildBitString(constellation.toBitArray()));
            constellationObj.setDataType(2, DataType.BITSTRING);
            reader.writeObject(constellationObj, 2);
            AppLogger.i("PrimeSecurity", "Writting Prime 1.4 constellationCoding : OK");
        } else {
            AppLogger.i("PrimeSecurity", "Constellation coding: sección desactivada, no se envía SET.");
        }

        if (escribirMacMin) {
            escribirEntero(reader, OBIS_MAC_MIN_BAND_SEARCH_TIME, macMin);
            AppLogger.i("PrimeSecurity", "Writting macMinBandSearchTime : OK");
        }

        if (escribirMacMax) {
            escribirEntero(reader, OBIS_MAC_MAX_BAND_SEARCH_TIME, macMax);
            AppLogger.i("PrimeSecurity", "Writting macMaxBandSearchTime : OK");
        }
    }

    private static void escribirEntero(GXDLMSReader reader, String obis, int valor) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reg.setValue(new GXUInt16(valor));
        reader.writeObject(reg, 2);
    }

    private static GXBitString buildBitString(boolean[] bits) {
        StringBuilder sb = new StringBuilder(bits.length);
        for (boolean bit : bits) {
            sb.append(bit ? '1' : '0');
        }
        return new GXBitString(sb.toString());
    }
}