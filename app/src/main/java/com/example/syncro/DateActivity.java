package com.example.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.ControlModeResult;
import com.example.syncro.objects.params.ControlDisconnectMode;
import com.example.syncro.objects.params.DateReader;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

public class DateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_date);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout btnReadDate = findViewById(R.id.btnReadDate);
        LinearLayout btnSyncDate = findViewById(R.id.btnSyncDate);

        btnSyncDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DateActivity.this, SyncDateActivity.class);
                startActivity(intent);
            }
        });

        btnReadDate.setOnClickListener(v -> {
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();
            new Thread(() -> {
                try {
                    GXDLMSReader reader;
                    DLMSConnection conn;

                    if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                        conn = new DLMSConnection(config.getBluetoothDeviceName());
                        reader = conn.bluetoothConnnect(DateActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect(this);
                    }

                    String date = DateReader.readDate(reader);

                    conn.close();

                    runOnUiThread(() -> {
                        Intent intent = new Intent(DateActivity.this, ResultadosDateActivity.class);
                        intent.putExtra("date", date);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        //progressBar.setVisibility(View.GONE);
                        Toast.makeText(DateActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

    }
}