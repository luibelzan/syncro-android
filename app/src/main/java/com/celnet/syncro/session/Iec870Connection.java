package com.celnet.syncro.session;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.session.ConnectionConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;

/**
 * Abre el transporte (Bluetooth SPP con la misma sonda que DLMS, o TCP) y
 * entrega un {@link Iec870Client} listo para usar.
 *
 * Uso:
 * <pre>
 *   try (Iec870Connection conn = new Iec870Connection(config)) {
 *       Iec870Client client = conn.open(context);
 *       client.openSession();
 *       String serie = client.readSerial();
 *       List&lt;Iec870LoadProfileRow&gt; filas = client.readLoadProfile(ini, fin, Periodo.HORARIA);
 *       client.closeSession();
 *   }
 * </pre>
 */
public class Iec870Connection implements AutoCloseable {

    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final ConnectionConfig config;
    private BluetoothSocket btSocket;
    private Socket tcpSocket;

    public Iec870Connection(ConnectionConfig config) {
        this.config = config;
    }

    public Iec870Client open(Context context) throws Iec870Exception {
        try {
            if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                btSocket = openBluetooth(context);
                return new Iec870Client(btSocket.getInputStream(), btSocket.getOutputStream(),
                        config.getIecLinkAddress(), config.getIecMeasuringPoint(),
                        config.getIecPassword());
            } else {
                tcpSocket = new Socket();
                tcpSocket.connect(new InetSocketAddress(config.getIp(), config.getPort()), 10_000);
                return new Iec870Client(tcpSocket.getInputStream(), tcpSocket.getOutputStream(),
                        config.getIecLinkAddress(), config.getIecMeasuringPoint(),
                        config.getIecPassword());
            }
        } catch (IOException e) {
            close();
            throw new Iec870Exception("No se pudo abrir la conexión: " + e.getMessage(), e);
        }
    }

    private BluetoothSocket openBluetooth(Context context) throws Iec870Exception, IOException {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) throw new Iec870Exception("Bluetooth no disponible en este dispositivo");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new Iec870Exception("Permiso BLUETOOTH_CONNECT no concedido");
        }

        // El nombre guardado es el ID interno de la marca (ver ProbeBrands),
        // la misma búsqueda por subcadena que usa DLMSConnection.
        String marca = config.getBluetoothDeviceName();
        BluetoothDevice target = null;
        Set<BluetoothDevice> paired = adapter.getBondedDevices();
        if (paired != null && marca != null) {
            for (BluetoothDevice d : paired) {
                if (d.getName() != null
                        && d.getName().toLowerCase().contains(marca.toLowerCase())) {
                    target = d;
                    break;
                }
            }
        }
        if (target == null) throw new Iec870Exception("No se encontró la sonda emparejada");

        adapter.cancelDiscovery();
        BluetoothSocket socket = target.createRfcommSocketToServiceRecord(SPP);
        try {
            socket.connect();
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) { }
            throw e;
        }
        return socket;
    }

    @Override
    public void close() {
        try { if (btSocket != null) btSocket.close(); } catch (IOException ignored) { }
        try { if (tcpSocket != null) tcpSocket.close(); } catch (IOException ignored) { }
        btSocket = null;
        tcpSocket = null;
    }
}