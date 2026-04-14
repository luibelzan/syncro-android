package com.example.syncro;

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

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CurvaFila;
import com.example.syncro.objects.loadProfiles.LoadProfileReader;
import com.example.syncro.objects.params.ControlDisconnectMode;
import com.example.syncro.objects.params.SerialNumberReader;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

import java.util.ArrayList;

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

            // Mostrar valores en Toast de depuración
            Toast.makeText(IcpExecuteActivity.this,
                    "Conexión: " + config.getType() + "\n",
                    Toast.LENGTH_LONG).show();

            progressBar.setVisibility(View.VISIBLE);

            // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
            new Thread(() -> {
                try {
                    GXDLMSReader reader;
                    DLMSConnection conn;

                    if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                        conn = new DLMSConnection(config.getBluetoothDeviceName());
                        reader = conn.bluetoothConnnect(IcpExecuteActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect();
                    }

                    // Ejecutar operacion
                    if(operacion.equals("Connect")) {
                        ControlDisconnectMode.setControlDisconnectMode(reader, conn.getClient(), true);
                    } else {
                        ControlDisconnectMode.setControlDisconnectMode(reader, conn.getClient(), false);
                    }
                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        Intent intent = new Intent(IcpExecuteActivity.this, ResultadosCurvasActivity.class);
                        startActivity(intent);

                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(IcpExecuteActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        });
    }
}