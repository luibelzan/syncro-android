package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.objects.params.DateReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SyncDateActivity extends BaseActivity {

    // Formato solo para mostrar en pantalla. La fecha real que se envía a
    // DateReader.syncClock es el objeto Date que va guardado en fechaSeleccionada,
    // así que este patrón no afecta a la sincronización en ningún caso.
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Fecha/hora elegida con los pickers (o "ahora" mientras el checkbox esté desmarcado)
    private Date fechaSeleccionada = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sync_date);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText editDateTime = findViewById(R.id.editDateTime);
        EditText editTimezone = findViewById(R.id.editTimezone);
        MaterialCheckBox checkBoxManual = findViewById(R.id.checkBoxManual);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        // Valores por defecto
        editDateTime.setText(SDF.format(fechaSeleccionada));
        editTimezone.setText("120");

        // Estado inicial (bloqueados)
        setEditable(false, editDateTime, editTimezone);

        // Al marcar el checkbox se habilita poder abrir los pickers y editar el timezone
        checkBoxManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setEditable(isChecked, editDateTime, editTimezone);
            if (!isChecked) {
                // Al desmarcar, se vuelve a "ahora" (comportamiento original)
                fechaSeleccionada = new Date();
                editDateTime.setText(SDF.format(fechaSeleccionada));
            }
        });

        // Abre DatePicker -> TimePicker encadenados y actualiza fechaSeleccionada
        editDateTime.setOnClickListener(v -> mostrarDateTimePicker(editDateTime));

        btnNext.setOnClickListener(v -> {
            String timezoneStr = editTimezone.getText().toString().trim();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            int utcOffsetMinutes;
            try {
                utcOffsetMinutes = Integer.parseInt(timezoneStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "La zona horaria debe ser un número entero (ej: 120)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bloquear interacción mientras se ejecuta
            btnNext.setEnabled(false);
            checkBoxManual.setEnabled(false);

            final Date finalDateTime = fechaSeleccionada;
            final int finalOffset = utcOffsetMinutes;

            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(SyncDateActivity.this);

                    GXDLMSSecureClient2 client = conn.getClient();

                    DateReader.syncClock(res.reader, client, finalOffset, finalDateTime);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        checkBoxManual.setEnabled(true);

                        Toast.makeText(SyncDateActivity.this,
                                "Fecha y hora sincronizada correctamente",
                                Toast.LENGTH_LONG).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        checkBoxManual.setEnabled(true);
                        Toast.makeText(SyncDateActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } finally {
                    conn.close();
                }
            }).start();
        });
    }

    private void mostrarDateTimePicker(EditText editDateTime) {
        Calendar calInicial = Calendar.getInstance();
        calInicial.setTime(fechaSeleccionada);

        new DatePickerDialog(
                this,
                (dateView, year, month, day) -> {

                    // Tras elegir la fecha, encadenamos el selector de hora
                    new TimePickerDialog(
                            this,
                            (timeView, hour, minute) -> {

                                Calendar cal = Calendar.getInstance();
                                cal.set(year, month, day, hour, minute, 0);
                                cal.set(Calendar.MILLISECOND, 0);

                                fechaSeleccionada = cal.getTime();
                                editDateTime.setText(SDF.format(fechaSeleccionada));
                            },
                            calInicial.get(Calendar.HOUR_OF_DAY),
                            calInicial.get(Calendar.MINUTE),
                            true
                    ).show();

                },
                calInicial.get(Calendar.YEAR),
                calInicial.get(Calendar.MONTH),
                calInicial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setEditable(boolean enabled, EditText... fields) {
        for (EditText et : fields) {
            et.setEnabled(enabled);
        }
    }
}