package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.objects.params.TrafoParams;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/** Lectura y programación de la relación de transformación (TI / TT). */
public class TrafoActivity extends BaseActivity {

    private static final int VACIO = -1;
    private static final int INVALIDO = -2;

    private EditText editVoltagePrimary;
    private EditText editVoltageSecondary;
    private EditText editCurrentPrimary;
    private EditText editCurrentSecondary;
    private ExtendedFloatingActionButton btnRead;
    private ExtendedFloatingActionButton btnNext;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trafo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editVoltagePrimary   = findViewById(R.id.editVoltagePrimary);
        editVoltageSecondary = findViewById(R.id.editVoltageSecondary);
        editCurrentPrimary   = findViewById(R.id.editCurrentPrimary);
        editCurrentSecondary = findViewById(R.id.editCurrentSecondary);
        btnRead              = findViewById(R.id.btnRead);
        btnNext              = findViewById(R.id.btnNext);
        progressBar          = findViewById(R.id.progressContainer);

        btnRead.setOnClickListener(v -> leerRelaciones());
        btnNext.setOnClickListener(v -> programarRelaciones());
    }

    private void setBotonesEnabled(boolean enabled) {
        btnRead.setEnabled(enabled);
        btnNext.setEnabled(enabled);
    }

    private DLMSConnection crearConexion(ConnectionConfig config) {
        return (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                ? new DLMSConnection(config.getBluetoothDeviceName())
                : new DLMSConnection(config.getIp(), config.getPort());
    }

    // ------------------------------------------------------------------ leer

    private void leerRelaciones() {
        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        setBotonesEnabled(false);

        new Thread(() -> {
            DLMSConnection conn = null;
            try {
                conn = crearConexion(config);
                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(TrafoActivity.this);
                TrafoParams.RelacionesResult r = TrafoParams.leerRelaciones(res.reader);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);

                    if (!r.algunValor()) {
                        Toast.makeText(this, "No se pudo leer la relación de transformación",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    ponerValor(editVoltagePrimary, r.tensionPrimario);
                    ponerValor(editVoltageSecondary, r.tensionSecundario);
                    ponerValor(editCurrentPrimary, r.corrientePrimario);
                    ponerValor(editCurrentSecondary, r.corrienteSecundario);
                    Toast.makeText(this, "Relación de transformación leída",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mostrarErrorConexion(e);
            } finally {
                cerrar(conn);
            }
        }).start();
    }

    private void ponerValor(EditText campo, int valor) {
        if (valor >= 0) {
            campo.setText(String.valueOf(valor));
            campo.setError(null);
        }
    }

    // -------------------------------------------------------------- programar

    private void programarRelaciones() {
        int vp = leerCampo(editVoltagePrimary);
        int vs = leerCampo(editVoltageSecondary);
        int cp = leerCampo(editCurrentPrimary);
        int cs = leerCampo(editCurrentSecondary);
        if (vp == INVALIDO || vs == INVALIDO || cp == INVALIDO || cs == INVALIDO) return;

        // Cada relación se programa solo si se rellenan sus dos valores.
        boolean tension = vp != VACIO || vs != VACIO;
        boolean corriente = cp != VACIO || cs != VACIO;

        if (!tension && !corriente) {
            Toast.makeText(this, "Introduce al menos una relación (tensión o corriente)",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (tension && !completa(vp, vs, editVoltagePrimary, editVoltageSecondary)) return;
        if (corriente && !completa(cp, cs, editCurrentPrimary, editCurrentSecondary)) return;

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();
        final Integer fVp = tension ? vp : null;
        final Integer fVs = tension ? vs : null;
        final Integer fCp = corriente ? cp : null;
        final Integer fCs = corriente ? cs : null;

        progressBar.setVisibility(View.VISIBLE);
        setBotonesEnabled(false);

        new Thread(() -> {
            DLMSConnection conn = null;
            try {
                conn = crearConexion(config);
                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(TrafoActivity.this);
                TrafoParams.ProgramarResult result =
                        TrafoParams.programarRelaciones(res.reader, fVp, fVs, fCp, fCs);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);

                    if (result.todoOk()) {
                        Toast.makeText(this, "Relación de transformación programada correctamente",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        StringBuilder sb = new StringBuilder("No se pudo programar:\n");
                        if (!result.corrienteOk) {
                            sb.append("- Relación de corriente");
                            if (result.errorCorriente != null) sb.append(": ").append(result.errorCorriente);
                            sb.append("\n");
                        }
                        if (!result.tensionOk) {
                            sb.append("- Relación de tensión");
                            if (result.errorTension != null) sb.append(": ").append(result.errorTension);
                            sb.append("\n");
                        }
                        sb.append("\nRevisa que el contador esté en modo de escritura.");
                        mostrarError(sb.toString());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mostrarErrorConexion(e);
            } finally {
                cerrar(conn);
            }
        }).start();
    }

    /** VACIO si está en blanco, INVALIDO (y error en el campo) si no es 1..65535, o el valor. */
    private int leerCampo(EditText campo) {
        String s = campo.getText().toString().trim();
        if (s.isEmpty()) {
            campo.setError(null);
            return VACIO;
        }
        try {
            long v = Long.parseLong(s);
            if (v < 1 || v > TrafoParams.MAX_VALOR) {
                campo.setError("Debe estar entre 1 y " + TrafoParams.MAX_VALOR);
                return INVALIDO;
            }
            campo.setError(null);
            return (int) v;
        } catch (NumberFormatException e) {
            campo.setError("Debe ser un número entero");
            return INVALIDO;
        }
    }

    private boolean completa(int primario, int secundario, EditText ePrimario, EditText eSecundario) {
        if (primario == VACIO) {
            ePrimario.setError("Rellena también este valor");
            return false;
        }
        if (secundario == VACIO) {
            eSecundario.setError("Rellena también este valor");
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- común

    private void cerrar(DLMSConnection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void mostrarErrorConexion(Exception e) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            setBotonesEnabled(true);
            Toast.makeText(this,
                    "Error de conexión: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void mostrarError(String mensaje) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null)
                .setCancelable(true)
                .show();
    }
}