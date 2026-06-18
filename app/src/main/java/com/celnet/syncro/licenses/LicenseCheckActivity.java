package com.celnet.syncro.licenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.celnet.syncro.MainActivity;
import com.celnet.syncro.R;

/**
 * LicenseCheckActivity
 *
 * Se muestra cuando no hay licencia válida guardada.
 * El usuario debe:
 *   1. Conectar la sonda Bluetooth (para obtener la MAC)
 *   2. Introducir el código de licencia
 *   3. Pulsar "Activar"
 *
 * Si la activación es correcta, redirige a MainActivity.
 */
public class LicenseCheckActivity extends AppCompatActivity {

    private EditText    etLicenseCode;
    private Button      btnActivate;
    private LinearLayout progressContainer;
    private TextView    tvStatus;
    private TextView    tvMacInfo;

    // MAC obtenida durante el escaneo Bluetooth previo
    // Se pasa como extra del Intent desde BluetoothScanActivity
    private String deviceMac = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_check);

        etLicenseCode    = findViewById(R.id.etLicenseCode);
        btnActivate      = findViewById(R.id.btnActivate);
        progressContainer = findViewById(R.id.progressContainer);
        tvStatus         = findViewById(R.id.tvStatus);
        tvMacInfo        = findViewById(R.id.tvMacInfo);

        // Recibir la MAC de la sonda desde la actividad anterior
        deviceMac = getIntent().getStringExtra("device_mac");

        if (deviceMac != null) {
            tvMacInfo.setText("Sonda detectada: " + deviceMac);
            tvMacInfo.setVisibility(View.VISIBLE);
        } else {
            tvMacInfo.setText("⚠ Conecta primero la sonda Bluetooth para activar la licencia.");
            tvMacInfo.setVisibility(View.VISIBLE);
            btnActivate.setEnabled(false);
        }

        btnActivate.setOnClickListener(v -> attemptActivation());
    }

    private void attemptActivation() {
        String code = etLicenseCode.getText().toString().trim();

        if (code.isEmpty()) {
            etLicenseCode.setError("Introduce el código de licencia");
            return;
        }

        if (deviceMac == null) {
            Toast.makeText(this, "Conecta la sonda Bluetooth primero", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvStatus.setText("");

        LicenseManager.activate(this, code, deviceMac, (status, customer) -> {
            runOnUiThread(() -> {
                setLoading(false);
                switch (status) {
                    case VALID:
                        tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                        tvStatus.setText("✓ Licencia activada. Bienvenido, " +
                                (customer != null ? customer : "") + ".");
                        // Pequeña pausa para que el usuario vea el mensaje y luego navegar
                        btnActivate.postDelayed(() -> {
                            startActivity(new Intent(this, MainActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_NEW_TASK));
                            finish();
                        }, 1200);
                        break;

                    case INVALID_CODE:
                        showError("Código de licencia no válido.");
                        break;

                    case INVALID_MAC:
                        showError("Este código no está autorizado para esta sonda.\n(" + deviceMac + ")");
                        break;

                    case EXPIRED:
                        showError("La licencia ha caducado. Contacta con el soporte.");
                        break;

                    case DISABLED:
                        showError("La licencia ha sido desactivada. Contacta con el soporte.");
                        break;

                    case NETWORK_ERROR:
                        showError("Sin conexión a Internet. Verifica la red e inténtalo de nuevo.");
                        break;
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        progressContainer.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnActivate.setEnabled(!loading);
        etLicenseCode.setEnabled(!loading);
    }

    private void showError(String message) {
        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        tvStatus.setText("✗ " + message);
    }
}