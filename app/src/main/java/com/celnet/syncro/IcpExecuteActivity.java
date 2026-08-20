package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.parameters.ControlModeResult;
import com.celnet.syncro.objects.params.ControlDisconnectMode;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class IcpExecuteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_icp_execute);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Desplegable Tipo Operación
        AutoCompleteTextView spinnerTipoOperacion = findViewById(R.id.spinnerTipoOperacion);
        String[] tiposOperacion = getResources().getStringArray(R.array.tipo_operaciones_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tiposOperacion);
        spinnerTipoOperacion.setAdapter(adapter);
        if (tiposOperacion.length > 0) {
            spinnerTipoOperacion.setText(tiposOperacion[0], false);
        }

        // Desplegable Operación
        AutoCompleteTextView spinnerOperacion = findViewById(R.id.spinnerOperacion);
        String[] operaciones = getResources().getStringArray(R.array.operaciones_array);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, operaciones);
        spinnerOperacion.setAdapter(adapter2);
        if (operaciones.length > 0) {
            spinnerOperacion.setText(operaciones[0], false);
        }

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String tipoOperacion = spinnerTipoOperacion.getText().toString();
            String operacion = spinnerOperacion.getText().toString();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            Toast.makeText(IcpExecuteActivity.this,
                    "Conexión: " + config.getType() + "\n",
                    Toast.LENGTH_LONG).show();

            // Bloquear interacción mientras carga
            btnNext.setEnabled(false);
            spinnerTipoOperacion.setEnabled(false);
            spinnerOperacion.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(IcpExecuteActivity.this);

                    ControlModeResult result;

                    if (operacion.equals("Connect")) {
                        result = ControlDisconnectMode.setControlDisconnectMode(res.reader, conn.getClient(), true);
                    } else {
                        result = ControlDisconnectMode.setControlDisconnectMode(res.reader, conn.getClient(), false);
                    }

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerTipoOperacion.setEnabled(true);
                        spinnerOperacion.setEnabled(true);

                        Intent intent = new Intent(IcpExecuteActivity.this, ResultadosIcpActivity.class);
                        intent.putExtra("success", result.success);
                        intent.putExtra("estadoInicial", result.estadoInicial);
                        intent.putExtra("estadoFinal", result.estadoFinal);
                        intent.putExtra("modoControl", result.modoControl);
                        intent.putExtra("mensaje", result.mensaje);

                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerTipoOperacion.setEnabled(true);
                        spinnerOperacion.setEnabled(true);
                        Toast.makeText(IcpExecuteActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } finally {
                    conn.close();
                }
            }).start();

        });
    }
}