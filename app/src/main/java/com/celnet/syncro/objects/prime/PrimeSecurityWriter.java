package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.prime.ConstellationCoding;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.GXBitString;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSScriptTable;

public class PrimeSecurityWriter {

    private static final String OBIS_CONSTELLATION_CODING = "0.0.94.34.26.255";
    private static final String OBIS_SCRIPT_TABLE_DUALSTACK = "0.0.94.34.29.255";

    /** Valor especial: no tocar la versión Dual Stack actual (no se ejecuta ningún script) */
    public static final int DUALSTACK_MANTENER_ACTUAL = 0;

    /**
     * Programa la seguridad PRIME 1.4 del contador.
     *
     * @param reader                 wrapper de lectura/escritura DLMS ya conectado
     * @param constellation          estado deseado de los 16 bits de constellation/coding
     * @param dualStackOpcionElegida número mostrado en el desplegable (0,1,2,3).
     *                                Confirmado por log: coincide 1:1 con el scriptId real
     *                                (0 = mantener actual, no se ejecuta script;
     *                                 1, 2, 3 = se ejecuta el script con ese mismo valor).
     */
    public static void programarSeguridadPrime(GXDLMSReader reader,
                                               ConstellationCoding constellation,
                                               boolean escribirConstellationCoding,
                                               int dualStackOpcionElegida) throws Exception {

        // 1) SET del bit-string de Constellation Coding — solo si el maestro está activado
        if (escribirConstellationCoding) {
            GXDLMSData constellationObj = new GXDLMSData(OBIS_CONSTELLATION_CODING);
            constellationObj.setValue(buildBitString(constellation.toBitArray()));
            constellationObj.setDataType(2, DataType.BITSTRING);
            reader.writeObject(constellationObj, 2);
            AppLogger.i("PrimeSecurity", "Writting Prime 1.4 constellationCoding : OK");
        } else {
            AppLogger.i("PrimeSecurity", "Constellation coding: sección desactivada, no se envía SET.");
        }

        // 2) ACTION (execute) sobre la tabla de scripts, solo si el usuario no eligió "mantener actual"
        if (dualStackOpcionElegida == DUALSTACK_MANTENER_ACTUAL) {
            AppLogger.i("PrimeSecurity", "Dual stack Prime version: se mantiene el valor actual, no se ejecuta script.");
            return;
        }

        if (dualStackOpcionElegida < 1 || dualStackOpcionElegida > 3) {
            throw new IllegalArgumentException("Opción de Dual Stack no reconocida: " + dualStackOpcionElegida);
        }

        AppLogger.i("PrimeSecurity", "Executing Dual stack Prime version Script");
        GXDLMSScriptTable scriptTable = new GXDLMSScriptTable(OBIS_SCRIPT_TABLE_DUALSTACK);
        reader.method(scriptTable, 1, dualStackOpcionElegida, DataType.UINT16);
        AppLogger.i("PrimeSecurity", "Prime 1.4 – Executed Dual stack Prime version : OK");
    }

    private static GXBitString buildBitString(boolean[] bits) {
        StringBuilder sb = new StringBuilder(bits.length);
        for (boolean bit : bits) {
            sb.append(bit ? '1' : '0');
        }
        return new GXBitString(sb.toString());
    }
}