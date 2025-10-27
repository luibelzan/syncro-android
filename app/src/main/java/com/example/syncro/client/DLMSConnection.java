package com.example.syncro.client;

import gurux.common.enums.TraceLevel;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.InterfaceType;
import gurux.io.BaudRate;
import gurux.io.Parity;
import gurux.io.StopBits;
import gurux.serial.GXSerial;

public class DLMSConnection {

    public GXDLMSReader reader;
    public GXSerial serial;
    public GXDLMSSecureClient2 client;

    public DLMSConnection(GXDLMSReader reader, GXSerial serial, GXDLMSSecureClient2 client) {
        this.reader = reader;
        this.serial = serial;
        this.client = client;
    }

    public static DLMSConnection initializeConnection(String portName) throws Exception {
        GXSerial serial = new GXSerial();

        serial.setPortName(portName);
        serial.setBaudRate(BaudRate.BAUD_RATE_9600);
        serial.setDataBits(8);
        serial.setParity(Parity.NONE);
        serial.setStopBits(StopBits.ONE);
        serial.setTrace(TraceLevel.VERBOSE);
        serial.open();

        System.out.println("Puerto abierto: " + portName + " (modo IEC inicial)");

        // Configurar cliente DLMS
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);
        client.setInterfaceType(InterfaceType.HDLC);
        client.setServerAddressSize(2);
        client.setServerAddress(144);
        client.setClientAddress(1);
        client.setUseLogicalNameReferencing(true);
        client.setAuthentication(Authentication.LOW);
        client.setPassword("00000002");

        // Crear lector DLMS
        GXDLMSReader reader = new GXDLMSReader(client, serial, TraceLevel.VERBOSE, null);

        // Handshake inicial
        reader.initializeConnection();
        System.out.println("Handshake IEC completado. Conexión DLMS activa.\n");

        return new DLMSConnection(reader, serial, client);
    }

}
