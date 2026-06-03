package com.example.syncro;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.InstantaneousValues;
import com.example.syncro.models.MeterInfo;
import com.example.syncro.objects.ids.MeterInfoReader;
import com.example.syncro.objects.instantValues.InstantaneousValuesReader;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

public class ValoresInstantaneosActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_valores_instantaneos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        LinearLayout layoutResultados = findViewById(R.id.layoutResultados);
        TextView     tvResultado    = findViewById(R.id.tvResultado);
        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        layoutResultados.setVisibility(View.GONE);


        // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
        new Thread(() -> {
            try {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(ValoresInstantaneosActivity.this);


                // Leer Identificadores
                String datos = InstantaneousValuesReader.leerValores(res.reader);
                conn.close();
                //Log.d("VALORES", datos);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutResultados.setVisibility(View.VISIBLE);
                    tvResultado.setText(datos);
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Toda actualización de UI dentro de runOnUiThread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ValoresInstantaneosActivity.this,
                            "Error de conexión: " + e.getClass().getSimpleName() +
                                    " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();

    }
}