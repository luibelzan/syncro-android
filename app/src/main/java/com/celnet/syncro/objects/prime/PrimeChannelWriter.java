package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.GXBitString;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSScriptTable;

public class PrimeChannelWriter {

    private static final String OBIS_CHANNEL_SELECTION = "0.0.94.34.23.255";

    // Dual stack version (script table de ejecución) movida aquí desde PrimeSecurityWriter.
    private static final String OBIS_SCRIPT_TABLE_DUALSTACK = "0.0.94.34.29.255";

    /** Valor especial: no tocar la versión Dual Stack actual (no se ejecuta ningún script). */
    public static final int DUALSTACK_MANTENER_ACTUAL = 0;

    public static void programarCanalPrime(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                           boolean escribirCanal, boolean[] canalMascara,
                                           boolean escribirDualStack, int dualStackOpcionElegida) throws Exception {
        AppLogger.i("PrimeChannel", "Programando Canal Prime");

        if (escribirCanal) {
            escribirBits(reader, client, OBIS_CHANNEL_SELECTION, canalMascara);
            AppLogger.i("PrimeChannel", "Writting Prime 1.4 Channel selection : OK");
        }

        if (escribirDualStack) {
            if (dualStackOpcionElegida == DUALSTACK_MANTENER_ACTUAL) {
                AppLogger.i("PrimeChannel", "Dual stack Prime version: se mantiene el valor actual, no se ejecuta script.");
            } else if (dualStackOpcionElegida < 1 || dualStackOpcionElegida > 3) {
                throw new IllegalArgumentException("Opción de Dual Stack no reconocida: " + dualStackOpcionElegida);
            } else {
                AppLogger.i("PrimeChannel", "Executing Dual stack Prime version Script");
                GXDLMSScriptTable scriptTable = new GXDLMSScriptTable(OBIS_SCRIPT_TABLE_DUALSTACK);
                reader.method(scriptTable, 1, dualStackOpcionElegida, DataType.UINT16);
                AppLogger.i("PrimeChannel", "Dual stack Prime version – Executed : OK");
            }
        }
    }

    private static void escribirBits(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                     String obis, boolean[] bits) throws Exception {
        StringBuilder cadena = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            cadena.append((bits != null && i < bits.length && bits[i]) ? '1' : '0');
        }

        GXDLMSData data = new GXDLMSData(obis);
        data.setValue(new GXBitString(cadena.toString()));

        enviarEscritura(reader, client, data, 2);
    }

    private static void enviarEscritura(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                        GXDLMSObject obj, int attributeIndex) throws Exception {
        byte[][] data = client.write(obj, attributeIndex);
        GXReplyData reply = new GXReplyData();
        for (byte[] frame : data) {
            reply.clear();
            reader.readDLMSPacket(frame, reply);
            if (reply.getError() != 0) {
                throw new RuntimeException("Error escribiendo " + obj.getLogicalName() + ": " + reply.getError());
            }
        }
    }
}