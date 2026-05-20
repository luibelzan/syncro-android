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

import com.example.syncro.ConfigContadorActivity;
import com.example.syncro.client.GXDLMSSecureClient2;

import gurux.dlms.GXByteBuffer;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.Conformance;
import gurux.dlms.enums.InterfaceType;
import gurux.io.BaudRate;
import gurux.io.Parity;
import gurux.io.StopBits;
import gurux.net.GXNet;
import gurux.net.enums.NetworkType;
import gurux.serial.GXSerial;

public class DLMSConnection {

    private final String device;
    private final String ip;
    private final int port;
    private static GXDLMSSecureClient2 client;
    private static BluetoothCommunicator serial;
    private static GXDLMSReader reader;
    private static GXNet net;


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

    public DLMSConnection(String device) {
        this.device = device;
        this.ip = null;
        this.port = -1;
    }

    public DLMSConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.device = null;
    }

    public GXDLMSReader bluetoothConnnect(Context context) throws Exception {
        //configurarSerial();
        configurarClienteDlms(context);

        reader = new GXDLMSReader(client, serial, TraceLevel.VERBOSE, null);
        //reader.wait(30000);
        initializeConnection2(context, device);

        System.out.println("Handshake IEC completado. Cambiando a DLMS.");
        return reader;
    }

    public GXDLMSReader tcpConnect(Context context) throws Exception {
        net = new GXNet(NetworkType.TCP, ip, port);
        net.setTrace(TraceLevel.VERBOSE);
        net.open();

        System.out.println("Conectado por TCP/IP a " + ip + ":" + port);

        configurarClienteDlms(context);

        reader = new GXDLMSReader(client, net, TraceLevel.VERBOSE, null);
        reader.initializeConnection();
        System.out.println("Handshake IEC completado. Cambiando a DLMS.");
        return reader;
    }

    private static void configurarClienteDlms(Context context) {
        ConfigContadorActivity.DLMSConfigValues cfg = ConfigContadorActivity.loadConfig(context);

        client = new GXDLMSSecureClient2(true);
        // 1. IMPORTANTE: En puerto serie suele ser HDLC, en TCP suele ser WRAPPER
        client.setInterfaceType(InterfaceType.HDLC);
        // 2. CONFIGURACIÓN DE DIRECCIÓN (Aquí está el truco)
        // Para obtener la trama 00 02 00 21:
        // El primer '1' es el Management Logical Device.
        // El '16' es el Physical Device ID (común en Sagemcom/Landis).
        client.setServerAddress(GXDLMSClient.getServerAddress(
                cfg.logicalDevice, cfg.physicalDevice, cfg.addressSize));

        // Si lo anterior falla, intenta forzar el ServerAddressSize a 1
        // como tenías al principio, pero usa el ClientAddress 0x1 (decimal 1)
        // client.setServerAddress(0x03); // A veces el ID físico es simplemente 0x03
        client.setClientAddress(cfg.clientAddress);
        client.setUseLogicalNameReferencing(true);
        client.setAuthentication(cfg.authentication);
        client.setPassword(cfg.password.getBytes());
        // Limitar el tamaño de PDU para evitar que el Gateway TCP se sature
        client.setMaxReceivePDUSize(236);
    }


    public static DLMSConnection initializeConnection2(Context context, String dispositivo) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) throw new Exception("Bluetooth no disponible en este dispositivo");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Permiso BLUETOOTH_CONNECT no concedido");
        }

        String targetName = "TesPro";
        BluetoothDevice targetDevice = null;
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("DLMS", "Emparejado: " + device.getName() + " (" + device.getAddress() + ")");
                if (device.getName() != null && device.getName().contains(targetName)) {
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
            Log.d("DLMS", "Conectando a TesPro...");
            socket.connect();
            Log.d("DLMS", "Conexión Bluetooth SPP establecida.");

            // 3. AHORA crear el comunicador
            serial = new BluetoothCommunicator(socket);

            // Configurar cliente DLMS
            configurarClienteDlms(context);

            // Crear lector DLMS
            reader = new GXDLMSReader(client, serial, TraceLevel.VERBOSE, null);
            reader.setContext(context); // Para trace.txt

            // Handshake inicial
            reader.initializeConnectionBluetooth();
            Log.i("DLMS", "Handshake IEC completado. Conexión DLMS activa.");

            return new DLMSConnection(dispositivo);

        } catch (IOException e) {
            Log.e("DLMS", "Error conectando Bluetooth", e);
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            throw new Exception("Fallo al conectar Bluetooth: " + e.getMessage());
        }
    }

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

    public GXDLMSSecureClient2 getClient() {
        return client;
    }

    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }

    public void close() {
        try {
            if (reader != null) reader.close();
            if (serial != null && serial.isOpen()) serial.close();
            if (net != null && net.isOpen()) net.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}