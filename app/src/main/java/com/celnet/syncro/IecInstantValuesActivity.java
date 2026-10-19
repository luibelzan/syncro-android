package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.instantValues.Iec870InstantValues;
import com.celnet.syncro.objects.iec870.instantValues.Iec870InstantValuesReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

/**
 * Valores instantáneos por IEC-870. No hay nada que elegir: al entrar se lanza la
 * lectura (con la pantalla de progreso) y, al terminar, se pasa directamente a
 * {@link ResultadosIecValoresInstantaneosActivity}. Esta pantalla se cierra para
 * que "atrás" desde los resultados vuelva al menú de lecturas.
 */
public class IecInstantValuesActivity extends BaseActivity {

    private volatile boolean leyendo = false;
    private boolean lecturaLanzada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_instant_values);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mientras se lee no se puede salir: la conexión Bluetooth está en uso.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (leyendo) {
                    Toast.makeText(IecInstantValuesActivity.this,
                            "Lectura en curso, espere un momento…", Toast.LENGTH_SHORT).show();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // En el AndroidManifest esta actividad lleva
        // android:configChanges="orientation|screenSize|keyboardHidden" para que
        // girar la pantalla no la recree y relance la lectura.
        lanzarLectura();
    }

    private void lanzarLectura() {
        if (lecturaLanzada) return;
        lecturaLanzada = true;
        leyendo = true;

        View progress = findViewById(R.id.progressContainer);
        progress.setVisibility(View.VISIBLE);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        new Thread(() -> {
            try {
                Iec870InstantValues valores = Iec870InstantValuesReader
                        .leerValoresInstantaneos(IecInstantValuesActivity.this, config);

                runOnUiThread(() -> {
                    leyendo = false;
                    if (isFinishing() || isDestroyed()) return;
                    Intent intent = new Intent(IecInstantValuesActivity.this,
                            ResultadosIecValoresInstantaneosActivity.class);
                    intent.putExtra(ResultadosIecValoresInstantaneosActivity.EXTRA_VALORES, valores);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    leyendo = false;
                    if (isFinishing() || isDestroyed()) return;
                    progress.setVisibility(View.GONE);
                    new AlertDialog.Builder(IecInstantValuesActivity.this)
                            .setTitle("Error de lectura (IEC-870)")
                            .setMessage("No se pudieron leer los valores instantáneos.\n\n"
                                    + e.getMessage())
                            .setPositiveButton("Aceptar", (d, w) -> finish())
                            .setOnCancelListener(d -> finish())
                            .show();
                });
            }
        }).start();
    }
}