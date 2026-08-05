package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.CurvaFila;
import com.celnet.syncro.models.MeterInfo;
import com.celnet.syncro.objects.ids.MeterInfoReader;
import com.celnet.syncro.objects.loadProfiles.LoadProfileReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

import java.util.ArrayList;

public class IdsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ids);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        LinearLayout layoutResultados = findViewById(R.id.layoutResultados);

        TextView tvSerial = findViewById(R.id.tvSerial);
        TextView tvEquipo = findViewById(R.id.tvEquipo);
        TextView tvTipo = findViewById(R.id.tvTipo);
        TextView tvFirmware = findViewById(R.id.tvFirmware);
        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);

        // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
        new Thread(() -> {
            DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                    ? new DLMSConnection(config.getBluetoothDeviceName())
                    : new DLMSConnection(config.getIp(), config.getPort());

            try {
                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(IdsActivity.this);


                // Leer Identificadores
                MeterInfo datos = MeterInfoReader.leerIdentificadores(res.reader);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutResultados.setVisibility(View.VISIBLE);

                    tvSerial.setText("Número serial: " + res.serialNumber);
                    tvEquipo.setText("Identificador equipo: " + datos.equipo);
                    tvTipo.setText("Identificador tipo: " + datos.tipo);
                    tvFirmware.setText("Versión firmware: " + datos.firmware);

                });

            } catch (Exception e) {
                e.printStackTrace();
                // Toda actualización de UI dentro de runOnUiThread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    new androidx.appcompat.app.AlertDialog.Builder(IdsActivity.this)
                            .setTitle("Error de lectura")
                            .setMessage("No se pudieron leer los identificadores del contador.\n\n"
                                    + e.getMessage())
                            .setPositiveButton("Aceptar", null)
                            .setCancelable(true)
                            .show();
                });
            } finally {
                conn.close();
            }
        }).start();


    }
}