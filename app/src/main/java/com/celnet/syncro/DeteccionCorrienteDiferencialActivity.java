package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.parameters.DifferentialCurrentDetectionInfo;
import com.celnet.syncro.objects.params.DifferentialCurrentDetectionReader;
import com.celnet.syncro.objects.params.DifferentialCurrentDetectionWriter;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.celnet.syncro.utils.AppLogger;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class DeteccionCorrienteDiferencialActivity extends BaseActivity {

    private TextInputEditText etUmbralVariacion;
    private TextInputEditText etUmbralTiempo;
    private TextInputEditText etUmbralCorrienteMinima;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deteccion_corriente_diferencial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUmbralVariacion = findViewById(R.id.etUmbralVariacion);
        etUmbralTiempo = findViewById(R.id.etUmbralTiempo);
        etUmbralCorrienteMinima = findViewById(R.id.etUmbralCorrienteMinima);

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnLeerActual = findViewById(R.id.btnLeerActual);
        ExtendedFloatingActionButton btnProgramar = findViewById(R.id.btnProgramar);

        btnLeerActual.setOnClickListener(v -> leerActual(progressBar, btnLeerActual, btnProgramar));
        btnProgramar.setOnClickListener(v -> programar(progressBar, btnLeerActual, btnProgramar));
    }

    private void leerActual(LinearLayout progressBar,
                            ExtendedFloatingActionButton btnLeerActual,
                            ExtendedFloatingActionButton btnProgramar) {

        progressBar.setVisibility(View.VISIBLE);
        btnLeerActual.setEnabled(false);
        btnProgramar.setEnabled(false);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        new Thread(() -> {
            DLMSConnection conn = null;
            try {
                conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(this);
                DifferentialCurrentDetectionInfo datos =
                        DifferentialCurrentDetectionReader.leerDifferentialCurrentDetection(res.reader);

                AppLogger.i("DifferentialCurrent", "Resultado lectura:\n" + datos.toString());

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);

                    // El formulario de esta pantalla es para PROGRAMAR, no un
                    // espejo de la última lectura: no se rellenan los inputs.
                    // El resultado de la lectura vive únicamente en su propia
                    // pantalla de solo lectura.
                    Intent intent = new Intent(DeteccionCorrienteDiferencialActivity.this,
                            ResultadosDeteccionCorrienteDiferencialActivity.class);
                    intent.putExtra(ResultadosDeteccionCorrienteDiferencialActivity.EXTRA_INFO, datos);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);
                    mostrarError("No se pudo leer Differential Current Detection.", e);
                });
            } finally {
                if (conn != null) conn.close();
            }
        }).start();
    }

    private void programar(LinearLayout progressBar,
                           ExtendedFloatingActionButton btnLeerActual,
                           ExtendedFloatingActionButton btnProgramar) {

        Double umbralVariacion = parseDoubleSeguro(etUmbralVariacion.getText().toString());
        Integer umbralTiempo = parseIntSeguro(etUmbralTiempo.getText().toString());
        Double umbralCorrienteMinima = parseDoubleSeguro(etUmbralCorrienteMinima.getText().toString());

        if (umbralVariacion == null || umbralTiempo == null || umbralCorrienteMinima == null) {
            Toast.makeText(this, "Revisa que los tres campos tengan valores numéricos válidos.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        btnLeerActual.setEnabled(false);
        btnProgramar.setEnabled(false);

        new Thread(() -> {
            DLMSConnection conn = null;
            try {
                conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(this);

                DifferentialCurrentDetectionWriter.programarDifferentialCurrentDetection(
                        res.reader, umbralVariacion, umbralTiempo, umbralCorrienteMinima);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);
                    Toast.makeText(this, "Differential Current Detection programado correctamente",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);
                    mostrarError("No se pudo programar Differential Current Detection.", e);
                });
            } finally {
                if (conn != null) conn.close();
            }
        }).start();
    }

    private Double parseDoubleSeguro(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntSeguro(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarError(String titulo, Exception e) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(titulo + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .setCancelable(true)
                .show();
    }
}