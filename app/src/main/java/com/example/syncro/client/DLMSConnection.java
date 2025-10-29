package com.example.syncro.client;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import gurux.common.enums.TraceLevel;
import com.example.syncro.client.GXDLMSSecureClient2;

import gurux.dlms.GXByteBuffer;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.Conformance;
import gurux.dlms.enums.InterfaceType;
import gurux.io.BaudRate;
import gurux.io.Parity;
import gurux.io.StopBits;
import gurux.serial.GXSerial;

public class DLMSConnection {

    public final GXDLMSSecureClient2 client;
    public final BluetoothCommunicator serial;
    public GXDLMSReader reader;


    /*
    private final InputStream in;
    private final OutputStream out;


    public DLMSConnection(GXDLMSSecureClient2 client, BluetoothSocket socket) throws Exception {
        this.client = client;
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

     */

    public DLMSConnection(GXDLMSReader reader, BluetoothCommunicator serial, GXDLMSSecureClient2 client) {
        this.reader = reader;
        this.serial = serial;
        this.client = client;
    }


    public static DLMSConnection initializeConnection2(Context context) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) throw new Exception("Bluetooth no disponible en este dispositivo");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Permiso BLUETOOTH_CONNECT no concedido");
        }

        String targetName = "TesPro V4_7706";
        BluetoothDevice targetDevice = null;
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("DLMS", "Emparejado: " + device.getName() + " (" + device.getAddress() + ")");
                if (device.getName() != null && device.getName().equals(targetName)) {
                    targetDevice = device;
                    break;
                }
            }
        }

        if (targetDevice == null) {
            throw new Exception("No se encontró la sonda TesPro emparejada");
        }

        UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        // 1. Crear socket
        BluetoothSocket socket = targetDevice.createRfcommSocketToServiceRecord(SPP);

        try {
            // 2. CONECTAR EL SOCKET ANTES DE USAR STREAMS
            Log.d("DLMS", "Conectando a TesPro V4_7706...");
            socket.connect();
            Log.d("DLMS", "Conexión Bluetooth SPP establecida.");

            // 3. AHORA crear el comunicador
            BluetoothCommunicator media = new BluetoothCommunicator(socket);

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
            GXDLMSReader reader = new GXDLMSReader(client, media, TraceLevel.VERBOSE, null);
            reader.setContext(context); // Para trace.txt

            // Handshake inicial
            reader.initializeConnection();
            Log.i("DLMS", "Handshake IEC completado. Conexión DLMS activa.");

            return new DLMSConnection(reader, media, client);

        } catch (IOException e) {
            Log.e("DLMS", "Error conectando Bluetooth", e);
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            throw new Exception("Fallo al conectar Bluetooth: " + e.getMessage());
        }
    }

    /*
    public void close() {
        try {
            if (client != null) {
                try {
                    byte[] disconnect = client.disconnectRequest();
                    if (disconnect != null && out != null) {
                        out.write(disconnect);
                        out.flush();
                        Log.i("DLMS", "DLMS disconnectRequest enviado correctamente.");
                    }
                } catch (Exception e) {
                    Log.w("DLMS", "Error enviando disconnectRequest", e);
                }
            }

            socket.close();
            Log.i("DLMS", "Socket Bluetooth cerrado.");
        } catch (Exception e) {
            Log.e("DLMS", "Error cerrando socket", e);
        }
    }
    */

    public static void closeConnection(GXDLMSReader reader, BluetoothCommunicator serial) {
        try {
            if (reader != null) {
                reader.close();
                System.out.println("Sesión DLMS cerrada correctamente.");
            }
        } catch (Exception e) {
            System.err.println("Error cerrando la sesión DLMS: " + e.getMessage());
        }

        try {
            if (serial != null && serial.isOpen()) {
                serial.close();
                System.out.println("Puerto serie cerrado correctamente.");
            }
        } catch (Exception e) {
            System.err.println("Error cerrando el puerto serie: " + e.getMessage());
        }
    }




    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }

}