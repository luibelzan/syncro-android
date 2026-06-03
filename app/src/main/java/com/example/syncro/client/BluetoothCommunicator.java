package com.example.syncro.client;

import gurux.common.IGXMedia;
import gurux.common.IGXMediaListener;
import gurux.common.ReceiveParameters;
import gurux.common.enums.TraceLevel;
import gurux.dlms.GXByteBuffer;

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
    private int waitTime = 60000; // 10 segundos por defecto

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
        if (!isOpen() || params == null) return false;

        try {
            long startTime = System.currentTimeMillis();
            int timeoutMs = (params.getWaitTime() > 0) ? params.getWaitTime() : waitTime;

            // --- PASO 1: Esperar el byte de inicio 0x7E ---
            while (true) {
                if ((System.currentTimeMillis() - startTime) > timeoutMs) {
                    Log.w("BT", "Timeout esperando inicio de frame 0x7E");
                    return false;
                }
                if (in.available() > 0) {
                    int b = in.read();
                    if (b == 0x7E) break;
                } else {
                    Thread.sleep(5);
                }
            }

            // --- PASO 2: Leer bytes 1 y 2 (longitud del frame HDLC) ---
            // En HDLC: byte[1] bit[7:5] = "101", bit[4:0]+byte[2] = longitud
            byte[] header = readExact(2, startTime, timeoutMs);
            if (header == null) return false;

            // La longitud HDLC está en los 11 bits bajos de los 2 primeros bytes
            int frameLength = ((header[0] & 0x07) << 8) | (header[1] & 0xFF);

            if (frameLength < 5 || frameLength > 2048) {
                Log.e("BT", "Longitud HDLC inválida: " + frameLength);
                return false;
            }

            // --- PASO 3: Leer el resto del frame (frameLength - 2 ya leídos + 0x7E final) ---
            // Frame completo = 0x7E + 2 bytes header + (frameLength-2) bytes restantes + 0x7E
            int remaining = frameLength - 2 + 1; // -2 ya leídos, +1 para el 0x7E final
            byte[] rest = readExact(remaining, startTime, timeoutMs);
            if (rest == null) return false;

            // Verificar que termina en 0x7E
            if (rest[rest.length - 1] != 0x7E) {
                Log.e("BT", "Frame no termina en 0x7E");
                return false;
            }

            // --- PASO 4: Ensamblar frame completo ---
            byte[] fullFrame = new byte[1 + 2 + remaining];
            fullFrame[0] = 0x7E;
            fullFrame[1] = header[0];
            fullFrame[2] = header[1];
            System.arraycopy(rest, 0, fullFrame, 3, remaining);

            bytesReceived += fullFrame.length;
            Log.d("BT", "Frame HDLC recibido (" + fullFrame.length + " bytes): " + bytesToHex(fullFrame));

            params.setReply((T) fullFrame);
            return true;

        } catch (Exception e) {
            Log.e("DLMS", "Error en receive()", e);
            return false;
        }
    }

    /**
     * Lee exactamente 'count' bytes del stream con control de timeout.
     */
    private byte[] readExact(int count, long startTime, int timeoutMs) throws Exception {
        byte[] buf = new byte[count];
        int read = 0;
        while (read < count) {
            if ((System.currentTimeMillis() - startTime) > timeoutMs) {
                Log.e("BT", "Timeout leyendo " + count + " bytes (leídos: " + read + ")");
                return null;
            }
            if (in.available() > 0) {
                int b = in.read();
                if (b == -1) return null;
                buf[read++] = (byte) b;
                bytesReceived++;
            } else {
                Thread.sleep(5);
            }
        }
        return buf;
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

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int value) {
        this.waitTime = value;
    }
}