package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.objects.contracts.ViewContract;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Arrays;

public class VerContratoActivity extends BaseActivity {

    private AutoCompleteTextView spinnerContrato;
    private String[] contratosArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ver_contrato);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Desplegable de contrato
        spinnerContrato = findViewById(R.id.spinnerContrato);
        contratosArray = getResources().getStringArray(R.array.program_contracts_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, contratosArray);
        spinnerContrato.setAdapter(adapter);
        if (contratosArray.length > 0) {
            spinnerContrato.setText(contratosArray[0], false);
        }

        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        btnNext.setOnClickListener(v -> {

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // ── Contrato ────────────────────────────────────────────────────────
            int posicionContrato = Arrays.asList(contratosArray).indexOf(spinnerContrato.getText().toString());
            int contract = (posicionContrato >= 0 ? posicionContrato : 0) + 1;

            // Bloquear interacción mientras carga
            btnNext.setEnabled(false);
            spinnerContrato.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            // ── Ejecutar en hilo de fondo (DLMS no puede ir en el hilo UI) ──────
            new Thread(() -> {
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(VerContratoActivity.this);

                    String datos = ViewContract.leerContrato(res.reader, contract);

                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerContrato.setEnabled(true);

                        Intent intent = new Intent(VerContratoActivity.this, ResultadosVerContratoActivity.class);
                        intent.putExtra("datos_contrato", datos);
                        startActivity(intent);
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerContrato.setEnabled(true);
                        Toast.makeText(this,
                                "Error al programar contrato: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }
}