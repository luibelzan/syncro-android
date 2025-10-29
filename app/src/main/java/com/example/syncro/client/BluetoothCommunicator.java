package com.example.syncro.client;

import gurux.common.IGXMedia;
import gurux.common.IGXMediaListener;
import gurux.common.ReceiveParameters;
import gurux.common.enums.TraceLevel;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class BluetoothCommunicator implements IGXMedia {
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    private boolean isOpen = false;
    private TraceLevel trace = TraceLevel.OFF;
    private final List<IGXMediaListener> listeners = new ArrayList<>();

    // Contadores de bytes
    private long bytesSent = 0;
    private long bytesReceived = 0;

    // Configuración
    private String settings = "";
    private Object eop = -1;
    private int configurableSettings = 0;
    private Object synchronous = new Object();
    private boolean isSynchronous = false;

    public BluetoothCommunicator(BluetoothSocket socket) throws Exception {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.isOpen = true;
    }

    // --- MÉTODOS REQUERIDOS POR IGXMedia ---

    @Override
    public void addListener(IGXMediaListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(IGXMediaListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void copy(Object target) {
        // No usado en Android
    }

    @Override
    public String getName() {
        return "Bluetooth";
    }

    @Override
    public TraceLevel getTrace() {
        return trace;
    }

    @Override
    public void setTrace(TraceLevel value) {
        this.trace = value;
    }

    @Override
    public void open() throws Exception {
        if (socket != null && !socket.isConnected()) {
            socket.connect();
        }
        isOpen = true;
        //notifyMediaStateChange(true);
    }

    @Override
    public boolean isOpen() {
        return isOpen && socket != null && socket.isConnected();
    }

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
                isOpen = false;
                //notifyMediaStateChange(false);
                Log.i("DLMS", "BluetoothCommunicator cerrado.");
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error cerrando BluetoothCommunicator", e);
        }
    }

    @Override
    public void send(Object data, String target) throws Exception {
        if (!isOpen()) throw new IllegalStateException("Comunicador cerrado");
        if (data instanceof byte[]) {
            byte[] bytes = (byte[]) data;
            out.write(bytes);
            out.flush();
            bytesSent += bytes.length;
            //notifyTrace(new TraceEventArgs(TraceTypes.INFO, "TX: " + bytesToHex(bytes)));
        } else {
            throw new IllegalArgumentException("Solo se admiten byte[]");
        }
    }

    @Override
    public String getMediaType() {
        return "Bluetooth";
    }

    @Override
    public String getSettings() {
        return settings;
    }

    @Override
    public void setSettings(String value) {
        this.settings = value != null ? value : "";
    }

    @Override
    public Object getSynchronous() {
        return synchronous;
    }

    @Override
    public boolean getIsSynchronous() {
        return isSynchronous;
    }

    @Override
    public <T> boolean receive(ReceiveParameters<T> params) {
        if (!isOpen()) return false;

        try {
            int count = in.available();
            if (count == 0) return false;

            byte[] buffer = new byte[Math.min(count, 512)];
            int read = in.read(buffer);
            if (read > 0) {
                byte[] data = new byte[read];
                System.arraycopy(buffer, 0, data, 0, read);
                bytesReceived += read;

                if (params.getReply() != null && params.getReply() instanceof byte[]) {
                    // Asignar datos recibidos
                    System.arraycopy(data, 0, params.getReply(), 0, Math.min(data.length, ((byte[]) params.getReply()).length));
                }

                //notifyTrace(new TraceEventArgs(TraceTypes.INFO, "RX: " + bytesToHex(data)));
                return true;
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error en receive()", e);
        }
        return false;
    }

    @Override
    public void resetSynchronousBuffer() {
        synchronous = null;
    }

    @Override
    public long getBytesSent() {
        return bytesSent;
    }

    @Override
    public long getBytesReceived() {
        return bytesReceived;
    }

    @Override
    public void resetByteCounters() {
        bytesSent = 0;
        bytesReceived = 0;
    }

    @Override
    public void validate() {
        // Validación básica
        if (socket == null || !socket.isConnected()) {
            throw new IllegalStateException("Socket Bluetooth no conectado");
        }
    }

    @Override
    public Object getEop() {
        return eop;
    }

    @Override
    public void setEop(Object value) {
        this.eop = value;
    }

    @Override
    public int getConfigurableSettings() {
        return configurableSettings;
    }

    @Override
    public void setConfigurableSettings(int value) {
        this.configurableSettings = value;
    }

    @Override
    public boolean properties(javax.swing.JFrame parent) {
        return false;
    }

    // --- MÉTODOS AUXILIARES ---
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}