package com.example.syncro.client;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

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

public class DLMSConnection {

    public final GXDLMSSecureClient2 client;
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;

    public DLMSConnection(GXDLMSSecureClient2 client, BluetoothSocket socket) throws Exception {
        this.client = client;
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public static DLMSConnection initializeConnection(Context context) throws Exception {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) throw new Exception("Bluetooth no disponible en este dispositivo");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Permiso BLUETOOTH_CONNECT no concedido");
        }

        // Buscar un dispositivo emparejado (ajusta el nombre o MAC)
        String targetName = "TesPro V4_7706";   // o podrías usar el prefijo "TesPro"
        String targetAddress = "0F:03:25:80:80:1F";

        BluetoothDevice targetDevice = null;
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("DLMS", "Emparejado: " + device.getName() + " (" + device.getAddress() + ")");
                if (device.getName() != null && device.getName().equals(targetName)) {
                    targetDevice = device;
                    break;
                }
                // También podrías comparar por dirección:
                // if (device.getAddress().equals(targetAddress)) { targetDevice = device; break; }
            }
        }

        if (targetDevice == null) {
            throw new Exception("No se encontró la sonda TesPro emparejada");
        }

        // Conexión SPP
        UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket socket = targetDevice.createRfcommSocketToServiceRecord(SPP);
        adapter.cancelDiscovery();
        socket.connect();

        // Configurar cliente DLMS
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);
        client.setInterfaceType(InterfaceType.HDLC);
        client.setServerAddressSize(2);
        client.setServerAddress(144);
        client.setClientAddress(1);
        client.setUseLogicalNameReferencing(true);
        client.setAuthentication(Authentication.LOW);
        client.setPassword("00000002");
        // Forzar conformance correcto
        Set<Conformance> proposed = new HashSet<>();
        proposed.add(Conformance.GET);
        proposed.add(Conformance.SET);
        proposed.add(Conformance.SELECTIVE_ACCESS);
        proposed.add(Conformance.BLOCK_TRANSFER_WITH_GET_OR_READ);
        proposed.add(Conformance.BLOCK_TRANSFER_WITH_SET_OR_WRITE);
        proposed.add(Conformance.BLOCK_TRANSFER_WITH_ACTION);
        proposed.add(Conformance.ACTION);
        proposed.add(Conformance.MULTIPLE_REFERENCES);
        proposed.add(Conformance.DATA_NOTIFICATION);
        proposed.add(Conformance.ACCESS);
        proposed.add(Conformance.ATTRIBUTE_0_SUPPORTED_WITH_SET);
        proposed.add(Conformance.PRIORITY_MGMT_SUPPORTED);

        client.setProposedConformance(proposed);

        // Opcional: forzar dedicated key y versión
        client.setUseUtc2NormalTime(false);
        client.setAutoIncreaseInvokeID(true);

        /*
        client.getHdlcSettings().setMaxInfoTX(128);
        client.getHdlcSettings().setMaxInfoRX(128);
        client.getHdlcSettings().setWindowSizeTX(1);
        client.getHdlcSettings().setWindowSizeRX(1);
         */

        Log.i("DLMS", "Bluetooth conectado. Iniciando handshake...");

        // --- Handshake DLMS ---
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        // --- SNRM → UA ---
        byte[] snrmFrame = client.snrmRequest();
        out.write(snrmFrame);
        out.flush();

        Log.d("DLMS", "SNRM sent: " + bytesToHex(snrmFrame, snrmFrame.length));

        GXByteBuffer bufferSnrm = new GXByteBuffer();
        byte[] tmpSnrm = new byte[512];
        long startTimeSnrm = System.currentTimeMillis();
        boolean completeSnrm = false;

        while (System.currentTimeMillis() - startTimeSnrm < 10000 && !completeSnrm) {  // Timeout de 10 segundos
            int count = in.read(tmpSnrm);
            if (count > 0) {
                bufferSnrm.set(tmpSnrm, 0, count);
                Log.d("DLMS", "Bytes received so far for SNRM: " + bytesToHex(bufferSnrm.array(), bufferSnrm.size()));

                // Verifica si es un frame completo: empieza y termina con 7E, y tiene al menos longitud mínima (ej. 8-10 bytes)
                if (bufferSnrm.size() >= 8 && bufferSnrm.getUInt8(0) == 0x7E && bufferSnrm.getUInt8(bufferSnrm.size() - 1) == 0x7E) {
                    completeSnrm = true;
                }
            } else if (count == -1) {
                throw new Exception("Stream cerrado inesperadamente durante SNRM");
            }

            if (!completeSnrm) {
                Thread.sleep(200);  // Delay de 200ms para esperar más datos (ajusta si es necesario)
            }
        }

        if (bufferSnrm.size() == 0) {
            throw new Exception("No se recibió respuesta al SNRM");
        }

        Log.d("DLMS", "UA reply completa: " + bytesToHex(bufferSnrm.array(), bufferSnrm.size()));
        GXReplyData reply = new GXReplyData();
        client.getData(bufferSnrm.array(), reply);
        client.parseUAResponse(reply.getData());

        // --- AARQ → AARE ---
        byte[][] aarqFrames = client.aarqRequest();
        for (byte[] frame : aarqFrames) {
            out.write(frame);
            out.flush();

            Log.d("DLMS", "AARQ frame sent: " + bytesToHex(frame, frame.length));

            GXByteBuffer bufferAarq = new GXByteBuffer();
            byte[] tmpAarq = new byte[512];
            long startTimeAarq = System.currentTimeMillis();
            boolean completeAarq = false;

            while (System.currentTimeMillis() - startTimeAarq < 10000 && !completeAarq) {  // Timeout de 10 segundos
                int count = in.read(tmpAarq);
                if (count > 0) {
                    bufferAarq.set(tmpAarq, 0, count);
                    Log.d("DLMS", "Bytes received so far for AARQ: " + bytesToHex(bufferAarq.array(), bufferAarq.size()));

                    // Verifica si es un frame completo: empieza y termina con 7E, y tiene al menos longitud mínima
                    if (bufferAarq.size() >= 8 && bufferAarq.getUInt8(0) == 0x7E && bufferAarq.getUInt8(bufferAarq.size() - 1) == 0x7E) {
                        completeAarq = true;
                    }
                } else if (count == -1) {
                    throw new Exception("Stream cerrado inesperadamente durante AARQ");
                }

                if (!completeAarq) {
                    Thread.sleep(200);  // Delay de 200ms
                }
            }

            if (bufferAarq.size() == 0) {
                throw new Exception("No se recibió respuesta al AARQ");
            }

            Log.d("DLMS", "AARE reply completa: " + bytesToHex(bufferAarq.array(), bufferAarq.size()));
            GXReplyData replyAARQ = new GXReplyData();
            client.getData(bufferAarq.array(), replyAARQ);
            client.parseAareResponse(replyAARQ.getData());
        }

        Log.i("DLMS", "Conexión DLMS establecida correctamente.");

        return new DLMSConnection(client, socket);
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            Log.e("DLMS", "Error cerrando socket", e);
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