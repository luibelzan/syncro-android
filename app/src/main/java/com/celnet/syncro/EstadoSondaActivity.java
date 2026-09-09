package com.celnet.syncro;

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

import com.celnet.syncro.utils.ProbeBrands;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class EstadoSondaActivity extends BaseActivity {

    private TextView tvNombre, tvMac, tvBateria;
    private Button btnActualizar;

    private BluetoothSocket socket;
    private BluetoothDevice targetDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estado_sonda);

        tvNombre      = findViewById(R.id.tvNombre);
        tvMac         = findViewById(R.id.tvMac);
        tvBateria     = findViewById(R.id.tvBateria);
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

                // ── Buscar dispositivo emparejado (cualquier marca soportada) ──
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                targetDevice = null;
                for (BluetoothDevice device : pairedDevices) {
                    if (ProbeBrands.coincideNombreSonda(device.getName())) {
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

                // ── Conectar socket SPP ────────────────────────────────────
                UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                socket = targetDevice.createRfcommSocketToServiceRecord(SPP);
                socket.connect();
                Log.d("SONDA", "Conectado");

                OutputStream out = socket.getOutputStream();
                InputStream  in  = socket.getInputStream();

                // ⚠️ TODO Bigrid: los comandos "GetBatteryVolt" / "AT+ADDR" y el
                // formato de respuesta "V=XXXX" son específicos del firmware de
                // TesPro. Si la sonda emparejada es Bigrid, este protocolo
                // probablemente NO es el correcto y devolverá una lectura sin
                // sentido (o vacía) sin lanzar ningún error visible. En cuanto
                // tengas el protocolo de comandos de Bigrid, aquí hay que
                // ramificar según targetDevice.getName() para usar el conjunto
                // de comandos correcto según la marca.

                // ── PASO 1: Leer batería ───────────────────────────────────
                // Comando del fabricante (TesPro): "GetBatteryVolt"
                // Respuesta esperada: "V=4027" (valor en milivoltios)
                runOnUiThread(() -> tvBateria.setText("Leyendo batería..."));

                String respuestaBatt = enviarComando(out, in, "GetBatteryVolt\r\n", 3000);
                Log.d("SONDA", "Respuesta GetBatteryVolt: '" + respuestaBatt + "'");

                // ── PASO 2: Parsear voltaje y calcular porcentaje ──────────
                String bateriaTexto = parsearBateria(respuestaBatt);

                // ── PASO 3: Leer MAC de la sonda ───────────────────────────
                String respuestaMac = enviarComando(out, in, "AT+ADDR\r\n", 2000);
                Log.d("SONDA", "Respuesta AT+ADDR: '" + respuestaMac + "'");
                final String mac = respuestaMac.isEmpty() ? targetDevice.getAddress() : respuestaMac;

                // ── Actualizar UI ──────────────────────────────────────────
                final String batt = bateriaTexto;
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
     * Parsea la respuesta "V=4027" del comando GetBatteryVolt.
     * Convierte milivoltios a voltios y calcula el porcentaje estimado
     * basado en el rango típico de una batería LiPo (3.3V–4.2V).
     */
    private String parsearBateria(String respuesta) {
        if (respuesta == null || respuesta.isEmpty()) return "Sin respuesta";

        if (respuesta.contains("V=")) {
            try {
                String valorStr = respuesta
                        .substring(respuesta.indexOf("V=") + 2)
                        .trim()
                        .split("[\\r\\n\\s]+")[0];

                int raw = Integer.parseInt(valorStr);

                // Determinar el factor de escala según el rango del valor
                double voltios;
                if (raw > 1000) {
                    // Ya viene en mV (ej: 4027 → 4.027V)
                    voltios = raw / 1000.0;
                } else if (raw > 100) {
                    // Viene en décimas de mV × 10 (ej: 381 → 3.81V)
                    voltios = raw / 100.0;
                } else {
                    voltios = raw; // Fallback: mostrar raw
                }

                int porcentaje = (int) Math.min(100, Math.max(0,
                        (voltios - 3.3) / (4.2 - 3.3) * 100
                ));

                Log.d("SONDA", String.format("Batería raw=%d → %.3fV → %d%%",
                        raw, voltios, porcentaje));

                return String.format("%.2fV  (%d%%)", voltios, porcentaje);

            } catch (NumberFormatException e) {
                return respuesta;
            }
        }
        return respuesta;
    }

    /**
     * Envía un comando y espera la respuesta con lectura activa.
     * Sale en cuanto detecta una línea completa o el patrón esperado.
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
                    int    len    = in.read(buffer);
                    sb.append(new String(buffer, 0, len));

                    String partial = sb.toString().trim();

                    // Salir en cuanto tengamos una respuesta completa:
                    // "ok" para comandos AT, "V=XXXX" para GetBatteryVolt
                    if (partial.toLowerCase().contains("ok")
                            || partial.startsWith("V=")
                            || partial.endsWith("\n")) {
                        break;
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            String respuesta = sb.toString().trim();
            Log.d("SONDA_CMD", "Respuesta (" + (System.currentTimeMillis() - start) + "ms): '"
                    + respuesta + "'");
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