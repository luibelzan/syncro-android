package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.objects.params.ScreenParams;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

public class PantallaActivity extends AppCompatActivity {

    private EditText editTiempoDesplazamiento;
    private EditText editModoDesplazamiento;
    private ImageButton btnNext;
    private ImageButton btnProgram;
    private LinearLayout progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pantalla);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTiempoDesplazamiento = findViewById(R.id.editTiempoDesplazamiento);
        editModoDesplazamiento   = findViewById(R.id.editModoDesplazamiento);
        btnNext                  = findViewById(R.id.btnNext);
        btnProgram               = findViewById(R.id.btnProgram);
        progressBar              = findViewById(R.id.progressContainer);

        // Solo permite números en tiempo
        editTiempoDesplazamiento.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Fuerza mayúsculas y limita a 1 carácter en modo
        editModoDesplazamiento.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        editModoDesplazamiento.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(1),
                new InputFilter.AllCaps()
        });

        btnNext.setOnClickListener(v -> leerParametros());
        btnProgram.setOnClickListener(v -> programarParametros());

    }

    private void setBotonesEnabled(boolean enabled) {
        btnNext.setEnabled(enabled);
        btnProgram.setEnabled(enabled);
    }

    private void leerParametros() {
        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        setBotonesEnabled(false);

        new Thread(() -> {
            try {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(PantallaActivity.this);
                ScreenParams.ScreenParamsResult params = ScreenParams.leerParametrosPantalla(res.reader);
                conn.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);

                    Intent intent = new Intent(PantallaActivity.this, ResultadosPantallaActivity.class);
                    intent.putExtra(ResultadosPantallaActivity.EXTRA_TIEMPO, params.tiempoDesplazamiento);
                    intent.putExtra(ResultadosPantallaActivity.EXTRA_MODO, params.modoDesplazamiento);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);
                    Toast.makeText(PantallaActivity.this,
                            "Error de conexión: " + e.getClass().getSimpleName() +
                                    " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void programarParametros() {
        String tiempoStr = editTiempoDesplazamiento.getText().toString().trim();
        String modoStr   = editModoDesplazamiento.getText().toString().trim();

        // Validaciones antes de conectar
        if (tiempoStr.isEmpty()) {
            editTiempoDesplazamiento.setError("El tiempo no puede estar vacío");
            return;
        }
        if (modoStr.isEmpty()) {
            editModoDesplazamiento.setError("El modo no puede estar vacío");
            return;
        }
        int tiempo;
        try {
            tiempo = Integer.parseInt(tiempoStr);
        } catch (NumberFormatException e) {
            editTiempoDesplazamiento.setError("Debe ser un número entero");
            return;
        }
        if (!modoStr.matches("[A-Z]")) {
            editModoDesplazamiento.setError("Debe ser una sola letra mayúscula");
            return;
        }

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();
        progressBar.setVisibility(View.VISIBLE);
        setBotonesEnabled(false);

        new Thread(() -> {
            try {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(PantallaActivity.this);
                boolean ok = ScreenParams.programarParametrosPantalla(res.reader, modoStr, tiempo);
                conn.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);
                    Toast.makeText(PantallaActivity.this,
                            ok ? "Parámetros programados correctamente" : "Error al programar",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);
                    Toast.makeText(PantallaActivity.this,
                            "Error de conexión: " + e.getClass().getSimpleName() +
                                    " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}