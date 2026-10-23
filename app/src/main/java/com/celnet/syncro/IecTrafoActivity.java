package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.params.Iec870TrafoRatio;
import com.celnet.syncro.objects.iec870.params.Iec870TrafoReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/**
 * Relación de transformación por IEC-870: tensión (deciVolts) e intensidad
 * (deciAmps), primario / secundario.
 * - "Leer": lee los cuatro valores del contador y los vuelca en el formulario.
 * - "Programar": valida el formulario. La escritura en el contador aún no está
 *   implementada (falta el log del software funcional).
 */
public class IecTrafoActivity extends BaseActivity {

    private static final long MAX_UINT32 = 4294967295L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_trafo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText editVPrim = findViewById(R.id.editVoltagePrimary);
        EditText editVSec = findViewById(R.id.editVoltageSecondary);
        EditText editIPrim = findViewById(R.id.editCurrentPrimary);
        EditText editISec = findViewById(R.id.editCurrentSecondary);
        ExtendedFloatingActionButton btnRead = findViewById(R.id.btnRead);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        // ---------------- Leer ----------------
        btnRead.setOnClickListener(v -> {
            bloquear(true, btnRead, btnNext, progressBar);

            new Thread(() -> {
                try {
                    Iec870TrafoRatio rt = Iec870TrafoReader.leer(IecTrafoActivity.this, config);

                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, progressBar);
                        editVPrim.setText(String.valueOf(rt.tensionPrimariaDv));
                        editVSec.setText(String.valueOf(rt.tensionSecundariaDv));
                        editIPrim.setText(String.valueOf(rt.corrientePrimariaDa));
                        editISec.setText(String.valueOf(rt.corrienteSecundariaDa));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, progressBar);
                        mostrarError("No se pudo leer la relación de transformación.", e);
                    });
                }
            }).start();
        });

        // ---------------- Programar ----------------
        btnNext.setOnClickListener(v -> {
            Long vPrim = leerValor(editVPrim);
            Long vSec = leerValor(editVSec);
            Long iPrim = leerValor(editIPrim);
            Long iSec = leerValor(editISec);
            if (vPrim == null || vSec == null || iPrim == null || iSec == null) {
                return;
            }

            // TODO: escritura por IEC-870 pendiente del log del software funcional.
            new AlertDialog.Builder(this)
                    .setTitle("Programación no disponible")
                    .setMessage("La programación de la relación de transformación por IEC-870 "
                            + "todavía no está implementada.\n\nValores del formulario:\n"
                            + "Tensión: " + vPrim + " / " + vSec + " dV\n"
                            + "Corriente: " + iPrim + " / " + iSec + " dA")
                    .setPositiveButton("Aceptar", null)
                    .show();
        });
    }

    /** Valor entero positivo (uint32) del campo; null y error en el campo si no es válido. */
    private Long leerValor(EditText campo) {
        String texto = campo.getText().toString().trim();
        if (texto.isEmpty()) {
            campo.setError("Campo obligatorio");
            campo.requestFocus();
            return null;
        }
        long valor;
        try {
            valor = Long.parseLong(texto);
        } catch (NumberFormatException e) {
            campo.setError("Número inválido");
            campo.requestFocus();
            return null;
        }
        if (valor <= 0 || valor > MAX_UINT32) {
            campo.setError("Debe estar entre 1 y " + MAX_UINT32);
            campo.requestFocus();
            return null;
        }
        return valor;
    }

    private void bloquear(boolean leyendo, ExtendedFloatingActionButton btnRead,
                          ExtendedFloatingActionButton btnNext, LinearLayout progress) {
        btnRead.setEnabled(!leyendo);
        btnNext.setEnabled(!leyendo);
        progress.setVisibility(leyendo ? View.VISIBLE : View.GONE);
    }

    private void mostrarError(String mensaje, Exception e) {
        new AlertDialog.Builder(this)
                .setTitle("Error (IEC-870)")
                .setMessage(mensaje + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .show();
    }
}