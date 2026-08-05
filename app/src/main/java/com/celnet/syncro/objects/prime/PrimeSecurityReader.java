package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.PrimeSecurityData;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;

public class PrimeSecurityReader {

    // ⚠️ TODO: sustituye estos 4 OBIS por los reales de tu medidor.
    // Los que aparecen aquí son placeholders — ajusta también la clase DLMS
    // (GXDLMSData / GXDLMSRegister / etc.) si el objeto real no es "Data".
    private static final String OBIS_CONSTELLATION_CODING = "0.0.94.34.20.255";
    private static final String OBIS_SAR_SIZE             = "0.0.94.34.21.255";
    private static final String OBIS_ARQ_ENABLE            = "0.0.94.34.22.255";
    private static final String OBIS_DUAL_STACK_VERSION    = "0.0.94.34.23.255";

    public static PrimeSecurityData leerPrimeSecurity(GXDLMSReader reader) {
        AppLogger.w("Syncro", "Leyendo configuración de seguridad PRIME");
        PrimeSecurityData datos = new PrimeSecurityData();

        try {
            GXDLMSData constellationCoding = new GXDLMSData(OBIS_CONSTELLATION_CODING);
            reader.read(constellationCoding, 2);
            datos.constellationCodingRaw = toInt(constellationCoding.getValue());
            AppLogger.i("PrimeSecurity", "Reading Prime 1.4 constellationCoding : OK");

            GXDLMSData sarSize = new GXDLMSData(OBIS_SAR_SIZE);
            reader.read(sarSize, 2);
            datos.sarSizeRaw = toInt(sarSize.getValue());
            AppLogger.i("PrimeSecurity", "Reading Prime 1.4 SARSize : OK");

            GXDLMSData arqEnable = new GXDLMSData(OBIS_ARQ_ENABLE);
            reader.read(arqEnable, 2);
            datos.arqEnabled = toInt(arqEnable.getValue()) != 0;
            AppLogger.i("PrimeSecurity", "Reading Prime 1.4 ARQ Enable/Disable : OK");

            GXDLMSData dualStackVersion = new GXDLMSData(OBIS_DUAL_STACK_VERSION);
            reader.read(dualStackVersion, 2);
            datos.dualStackVersionRaw = toInt(dualStackVersion.getValue());
            AppLogger.i("PrimeSecurity", "Reading Prime 1.4 Dualstackversion : OK");

            AppLogger.i("PrimeSecurity", "Operación OK");

        } catch (Exception e) {
            AppLogger.e("PrimeSecurity", "Error crítico: " + e.getMessage());
        }

        return datos;
    }

    private static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}