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

        TextView tvSerial = findViewById(R.id.tvSerial);
        TextView tvTipo = findViewById(R.id.tvTipo);
        TextView tvDeciVolts = findViewById(R.id.tvDeciVolts);
        TextView tvVolRelation = findViewById(R.id.tvVolRelation);
        TextView tvInstantVol = findViewById(R.id.tvInstantVol);
        TextView tvVolFase1 = findViewById(R.id.tvVolFase1);
        TextView tvVolFase2 = findViewById(R.id.tvVolFase2);
        TextView tvVolFase3 = findViewById(R.id.tvVolFase3);
        TextView tvDeciAmps = findViewById(R.id.tvDeciAmps);
        TextView tvARelation = findViewById(R.id.tvARelation);
        TextView tvInstantA = findViewById(R.id.tvInstantA);
        TextView tvAFase1 = findViewById(R.id.tvAFase1);
        TextView tvAFase2 = findViewById(R.id.tvAFase2);
        TextView tvAFase3 = findViewById(R.id.tvAFase3);
        TextView tvPotenciaActivaPlus = findViewById(R.id.tvPotenciaActivaPlus);
        TextView tvPotenciaActivaMinus = findViewById(R.id.tvPotenciaActivaMinus);
        TextView tvPotenciaReactivaPlus = findViewById(R.id.tvPotenciaReactivaPlus);
        TextView tvPotenciaReactivaMinus = findViewById(R.id.tvPotenciaReactivaMinus);
        TextView tvFactorPotencia = findViewById(R.id.tvFactorPotencia);
        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);

        // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
        new Thread(() -> {
            try {
                GXDLMSReader reader;
                DLMSConnection conn;

                if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                    conn = new DLMSConnection(config.getBluetoothDeviceName());
                    reader = conn.bluetoothConnnect(ValoresInstantaneosActivity.this);
                } else {
                    conn = new DLMSConnection(config.getIp(), config.getPort());
                    reader = conn.tcpConnect(this);
                }

                // Leer Identificadores
                String datos = InstantaneousValuesReader.leerValores(reader);
                conn.close();
                Log.d("VALORES", datos);
/*
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutResultados.setVisibility(View.VISIBLE);

                    // 🔹 Generales
                    tvSerial.setText("Número serial: " + datos.serial);
                    tvTipo.setText("Fecha: " + datos.fechaHora);

                    // =========================
                    // 🔹 VOLTAJE
                    // =========================
                    tvDeciVolts.setText("Relación V: " + datos.deciVolts);
                    tvVolRelation.setText("Relación V (prim/sec): " + datos.volRelation);

                    tvInstantVol.setText("Voltajes instantáneos:");

                    tvVolFase1.setText("Fase 1: " + datos.vFase1 + " V");
                    tvVolFase2.setText("Fase 2: " + datos.vFase2 + " V");
                    tvVolFase3.setText("Fase 3: " + datos.vFase3 + " V");

                    // =========================
                    // 🔹 CORRIENTE
                    // =========================
                    tvDeciAmps.setText("Relación A: " + datos.deciAmps);
                    tvARelation.setText("Relación A (prim/sec): " + datos.aRelation);

                    tvInstantA.setText("Corrientes instantáneas:");

                    tvAFase1.setText("Fase 1: " + datos.aFase1 + " A");
                    tvAFase2.setText("Fase 2: " + datos.aFase2 + " A");
                    tvAFase3.setText("Fase 3: " + datos.aFase3 + " A");

                    // =========================
                    // 🔹 POTENCIAS
                    // =========================
                    tvPotenciaActivaPlus.setText("P+ (Activa importada): " + datos.pActivaPlus + " kW");
                    tvPotenciaActivaMinus.setText("P- (Activa exportada): " + datos.pActivaMinus + " kW");

                    tvPotenciaReactivaPlus.setText("Q+ (Reactiva inductiva): " + datos.pReactivaPlus + " kvar");
                    tvPotenciaReactivaMinus.setText("Q- (Reactiva capacitiva): " + datos.pReactivaMinus + " kvar");

                    // =========================
                    // 🔹 FACTOR DE POTENCIA
                    // =========================
                    tvFactorPotencia.setText("Factor de potencia: " + datos.factorPotencia);
                });
                */

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