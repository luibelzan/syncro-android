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

    private TextView tvNombre, tvMac, tvBateria;
    private Button btnActualizar;

    private BluetoothSocket socket;
    private BluetoothDevice targetDevice;

    private final String TARGET_NAME = "TesPro V4_7706";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estado_sonda);

        // ✅ Inicializar vistas
        tvNombre = findViewById(R.id.tvNombre);
        tvMac = findViewById(R.id.tvMac);
        tvBateria = findViewById(R.id.tvBateria);
        btnActualizar = findViewById(R.id.btnActualizar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Acción del botón
        btnActualizar.setOnClickListener(v -> conectarYLeerEstado());
    }

    private void conectarYLeerEstado() {

        // Mostrar estado inicial
        runOnUiThread(() -> tvBateria.setText("Conectando..."));

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

                // 🔍 Buscar dispositivo emparejado
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

                targetDevice = null;

                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName() != null && device.getName().equals(TARGET_NAME)) {
                        targetDevice = device;
                        break;
                    }
                }

                if (targetDevice == null) {
                    mostrarError("Sonda no encontrada");
                    return;
                }

                // 📱 Mostrar info básica
                runOnUiThread(() -> {
                    tvNombre.setText("Nombre: " + targetDevice.getName());
                    tvMac.setText("MAC: " + targetDevice.getAddress());
                });

                // 🔗 Crear conexión SPP
                UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                socket = targetDevice.createRfcommSocketToServiceRecord(SPP);

                Log.d("SONDA", "Conectando...");
                socket.connect();
                Log.d("SONDA", "Conectado");

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // 🔋 Comando batería (⚠️ puede variar según fabricante)
                String comando = "AT+BATT?\r\n";
                out.write(comando.getBytes());

                Thread.sleep(500);

                byte[] buffer = new byte[256];
                int len;
                StringBuilder sb = new StringBuilder();
                long startTime = System.currentTimeMillis();

                while (System.currentTimeMillis() - startTime < 3000) { // Espera 3 segundos
                    if (in.available() > 0) {
                        len = in.read(buffer);
                        sb.append(new String(buffer, 0, len));
                    }
                }

                String respuesta = sb.toString().trim();
                if (!respuesta.isEmpty()) {
                    runOnUiThread(() -> tvBateria.setText("Batería: " + respuesta));
                } else {
                    mostrarError("Sin respuesta de batería");
                }

            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void mostrarError(String msg) {
        runOnUiThread(() -> tvBateria.setText(msg));
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