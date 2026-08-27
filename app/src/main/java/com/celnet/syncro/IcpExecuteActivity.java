package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.parameters.ControlModeResult;
import com.celnet.syncro.objects.params.ControlDisconnectMode;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class IcpExecuteActivity extends AppCompatActivity {

    // Ajusta este texto si el valor real en tipo_operaciones_array es distinto
    // (p.ej. "Automatico" sin tilde, "Auto", etc.)
    private static final String TIPO_OPERACION_AUTOMATICO = "Automatico";

    private static final SimpleDateFormat FORMATO_VISUAL =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Calendar con la fecha/hora elegida por el usuario en modo automático.
    // Null mientras no se haya seleccionado nada.
    private Calendar fechaActivacionSeleccionada = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_icp_execute);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextInputLayout layoutFechaActivacion = findViewById(R.id.layoutFechaActivacion);
        TextInputEditText etFechaActivacion = findViewById(R.id.etFechaActivacion);

        // Desplegable Tipo Operación
        AutoCompleteTextView spinnerTipoOperacion = findViewById(R.id.spinnerTipoOperacion);
        String[] tiposOperacion = getResources().getStringArray(R.array.tipo_operaciones_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tiposOperacion);
        spinnerTipoOperacion.setAdapter(adapter);
        if (tiposOperacion.length > 0) {
            spinnerTipoOperacion.setText(tiposOperacion[0], false);
        }

        // Muestra/oculta el campo de fecha/hora según el tipo de operación elegido.
        spinnerTipoOperacion.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = tiposOperacion[position];
            boolean esAutomatico = TIPO_OPERACION_AUTOMATICO.equals(seleccion);
            layoutFechaActivacion.setVisibility(esAutomatico ? View.VISIBLE : View.GONE);
            if (!esAutomatico) {
                fechaActivacionSeleccionada = null;
                etFechaActivacion.setText("");
            }
        });

        // Abre el selector de fecha y, tras elegirla, el selector de hora.
        View.OnClickListener abrirSelectorFechaHora = v -> mostrarSelectorFechaHora(etFechaActivacion);
        etFechaActivacion.setOnClickListener(abrirSelectorFechaHora);
        layoutFechaActivacion.setEndIconOnClickListener(v -> mostrarSelectorFechaHora(etFechaActivacion));

        // Desplegable Operación
        AutoCompleteTextView spinnerOperacion = findViewById(R.id.spinnerOperacion);
        String[] operaciones = getResources().getStringArray(R.array.operaciones_array);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, operaciones);
        spinnerOperacion.setAdapter(adapter2);
        if (operaciones.length > 0) {
            spinnerOperacion.setText(operaciones[0], false);
        }

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String tipoOperacion = spinnerTipoOperacion.getText().toString();
            String operacion = spinnerOperacion.getText().toString();
            boolean esAutomatico = TIPO_OPERACION_AUTOMATICO.equals(tipoOperacion);
            boolean connect = operacion.equals("Connect");

            if (esAutomatico && fechaActivacionSeleccionada == null) {
                Toast.makeText(this, "Selecciona la fecha y hora de activación.", Toast.LENGTH_SHORT).show();
                return;
            }

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            Toast.makeText(IcpExecuteActivity.this,
                    "Conexión: " + config.getType() + "\n",
                    Toast.LENGTH_LONG).show();

            // Bloquear interacción mientras carga
            btnNext.setEnabled(false);
            spinnerTipoOperacion.setEnabled(false);
            spinnerOperacion.setEnabled(false);
            etFechaActivacion.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(IcpExecuteActivity.this);

                    ControlModeResult result;

                    if (esAutomatico) {
                        result = ControlDisconnectMode.programarDesconexionAutomatica(
                                res.reader, connect, fechaActivacionSeleccionada);
                    } else {
                        result = ControlDisconnectMode.setControlDisconnectMode(
                                res.reader, conn.getClient(), connect);
                    }

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerTipoOperacion.setEnabled(true);
                        spinnerOperacion.setEnabled(true);
                        etFechaActivacion.setEnabled(true);

                        Intent intent = new Intent(IcpExecuteActivity.this, ResultadosIcpActivity.class);
                        intent.putExtra("success", result.success);
                        intent.putExtra("estadoInicial", result.estadoInicial);
                        intent.putExtra("estadoFinal", result.estadoFinal);
                        intent.putExtra("modoControl", result.modoControl);
                        intent.putExtra("mensaje", result.mensaje);

                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        spinnerTipoOperacion.setEnabled(true);
                        spinnerOperacion.setEnabled(true);
                        etFechaActivacion.setEnabled(true);
                        Toast.makeText(IcpExecuteActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } finally {
                    conn.close();
                }
            }).start();

        });
    }

    /**
     * Encadena DatePickerDialog + TimePickerDialog. Al confirmar ambos,
     * guarda el resultado en fechaActivacionSeleccionada y lo refleja
     * en el campo de texto.
     */
    private void mostrarSelectorFechaHora(TextInputEditText etFechaActivacion) {
        Calendar base = (fechaActivacionSeleccionada != null)
                ? (Calendar) fechaActivacionSeleccionada.clone()
                : Calendar.getInstance();

        new DatePickerDialog(this, (viewFecha, year, month, dayOfMonth) -> {
            Calendar conFecha = (Calendar) base.clone();
            conFecha.set(Calendar.YEAR, year);
            conFecha.set(Calendar.MONTH, month);
            conFecha.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (viewHora, hourOfDay, minute) -> {
                Calendar conFechaYHora = (Calendar) conFecha.clone();
                conFechaYHora.set(Calendar.HOUR_OF_DAY, hourOfDay);
                conFechaYHora.set(Calendar.MINUTE, minute);
                conFechaYHora.set(Calendar.SECOND, 0);
                conFechaYHora.set(Calendar.MILLISECOND, 0);

                fechaActivacionSeleccionada = conFechaYHora;
                etFechaActivacion.setText(FORMATO_VISUAL.format(conFechaYHora.getTime()));

            }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), true).show();

        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show();
    }
}