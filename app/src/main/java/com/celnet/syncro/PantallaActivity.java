package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

public class PantallaActivity extends AppCompatActivity {

    private MaterialCheckBox cbTiempoDesplazamiento;
    private MaterialCheckBox cbModoDesplazamiento;
    private EditText editTiempoDesplazamiento;
    private EditText editModoDesplazamiento;
    private ExtendedFloatingActionButton btnNext;
    private ExtendedFloatingActionButton btnProgram;
    private LinearLayout progressBar;
    private MaterialCheckBox cbOutputLed;
    private AutoCompleteTextView spinnerOutputLed;
    private TextInputLayout layoutOutputLed;

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

        cbTiempoDesplazamiento   = findViewById(R.id.cbTiempoDesplazamiento);
        cbModoDesplazamiento     = findViewById(R.id.cbModoDesplazamiento);
        editTiempoDesplazamiento = findViewById(R.id.editTiempoDesplazamiento);
        editModoDesplazamiento   = findViewById(R.id.editModoDesplazamiento);
        btnNext                  = findViewById(R.id.btnNext);
        btnProgram               = findViewById(R.id.btnProgram);
        progressBar              = findViewById(R.id.progressContainer);
        cbOutputLed       = findViewById(R.id.cbOutputLed);
        spinnerOutputLed  = findViewById(R.id.spinnerOutputLed);
        layoutOutputLed   = findViewById(R.id.layoutOutputLed);

        String[] opcionesOutputLed = getResources().getStringArray(R.array.output_led_array);
        ArrayAdapter<String> adapterOutputLed = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, opcionesOutputLed);
        spinnerOutputLed.setAdapter(adapterOutputLed);
        spinnerOutputLed.setText(opcionesOutputLed[0], false); // por defecto: opción 0

        cbOutputLed.setChecked(true);
        cbOutputLed.setOnCheckedChangeListener((btn, checked) ->
                actualizarHabilitacionCampo(spinnerOutputLed, checked));

        editTiempoDesplazamiento.setInputType(InputType.TYPE_CLASS_NUMBER);
        editModoDesplazamiento.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        editModoDesplazamiento.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(1),
                new InputFilter.AllCaps()
        });

        // Checkboxes de sección: activan/desactivan el campo asociado.
        // Por defecto ambas secciones activas (coincide con el comportamiento previo,
        // que siempre enviaba los dos parámetros).
        cbTiempoDesplazamiento.setChecked(true);
        cbModoDesplazamiento.setChecked(true);

        cbTiempoDesplazamiento.setOnCheckedChangeListener((btn, checked) ->
                actualizarHabilitacionCampo(editTiempoDesplazamiento, checked));
        cbModoDesplazamiento.setOnCheckedChangeListener((btn, checked) ->
                actualizarHabilitacionCampo(editModoDesplazamiento, checked));

        btnNext.setOnClickListener(v -> leerParametros());
        btnProgram.setOnClickListener(v -> programarParametros());
    }

    private void actualizarHabilitacionCampo(EditText campo, boolean habilitado) {
        campo.setEnabled(habilitado);
        campo.setAlpha(habilitado ? 1f : 0.4f);
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

                    editTiempoDesplazamiento.setText(String.valueOf(params.tiempoDesplazamiento));
                    editModoDesplazamiento.setText(params.modoDesplazamiento);
                    cbTiempoDesplazamiento.setChecked(true);
                    cbModoDesplazamiento.setChecked(true);

                    if (params.outputLed >= 0) {
                        String[] opciones = getResources().getStringArray(R.array.output_led_array);
                        for (String opcion : opciones) {
                            if (opcion.startsWith(params.outputLed + ":")) {
                                spinnerOutputLed.setText(opcion, false);
                                break;
                            }
                        }
                        cbOutputLed.setChecked(true);
                    }

                    Intent intent = new Intent(PantallaActivity.this, ResultadosPantallaActivity.class);
                    intent.putExtra(ResultadosPantallaActivity.EXTRA_TIEMPO, params.tiempoDesplazamiento);
                    intent.putExtra(ResultadosPantallaActivity.EXTRA_MODO, params.modoDesplazamiento);
                    intent.putExtra(ResultadosPantallaActivity.EXTRA_OUTPUT_LED, params.outputLed);
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
        boolean enviarTiempo = cbTiempoDesplazamiento.isChecked();
        boolean enviarModo   = cbModoDesplazamiento.isChecked();
        boolean enviarOutputLed = cbOutputLed.isChecked();

        if (!enviarTiempo && !enviarModo && !enviarOutputLed) {
            Toast.makeText(this, "Selecciona al menos un parámetro para programar", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer tiempo = null;
        String modo = null;
        Integer outputLed = null;

        if (enviarTiempo) {
            String tiempoStr = editTiempoDesplazamiento.getText().toString().trim();
            if (tiempoStr.isEmpty()) {
                editTiempoDesplazamiento.setError("El tiempo no puede estar vacío");
                return;
            }
            try {
                tiempo = Integer.parseInt(tiempoStr);
            } catch (NumberFormatException e) {
                editTiempoDesplazamiento.setError("Debe ser un número entero");
                return;
            }
        }

        if (enviarModo) {
            String modoStr = editModoDesplazamiento.getText().toString().trim();
            if (modoStr.isEmpty()) {
                editModoDesplazamiento.setError("El modo no puede estar vacío");
                return;
            }
            if (!modoStr.matches("[A-Z]")) {
                editModoDesplazamiento.setError("Debe ser una sola letra mayúscula");
                return;
            }
            modo = modoStr;
        }

        if (enviarOutputLed) {
            String seleccion = spinnerOutputLed.getText().toString();
            try {
                outputLed = Integer.parseInt(seleccion.split(":")[0].trim());
            } catch (Exception e) {
                Toast.makeText(this, "Selecciona una opción válida de Output Led", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();
        progressBar.setVisibility(View.VISIBLE);
        setBotonesEnabled(false);

        Integer finalTiempo = tiempo;
        String finalModo = modo;
        Integer finalOutputLed = outputLed;

        new Thread(() -> {
            try {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(PantallaActivity.this);
                ScreenParams.ProgramarResult result = ScreenParams.programarParametrosPantalla(
                        res.reader, finalModo, finalTiempo, finalOutputLed);
                conn.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setBotonesEnabled(true);

                    if (result.todoOk()) {
                        Toast.makeText(PantallaActivity.this,
                                "Parámetros programados correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        StringBuilder sb = new StringBuilder("Algunos parámetros no se pudieron programar:\n");
                        if (!result.modoOk) sb.append("- Modo de pantalla\n");
                        if (!result.tiempoOk) sb.append("- Tiempo de desplazamiento\n");
                        if (!result.outputLedOk) {
                            sb.append("- Output Led");
                            if (result.errorOutputLed != null) sb.append(": ").append(result.errorOutputLed);
                        }
                        mostrarError("Programación parcial", new Exception(sb.toString()));
                    }
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

    private void mostrarError(String titulo, Exception e) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(titulo + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .setCancelable(true)
                .show();
    }
}