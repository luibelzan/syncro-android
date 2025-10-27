package com.example.syncro.client;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.Set;

import gurux.common.enums.TraceLevel;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.InterfaceType;
import gurux.io.BaudRate;
import gurux.io.Parity;
import gurux.io.StopBits;
import gurux.serial.GXSerial;

public class DLMSConnection  {

    public GXDLMSReader reader;
    public GXSerial serial;
    public GXDLMSSecureClient2 client;

    public DLMSConnection(GXDLMSReader reader, GXSerial serial, GXDLMSSecureClient2 client) {
        this.reader = reader;
        this.serial = serial;
        this.client = client;
    }

    public static DLMSConnection initializeConnection(Context context) throws Exception {
        String address = "";
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw new SecurityException("Permiso BLUETOOTH_CONNECT no concedido");
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                address = device.getAddress(); // esto es lo que buscas
                Log.d("Bluetooth", "Dispositivo: " + name + ", MAC: " + address);
            }
        }

        GXSerial serial = new GXSerial();

        serial.setPortName(address);
        serial.setBaudRate(BaudRate.BAUD_RATE_9600);
        serial.setDataBits(8);
        serial.setParity(Parity.NONE);
        serial.setStopBits(StopBits.ONE);
        serial.setTrace(TraceLevel.VERBOSE);
        serial.open();

        System.out.println("Puerto abierto: " + address + " (modo IEC inicial)");

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
