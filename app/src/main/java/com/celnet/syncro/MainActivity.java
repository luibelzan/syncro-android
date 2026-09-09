package com.celnet.syncro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import com.celnet.syncro.client.DLMSConnection;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.celnet.syncro.utils.PasswordHelper;
import com.celnet.syncro.utils.ProbeBrands;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private RadioGroup radioGroupConexion;
    private LinearLayout layoutBluetooth;
    private LinearLayout layoutTcp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // En MainActivity.onCreate() o en tu clase Application:
        PasswordHelper.initDefaultPasswordIfNeeded(this);

        // 🔹 Verificar permisos Bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }

        // Referencias
        radioGroupConexion = findViewById(R.id.radioGroupConexion);
        layoutBluetooth = findViewById(R.id.layoutBluetooth);
        layoutTcp = findViewById(R.id.layoutTcp);

        // Mostrar layout inicial según selección por defecto
        layoutBluetooth.setVisibility(View.VISIBLE);
        layoutTcp.setVisibility(View.GONE);

        if (radioGroupConexion.getCheckedRadioButtonId() == R.id.rbTcp) {
            layoutBluetooth.setVisibility(View.GONE);
            layoutTcp.setVisibility(View.VISIBLE);
        }

        // Listener para cambio de selección
        radioGroupConexion.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.rbBluetooth) {

                layoutBluetooth.setVisibility(View.VISIBLE);
                layoutTcp.setVisibility(View.GONE);

            } else if (checkedId == R.id.rbTcp) {

                layoutBluetooth.setVisibility(View.GONE);
                layoutTcp.setVisibility(View.VISIBLE);
            }
        });

        // 🔹 Desplegable de sondas: se muestran las ETIQUETAS visibles
        // (p. ej. "TesPro", "Otros"), no los IDs internos reales de marca.
        AutoCompleteTextView spinnerSonda = findViewById(R.id.spinnerSonda);

        String[] sondas = ProbeBrands.etiquetasVisibles();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, sondas);
        spinnerSonda.setAdapter(adapter);
        if (sondas.length > 0) {
            spinnerSonda.setText(sondas[0], false);
        }

        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            MaterialRadioButton rbBluetooth = findViewById(R.id.rbBluetooth);
            TextInputEditText etIp = findViewById(R.id.etIp);
            TextInputEditText etPort = findViewById(R.id.etPort);

            ConnectionConfig config;

            if (rbBluetooth.isChecked()) {
                config = new ConnectionConfig(ConnectionConfig.ConnectionType.BLUETOOTH);
                // Traducir la etiqueta visible elegida (p. ej. "Otros") a su
                // ID interno real (p. ej. "bigrid") antes de guardarla — el
                // filtrado por nombre en DLMSConnection/ProbeBrands trabaja
                // siempre con IDs internos, nunca con las etiquetas de la UI.
                String etiquetaElegida = spinnerSonda.getText().toString();
                String deviceName = ProbeBrands.idInternoParaEtiqueta(etiquetaElegida);
                config.setBluetoothDeviceName(deviceName);
            } else {

                String ip =
                        etIp.getText().toString().trim();

                String puertoTexto =
                        etPort.getText().toString().trim();

                if (ip.isEmpty()) {

                    etIp.setError("Introduzca una dirección IP");
                    etIp.requestFocus();
                    return;
                }

                if (!esIpValida(ip)) {

                    etIp.setError("Formato IP inválido");
                    etIp.requestFocus();
                    return;
                }

                if (puertoTexto.isEmpty()) {

                    etPort.setError("Introduzca un puerto");
                    etPort.requestFocus();
                    return;
                }

                int port;

                try {

                    port = Integer.parseInt(puertoTexto);

                } catch (NumberFormatException e) {

                    etPort.setError("Puerto inválido");
                    etPort.requestFocus();
                    return;
                }

                if (port < 1 || port > 65535) {

                    etPort.setError(
                            "El puerto debe estar entre 1 y 65535");

                    etPort.requestFocus();
                    return;
                }

                config = new ConnectionConfig(
                        ConnectionConfig.ConnectionType.TCP);

                config.setIp(ip);
                config.setPort(port);
            }

            // Guardamos la configuración actual
            SessionManager.getInstance().setConnectionConfig(config);

            // Ir a la siguiente actividad
            startActivity(new Intent(MainActivity.this, SecondActivity.class));
        });

    }

    private boolean esIpValida(String ip) {

        String patron =
                "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}" +
                        "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$";

        return ip.matches(patron);
    }
}