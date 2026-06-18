package com.celnet.syncro.client;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import gurux.common.enums.TraceLevel;

import com.celnet.syncro.ConfigContadorActivity;
import com.celnet.syncro.objects.params.SerialNumberReader;

import gurux.dlms.GXDLMSClient;
import gurux.dlms.enums.InterfaceType;
import gurux.net.GXNet;
import gurux.net.enums.NetworkType;

public class DLMSConnection {

    private static final String TAG = "DLMSConnection";

    // Orden de prueba: 1 y 4 son los más comunes en campo
    private static final int[] ADDRESS_SIZE_CANDIDATES = {1, 4, 2, 0};

    private final String device;
    private final String ip;
    private final int port;

    private GXDLMSSecureClient2 client;
    private BluetoothCommunicator serial;
    private GXDLMSReader reader;
    private GXNet net;

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

    // -----------------------------------------------------------------------
    // AUTO-DETECT: prueba addressSizes hasta encontrar uno que devuelva datos
    // -----------------------------------------------------------------------

    public ConnectionResult connectWithAutoDetect(Context context) throws Exception {
        int savedSize = ConfigContadorActivity.loadConfig(context).addressSize;
        int[] candidates = buildCandidateOrder(savedSize);

        Exception lastException = null;

        for (int size : candidates) {
            Log.d(TAG, "Probando addressSize=" + size);
            try {
                GXDLMSReader r = (device != null)
                        ? connectBluetooth(context, size)
                        : connectTcp(context, size);

                // Verificar que el contador realmente responde
                String serial = SerialNumberReader.readSerialNumer(r);
                if (serial != null && !serial.isEmpty()) {
                    Log.i(TAG, "addressSize=" + size + " OK. Serial=" + serial);

                    if (size != savedSize) {
                        saveAddressSize(context, size);
                        Log.i(TAG, "Nuevo addressSize guardado: " + size);
                    }
                    return new ConnectionResult(r, serial);
                }

                Log.w(TAG, "addressSize=" + size + " conectó pero no devolvió serial. Reintentando...");
                closeInternal();

            } catch (Exception e) {
                Log.w(TAG, "addressSize=" + size + " falló: " + e.getMessage());
                lastException = e;
                closeInternal();
            }
        }

        throw new Exception(
                "No se pudo conectar con ningún addressSize." +
                        (lastException != null ? " Último error: " + lastException.getMessage() : ""),
                lastException
        );
    }

    // -----------------------------------------------------------------------
    // Conexión TCP con addressSize explícito
    // -----------------------------------------------------------------------

    private GXDLMSReader connectTcp(Context context, int addressSize) throws Exception {
        net = new GXNet(NetworkType.TCP, ip, port);
        net.setTrace(TraceLevel.VERBOSE);
        net.open();
        Log.d(TAG, "Conectado por TCP a " + ip + ":" + port);

        configurarClienteDlms(context, addressSize);

        reader = new GXDLMSReader(client, net, TraceLevel.VERBOSE, null);
        reader.initializeConnection();
        return reader;
    }

    // -----------------------------------------------------------------------
    // Conexión Bluetooth con addressSize explícito
    // -----------------------------------------------------------------------

    private GXDLMSReader connectBluetooth(Context context, int addressSize) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) throw new Exception("Bluetooth no disponible en este dispositivo");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Permiso BLUETOOTH_CONNECT no concedido");
        }

        BluetoothDevice targetDevice = findBluetoothDevice(adapter, context);
        if (targetDevice == null) throw new Exception("No se encontró la sonda TesPro emparejada");

        UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket socket = targetDevice.createRfcommSocketToServiceRecord(SPP);

        try {
            Log.d(TAG, "Conectando a TesPro...");
            socket.connect();
            Log.d(TAG, "Conexión Bluetooth SPP establecida.");

            serial = new BluetoothCommunicator(socket);
            configurarClienteDlms(context, addressSize);

            reader = new GXDLMSReader(client, serial, TraceLevel.VERBOSE, null);
            reader.setContext(context);
            reader.initializeConnectionBluetooth();
            Log.i(TAG, "Handshake IEC completado.");

            return reader;

        } catch (IOException e) {
            Log.e(TAG, "Error conectando Bluetooth", e);
            try { socket.close(); } catch (IOException ignored) {}
            throw new Exception("Fallo al conectar Bluetooth: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Métodos legacy (por si los usas en otros sitios del proyecto)
    // -----------------------------------------------------------------------

    public ConnectionResult bluetoothConnnect(Context context) throws Exception {
        return connectWithAutoDetect(context);
    }

    public ConnectionResult tcpConnect(Context context) throws Exception {
        return connectWithAutoDetect(context);
    }

    // -----------------------------------------------------------------------
    // Configuración del cliente DLMS
    // -----------------------------------------------------------------------

    private void configurarClienteDlms(Context context, int addressSize) {
        ConfigContadorActivity.DLMSConfigValues cfg = ConfigContadorActivity.loadConfig(context);

        client = new GXDLMSSecureClient2(true);
        client.setInterfaceType(InterfaceType.HDLC);
        client.setServerAddress(GXDLMSClient.getServerAddress(
                cfg.logicalDevice, cfg.physicalDevice, addressSize));
        client.setClientAddress(cfg.clientAddress);
        client.setUseLogicalNameReferencing(true);
        client.setAuthentication(cfg.authentication);
        client.setPassword(cfg.password.getBytes());
        client.setMaxReceivePDUSize(236);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private int[] buildCandidateOrder(int preferred) {
        int[] ordered = new int[ADDRESS_SIZE_CANDIDATES.length];
        ordered[0] = preferred;
        int idx = 1;
        for (int s : ADDRESS_SIZE_CANDIDATES) {
            if (s != preferred) ordered[idx++] = s;
        }
        return ordered;
    }

    private BluetoothDevice findBluetoothDevice(BluetoothAdapter adapter, Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        Set<BluetoothDevice> paired = adapter.getBondedDevices();
        if (paired == null) return null;
        for (BluetoothDevice d : paired) {
            Log.d(TAG, "Emparejado: " + d.getName() + " (" + d.getAddress() + ")");
            if (d.getName() != null && d.getName().contains("TesPro")) return d;
        }
        return null;
    }

    private void saveAddressSize(Context context, int size) {
        context.getSharedPreferences(ConfigContadorActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(ConfigContadorActivity.KEY_ADDRESS_SIZE, size)
                .apply();
    }

    /** Cierra recursos internos entre intentos de auto-detect */
    private void closeInternal() {
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (serial != null && serial.isOpen()) serial.close(); } catch (Exception ignored) {}
        try { if (net != null && net.isOpen()) net.close(); } catch (Exception ignored) {}
        reader = null;
        serial = null;
        net = null;
    }

    public void close() {
        closeInternal();
    }

    public GXDLMSSecureClient2 getClient() {
        return client;
    }

    public static void closeConnection(GXDLMSReader reader, BluetoothCommunicator serial) {
        try { if (reader != null) reader.close(); } catch (Exception e) { Log.e(TAG, "Error cerrando reader", e); }
        try { if (serial != null && serial.isOpen()) serial.close(); } catch (Exception e) { Log.e(TAG, "Error cerrando serial", e); }
    }

    public static class ConnectionResult {
        public final GXDLMSReader reader;
        public final String serialNumber;

        public ConnectionResult(GXDLMSReader reader, String serialNumber) {
            this.reader = reader;
            this.serialNumber = serialNumber;
        }
    }
}