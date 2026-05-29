package com.example.syncro;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class EstadoSondaActivity extends BaseActivity {

    private TextView tvNombre, tvMac, tvBateria, tvFirmware;
    private Button btnActualizar;

    private BluetoothSocket socket;
    private BluetoothDevice targetDevice;

    private final String TARGET_NAME = "TesPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estado_sonda);

        tvNombre    = findViewById(R.id.tvNombre);
        tvMac       = findViewById(R.id.tvMac);
        tvBateria   = findViewById(R.id.tvBateria);
        btnActualizar = findViewById(R.id.btnActualizar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnActualizar.setOnClickListener(v -> conectarYLeerEstado());
    }

    private void conectarYLeerEstado() {
        runOnUiThread(() -> {
            tvBateria.setText("Conectando...");
            btnActualizar.setEnabled(false);
        });

        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                if (adapter == null) {
                    mostrarError("Bluetooth no disponible");
                    return;
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    mostrarError("Permiso Bluetooth no concedido");
                    return;
                }

                // Buscar dispositivo emparejado
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                targetDevice = null;
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName() != null && device.getName().contains(TARGET_NAME)) {
                        targetDevice = device;
                        break;
                    }
                }

                if (targetDevice == null) {
                    mostrarError("Sonda no encontrada");
                    return;
                }

                runOnUiThread(() -> {
                    tvNombre.setText("Nombre: " + targetDevice.getName());
                    tvMac.setText("MAC: " + targetDevice.getAddress());
                });

                // Conectar socket SPP
                UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                socket = targetDevice.createRfcommSocketToServiceRecord(SPP);
                socket.connect();
                Log.d("SONDA", "Conectado");

                OutputStream out = socket.getOutputStream();
                InputStream  in  = socket.getInputStream();

                // ── PASO 1: Activar modo comandos ──────────────────────────
                runOnUiThread(() -> tvBateria.setText("Activando modo comandos..."));

                String respuestaOpen = enviarComando(out, in, "at\r\n", 3000);
                Log.d("SONDA", "Respuesta at+open=1: '" + respuestaOpen + "'");

                if (!respuestaOpen.toLowerCase().contains("ok")) {
                    respuestaOpen = enviarComando(out, in, "AT\r\n", 3000);
                    Log.d("SONDA", "Respuesta AT+OPEN=1: '" + respuestaOpen + "'");
                }

                if (!respuestaOpen.toLowerCase().contains("ok")) {
                    respuestaOpen = enviarComando(out, in, "AT+EN1\r\n", 3000);
                    Log.d("SONDA", "Respuesta AT+EN1: '" + respuestaOpen + "'");
                }

                boolean modoComandosActivo = respuestaOpen.toLowerCase().contains("ok");
                Log.d("SONDA", "Modo comandos activo: " + modoComandosActivo);

                // ── PASO 2: Leer batería ───────────────────────────────────
                runOnUiThread(() -> tvBateria.setText("Leyendo batería..."));

                String respuestaBatt = enviarComando(out, in, "AT+BATT?\r\n", 2000);
                Log.d("SONDA", "Respuesta AT+BATT?: '" + respuestaBatt + "'");

                if (respuestaBatt.isEmpty()) {
                    respuestaBatt = enviarComando(out, in, "AT+CBC?\r\n", 2000);
                    Log.d("SONDA", "Respuesta AT+CBC?: '" + respuestaBatt + "'");
                }

                // ── PASO 3: Leer firmware ──────────────────────────────────
                String respuestaVer = enviarComando(out, in, "AT+VER\r\n", 2000);
                Log.d("SONDA", "Respuesta AT+VER: '" + respuestaVer + "'");

                // ── PASO 4: Leer MAC de la sonda ───────────────────────────
                String respuestaMac = enviarComando(out, in, "AT+ADDR\r\n", 2000);
                Log.d("SONDA", "Respuesta AT+ADDR: '" + respuestaMac + "'");

                // ── Actualizar UI ──────────────────────────────────────────
                final String batt = respuestaBatt.isEmpty() ? "Sin respuesta" : respuestaBatt;
                final String ver  = respuestaVer.isEmpty()  ? "Sin respuesta" : respuestaVer;
                final String mac  = respuestaMac.isEmpty()  ? targetDevice.getAddress() : respuestaMac;

                runOnUiThread(() -> {
                    tvBateria.setText("Batería: " + batt);
                    tvMac.setText("MAC: " + mac);
                    btnActualizar.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e("SONDA", "Error", e);
                mostrarError("Error: " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }).start();
    }

    /**
     * Envía un comando AT y espera la respuesta con lectura activa.
     */
    private String enviarComando(OutputStream out, InputStream in,
                                 String comando, int maxWaitMs) {
        try {
            // Limpiar buffer antes de enviar
            while (in.available() > 0) in.read();

            Log.d("SONDA_CMD", "Enviando: " + comando.trim());
            out.write(comando.getBytes());
            out.flush();

            StringBuilder sb    = new StringBuilder();
            long          start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < maxWaitMs) {
                if (in.available() > 0) {
                    byte[] buffer = new byte[256];
                    int len = in.read(buffer);
                    sb.append(new String(buffer, 0, len));

                    // Salir en cuanto tengamos una línea completa
                    String partial = sb.toString().trim();
                    if (partial.toLowerCase().contains("ok") || partial.endsWith("\n")) {
                        break;
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            String respuesta = sb.toString().trim();
            Log.d("SONDA_CMD", "Respuesta (" + (System.currentTimeMillis() - start) + "ms): '" + respuesta + "'");
            return respuesta;

        } catch (Exception e) {
            Log.e("SONDA", "Error enviando comando: " + comando.trim(), e);
            return "";
        }
    }

    private void mostrarError(String msg) {
        runOnUiThread(() -> {
            tvBateria.setText(msg);
            btnActualizar.setEnabled(true);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cerrarConexion();
    }

    private void cerrarConexion() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                Log.d("SONDA", "Socket cerrado");
            }
        } catch (Exception e) {
            Log.e("SONDA", "Error cerrando socket", e);
        }
    }
}