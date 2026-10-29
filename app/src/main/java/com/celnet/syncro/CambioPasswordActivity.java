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

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.objects.params.PasswordParams;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/**
 * Cambio de contraseñas (solo contadores DLMS): lectura, mngmt/Local security y firmware. Cada una se
 * programa solo si su casilla está marcada. Implementadas: lectura, gestión y firmware (esta última en sesión aparte).
 */
public class CambioPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cambio_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialCheckBox checkLectura = findViewById(R.id.checkReadPassword);
        MaterialCheckBox checkMgmt = findViewById(R.id.checkMgmtPassword);
        MaterialCheckBox checkFirmware = findViewById(R.id.checkFirmwarePassword);
        EditText editLectura = findViewById(R.id.editReadPassword);
        EditText editMgmt = findViewById(R.id.editMgmtPassword);
        EditText editFirmware = findViewById(R.id.editFirmwarePassword);
        EditText editFirmwareActual = findViewById(R.id.editFirmwarePasswordActual);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        checkLectura.setOnCheckedChangeListener((b, marcado) -> editLectura.setEnabled(marcado));
        checkMgmt.setOnCheckedChangeListener((b, marcado) -> editMgmt.setEnabled(marcado));
        checkFirmware.setOnCheckedChangeListener((b, marcado) -> {
            editFirmware.setEnabled(marcado);
            editFirmwareActual.setEnabled(marcado);
        });

        btnNext.setOnClickListener(v -> {
            if (!checkLectura.isChecked() && !checkMgmt.isChecked() && !checkFirmware.isChecked()) {
                Toast.makeText(this, "Marque al menos una contraseña a programar",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (checkLectura.isChecked() && !valida(editLectura)) {
                return;
            }
            if (checkMgmt.isChecked() && !valida(editMgmt)) {
                return;
            }

            if (checkFirmware.isChecked() && (!valida(editFirmware) || !valida(editFirmwareActual))) {
                return;
            }

            boolean progFirmware = checkFirmware.isChecked();
            String passwordFirmware = editFirmware.getText().toString().trim();
            String passwordFirmwareActual = editFirmwareActual.getText().toString().trim();
            boolean progLectura = checkLectura.isChecked();
            boolean progMgmt = checkMgmt.isChecked();
            String password = editLectura.getText().toString().trim();
            String passwordMgmt = editMgmt.getText().toString().trim();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            btnNext.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = nuevaConexion(config);
                DLMSConnection connFw = null;

                try {
                    PasswordParams.ProgramarResult resultado = new PasswordParams.ProgramarResult();

                    if (progLectura || progMgmt) {
                        DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(CambioPasswordActivity.this);
                        if (progLectura) {
                            resultado = PasswordParams.programarPasswordLectura(res.reader, password);
                        }
                        // La de gestión va la última de esta sesión: tras cambiarla no se reutiliza.
                        if (progMgmt) {
                            resultado = PasswordParams.programarPasswordMgmt(res.reader, passwordMgmt, resultado);
                        }
                    } else {
                        // Solo firmware: una conexión previa fija el addressSize y comprueba el contador.
                        conn.connectWithAutoDetect(CambioPasswordActivity.this);
                    }

                    if (progFirmware) {
                        // Como el software funcional: se cierra la sesión de gestión y se abre la de firmware.
                        conn.close();
                        connFw = nuevaConexion(config);
                        try {
                            DLMSConnection.ConnectionResult resFw =
                                    connFw.connectFirmwareSession(CambioPasswordActivity.this, passwordFirmwareActual);
                            resultado = PasswordParams.programarPasswordFirmware(resFw.reader, passwordFirmware, resultado);
                        } catch (Exception e) {
                            resultado.firmwareOk = false;
                            resultado.errorFirmware = "No se pudo abrir la sesión de firmware: " + e.getMessage();
                        }
                    }
                    final PasswordParams.ProgramarResult fin = resultado;

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        if (fin.todoOk()) {
                            Toast.makeText(CambioPasswordActivity.this,
                                    "Contraseña(s) programada(s) correctamente",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            StringBuilder msg = new StringBuilder();
                            if (!fin.lecturaOk) {
                                msg.append("Lectura: ").append(fin.errorLectura).append('\n');
                            }
                            if (!fin.mgmtOk) {
                                msg.append("Gestión: ").append(fin.errorMgmt).append('\n');
                            }
                            if (!fin.firmwareOk) {
                                msg.append("Firmware: ").append(fin.errorFirmware);
                            }
                            mostrarError("No se pudo programar alguna contraseña.", msg.toString());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        mostrarError("No se pudo conectar con el contador.", e.getMessage());
                    });
                } finally {
                    conn.close();
                    if (connFw != null) {
                        connFw.close();
                    }
                }
            }).start();
        });
    }

    private DLMSConnection nuevaConexion(ConnectionConfig config) {
        return (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                ? new DLMSConnection(config.getBluetoothDeviceName())
                : new DLMSConnection(config.getIp(), config.getPort());
    }

    /** Contraseña de 1 a 8 cifras; marca el error en el campo si no lo es. */
    private boolean valida(EditText campo) {
        String texto = campo.getText().toString().trim();
        if (!texto.matches("\\d{1,8}")) {
            campo.setError("Debe tener de 1 a 8 cifras");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    private void mostrarError(String mensaje, String detalle) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(mensaje + (detalle != null ? "\n\n" + detalle : ""))
                .setPositiveButton("Aceptar", null)
                .show();
    }
}