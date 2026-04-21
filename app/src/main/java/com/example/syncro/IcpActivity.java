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
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

public class IcpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_icp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout btnIcpStatus = findViewById(R.id.btnIcpStatus);
        LinearLayout btnIcpExecute = findViewById(R.id.btnIcpExecute);
        LinearLayout btnIcpMode = findViewById(R.id.btnIcpMode);
        //LinearLayout progressBar = findViewById(R.id.progressContainer);

        btnIcpStatus.setOnClickListener(v -> {
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            new Thread(() -> {
                try {
                    GXDLMSReader reader;
                    DLMSConnection conn;

                    if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                        conn = new DLMSConnection(config.getBluetoothDeviceName());
                        reader = conn.bluetoothConnnect(IcpActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect();
                    }

                    ControlModeResult result = ControlDisconnectMode.readControlDisconnectMode(reader, conn.getClient());

                    conn.close();

                    runOnUiThread(() -> {

                        Intent intent = new Intent(IcpActivity.this, ResultadosIcpActivity.class);

                        intent.putExtra("success", result.success);
                        intent.putExtra("estadoInicial", result.estadoInicial);
                        intent.putExtra("estadoFinal", result.estadoFinal);
                        intent.putExtra("mensaje", result.mensaje);

                        startActivity(intent);
                    });
            } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        //progressBar.setVisibility(View.GONE);
                        Toast.makeText(IcpActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

        btnIcpExecute.setOnClickListener(v -> {
            Intent intent = new Intent(IcpActivity.this, IcpExecuteActivity.class);
            startActivity(intent);
        });

        btnIcpMode.setOnClickListener(v -> {

        });
    }
}