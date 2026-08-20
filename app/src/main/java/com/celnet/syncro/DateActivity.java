package com.celnet.syncro;

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

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.objects.params.DateReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

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
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        btnSyncDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DateActivity.this, SyncDateActivity.class);
                startActivity(intent);
            }
        });

        btnReadDate.setOnClickListener(v -> {
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // Bloquear interacción mientras carga
            btnReadDate.setEnabled(false);
            btnSyncDate.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(DateActivity.this);

                    String date = DateReader.readDate(res.reader);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnReadDate.setEnabled(true);
                        btnSyncDate.setEnabled(true);

                        Intent intent = new Intent(DateActivity.this, ResultadosDateActivity.class);
                        intent.putExtra("date", date);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnReadDate.setEnabled(true);
                        btnSyncDate.setEnabled(true);
                        Toast.makeText(DateActivity.this,
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