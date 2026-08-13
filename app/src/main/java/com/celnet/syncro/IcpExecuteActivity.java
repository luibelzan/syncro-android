package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.ControlModeResult;
import com.celnet.syncro.objects.params.ControlDisconnectMode;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

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

        //Spinner Tipo Operacion
        Spinner spinnerTipoOperacion = findViewById(R.id.spinnerTipoOperacion);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.tipo_operaciones_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerTipoOperacion.setAdapter(adapter);
        spinnerTipoOperacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Spinner Operacion
        Spinner spinnerOperacion = findViewById(R.id.spinnerOperacion);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,
                R.array.operaciones_array,
                android.R.layout.simple_spinner_item
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerOperacion.setAdapter(adapter2);
        spinnerOperacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String tipoOperacion = spinnerTipoOperacion.getSelectedItem().toString();
            String operacion = spinnerOperacion.getSelectedItem().toString();
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
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(IcpExecuteActivity.this);

                    ControlModeResult result;

                    if (operacion.equals("Connect")) {
                        result = ControlDisconnectMode.setControlDisconnectMode(res.reader, conn.getClient(), true);
                    } else {
                        result = ControlDisconnectMode.setControlDisconnectMode(res.reader, conn.getClient(), false);
                    }
                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerTipoOperacion.setEnabled(true);
                        spinnerOperacion.setEnabled(true);

                        Intent intent = new Intent(IcpExecuteActivity.this, ResultadosIcpActivity.class);
                        intent.putExtra("success", result.success);
                        intent.putExtra("estadoInicial", result.estadoInicial);
                        intent.putExtra("estadoFinal", result.estadoFinal);
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
                }
            }).start();

        });
    }
}