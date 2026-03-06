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
            GXByteBuffer buffer = new GXByteBuffer();
            long startTime = System.currentTimeMillis();
            byte eopByte = -1;

            // Determinar EOP
            if (params.getEop() instanceof Byte) {
                eopByte = (Byte) params.getEop();
            } else if (params.getEop() instanceof byte[]) {
                byte[] eopArr = (byte[]) params.getEop();
                if (eopArr.length > 0) eopByte = eopArr[0];
            }

            // Si no hay EOP, no sabemos cuándo termina → error
            if (eopByte == -1 && params.getEop() != null) {
                return false;
            }

            int minFrameSize = 8; // HDLC mínimo

            while (true) {
                // Timeout
                if (params.getWaitTime() > 0 && (System.currentTimeMillis() - startTime) > params.getWaitTime()) {
                    Log.d("BT", "Timeout en receive()");
                    return false;
                }

                // Leer byte por byte
                if (in.available() > 0) {
                    int b = in.read();
                    if (b == -1) return false;

                    buffer.setUInt8((byte) b);
                    bytesReceived++;

                    // Si tenemos EOP y suficiente tamaño
                    if (eopByte != -1 && b == eopByte && buffer.size() >= minFrameSize) {
                        // Verificar que también empieza con 0x7E
                        if (buffer.getUInt8(0) == 0x7E) {
                            // ¡Frame completo!
                            byte[] replyData = buffer.array();

                            // Asignar al parámetro de salida
                            if (params.getReply() == null) {
                                params.setReply((T) replyData);
                            } else if (params.getReply() instanceof byte[]) {
                                byte[] target = (byte[]) params.getReply();
                                if (target.length >= replyData.length) {
                                    System.arraycopy(replyData, 0, target, 0, replyData.length);
                                } else {
                                    // Si el buffer es pequeño, crear uno nuevo
                                    params.setReply((T) replyData);
                                }
                            }

                            Log.d("BT", "Frame HDLC recibido: " + bytesToHex(replyData));
                            return true;
                        }
                    }
                } else {
                    Thread.sleep(5); // Evitar busy-wait
                }
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error en receive()", e);
            return false;
        }
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