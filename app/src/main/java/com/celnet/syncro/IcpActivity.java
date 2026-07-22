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
import com.celnet.syncro.models.ControlModeResult;
import com.celnet.syncro.objects.params.ControlDisconnectMode;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

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
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        btnIcpStatus.setOnClickListener(v -> {
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // Bloquear interacción mientras carga
            btnIcpStatus.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(IcpActivity.this);

                    ControlModeResult result = ControlDisconnectMode.readControlDisconnectMode(res.reader, conn.getClient());

                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnIcpStatus.setEnabled(true);

                        Intent intent = new Intent(IcpActivity.this, ResultadosIcpActivity.class);

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
                        btnIcpStatus.setEnabled(true);
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
    }
}