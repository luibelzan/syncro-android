package com.celnet.syncro.objects.prime;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class PrimeChannelWriter {

    // OBIS confirmados byte a byte contra el log de referencia:
    //   Channel selection : TX class=1(Data) obis=0.0.94.34.23.255 -> RX 04 08 80 (bit-string)
    //   Active Channel     : TX class=1(Data) obis=0.0.94.34.24.255 -> RX 04 08 80 (bit-string)
    //   macMinBandSearchTime : TX class=3(Register) obis=0.0.94.34.32.255 -> RX 12 00 0A (=10)
    //   macMaxBandSearchTime : TX class=3(Register) obis=0.0.94.34.33.255 -> RX 12 02 58 (=600)
    private static final String OBIS_CHANNEL_SELECTION         = "0.0.94.34.23.255";
    private static final String OBIS_ACTIVE_CHANNEL             = "0.0.94.34.24.255";
    private static final String OBIS_MAC_MIN_BAND_SEARCH_TIME   = "0.0.94.34.32.255";
    private static final String OBIS_MAC_MAX_BAND_SEARCH_TIME   = "0.0.94.34.33.255";

    public static void programarCanalPrime(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                           boolean escribirCanal, boolean[] canalMascara,
                                           boolean escribirMacMin, int macMin,
                                           boolean escribirMacMax, int macMax) throws Exception {
        AppLogger.i("PrimeChannel", "Programando Canal Prime");

        if (escribirCanal) {
            escribirBits(reader, client, OBIS_CHANNEL_SELECTION, canalMascara);
            AppLogger.i("PrimeChannel", "Writting Prime 1.4 Channel selection : OK");
        }

        if (escribirMacMin) {
            escribirEntero(reader, client, OBIS_MAC_MIN_BAND_SEARCH_TIME, macMin);
            AppLogger.i("PrimeChannel", "Writting macMinBandSearchTime : OK");
        }

        if (escribirMacMax) {
            escribirEntero(reader, client, OBIS_MAC_MAX_BAND_SEARCH_TIME, macMax);
            AppLogger.i("PrimeChannel", "Writting macMaxBandSearchTime : OK");
        }
    }

    private static void escribirBits(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                     String obis, boolean[] bits) throws Exception {
        StringBuilder cadena = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            cadena.append((bits != null && i < bits.length && bits[i]) ? '1' : '0');
        }

        GXDLMSData data = new GXDLMSData(obis);
        // ⚠️ Construcción del bit-string: uso el constructor GXBitString(String).
        // Si tu versión de Gurux no lo tiene (API distinta), dímelo y lo adapto
        // al constructor real disponible (p.ej. GXBitString(byte[], int)).
        data.setValue(new gurux.dlms.GXBitString(cadena.toString()));

        enviarEscritura(reader, client, data, 2);
    }

    private static void escribirEntero(
            GXDLMSReader reader,
            GXDLMSSecureClient2 client,
            String obis,
            int valor) throws Exception {

        GXDLMSRegister reg = new GXDLMSRegister(obis);

        reg.setValue(new gurux.dlms.GXUInt16(valor));

        enviarEscritura(reader, client, reg, 2);
    }

    private static void enviarEscritura(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                        gurux.dlms.objects.GXDLMSObject obj, int attributeIndex) throws Exception {
        byte[][] data = client.write(obj, attributeIndex);
        gurux.dlms.GXReplyData reply = new gurux.dlms.GXReplyData();
        for (byte[] frame : data) {
            reply.clear();
            reader.readDLMSPacket(frame, reply);
            if (reply.getError() != 0) {
                throw new RuntimeException("Error escribiendo " + obj.getLogicalName() + ": " + reply.getError());
            }
        }
    }
}