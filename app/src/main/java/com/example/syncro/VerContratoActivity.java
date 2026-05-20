package com.example.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.objects.contracts.ViewContract;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VerContratoActivity extends AppCompatActivity {

    private Spinner spinnerContrato;

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

        //Spinner de contrato
        spinnerContrato = findViewById(R.id.spinnerContrato);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.program_contracts_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContrato.setAdapter(adapter);
        spinnerContrato.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ImageButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        btnNext.setOnClickListener(v -> {

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // ── Contrato ────────────────────────────────────────────────────────
            int contract = spinnerContrato.getSelectedItemPosition() + 1;

            progressBar.setVisibility(View.VISIBLE);

            // ── Ejecutar en hilo de fondo (DLMS no puede ir en el hilo UI) ──────
            new Thread(() -> {
                GXDLMSReader reader;
                DLMSConnection conn;
                try {

                    if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                        conn = new DLMSConnection(config.getBluetoothDeviceName());
                        reader = conn.bluetoothConnnect(VerContratoActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect(this);
                    }

                    String datos = ViewContract.leerContrato(reader, contract);
                    //Log.d("CONTRATO", datos);

                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        Intent intent = new Intent(VerContratoActivity.this, ResultadosVerContratoActivity.class);
                        intent.putExtra("datos_contrato", datos);
                        startActivity(intent);

                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Error al programar contrato: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        });
    }
}