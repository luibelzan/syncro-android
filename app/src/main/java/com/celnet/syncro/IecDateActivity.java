package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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

import com.celnet.syncro.objects.iec870.params.Iec870DateReader;
import com.celnet.syncro.models.iec870.params.Iec870DateTime;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fecha y hora del contador por IEC-870.
 * - "Leer": lee la fecha del contador y la muestra en {@link ResultadosDateActivity}
 *   (con la del sistema y la diferencia); también lee las fechas oficiales de cambio de
 *   hora del contador y las pasa a esa pantalla para mostrarlas.
 * - "Programar": escribe en el contador la fecha del formulario (la hora actual del
 *   móvil, o la elegida con los pickers si se marca la configuración manual).
 */
public class IecDateActivity extends BaseActivity {

    // Solo para mostrar en pantalla
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Fecha elegida con los pickers (solo se usa con el checkbox marcado)
    private Date fechaSeleccionada = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_date);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText editDateTime = findViewById(R.id.editDateTime);
        EditText editTimezone = findViewById(R.id.editTimezone);
        MaterialCheckBox checkBoxManual = findViewById(R.id.checkBoxManual);
        ExtendedFloatingActionButton btnRead = findViewById(R.id.btnRead);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        editDateTime.setText(SDF.format(fechaSeleccionada));
        editTimezone.setText(String.valueOf(offsetSistemaMinutos(fechaSeleccionada)));
        editDateTime.setEnabled(false);
        editTimezone.setEnabled(false);

        checkBoxManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editDateTime.setEnabled(isChecked);
            editTimezone.setEnabled(isChecked);
            if (!isChecked) {
                // Sin modo manual: fecha y zona horaria del sistema
                fechaSeleccionada = new Date();
                editDateTime.setText(SDF.format(fechaSeleccionada));
                editTimezone.setText(String.valueOf(offsetSistemaMinutos(fechaSeleccionada)));
            }
        });

        editDateTime.setOnClickListener(v -> mostrarDateTimePicker(editDateTime));

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        // ---------------- Leer la fecha del contador ----------------
        btnRead.setOnClickListener(v -> {
            bloquear(true, btnRead, btnNext, checkBoxManual, progressBar);

            new Thread(() -> {
                try {
                    Iec870DateReader.Resultado res =
                            Iec870DateReader.leerFecha(IecDateActivity.this, config);

                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, checkBoxManual, progressBar);
                        Intent intent = new Intent(IecDateActivity.this, ResultadosIecDateActivity.class);
                        intent.putExtra("date", res.fecha.toText());
                        intent.putExtra("cambio_verano", res.cambioHora.inicioVerano.toShortText());
                        intent.putExtra("cambio_invierno", res.cambioHora.inicioInvierno.toShortText());
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, checkBoxManual, progressBar);
                        mostrarError("No se pudo leer la fecha y hora del contador.", e);
                    });
                }
            }).start();
        });

        // ---------------- Programar la fecha ----------------
        btnNext.setOnClickListener(v -> {
            // Sin modo manual: la hora del móvil en este momento (no la de cuando se abrió la pantalla)
            boolean manual = checkBoxManual.isChecked();
            Date fecha = manual ? fechaSeleccionada : new Date();
            Iec870DateTime nueva = Iec870DateTime.fromDate(fecha, TimeZone.getDefault());

            if (manual) {
                int offset;
                try {
                    offset = Integer.parseInt(editTimezone.getText().toString().trim());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "La zona horaria debe ser un número entero de minutos (ej: 120)",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // Por encima de la hora estándar de la zona del móvil = horario de verano
                int estandar = TimeZone.getDefault().getRawOffset() / 60000;
                nueva.summer = offset > estandar;
            }

            bloquear(true, btnRead, btnNext, checkBoxManual, progressBar);

            new Thread(() -> {
                try {
                    Iec870DateTime leida = Iec870DateReader
                            .programarFecha(IecDateActivity.this, config, nueva);

                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, checkBoxManual, progressBar);
                        Toast.makeText(IecDateActivity.this,
                                "Fecha y hora programada correctamente.\nHora del contador: "
                                        + leida.toText(),
                                Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, checkBoxManual, progressBar);
                        mostrarError("No se pudo programar la fecha y hora del contador.", e);
                    });
                }
            }).start();
        });
    }

    /** Desfase respecto a UTC del sistema en la fecha dada, en minutos (p. ej. 120 en verano). */
    private static int offsetSistemaMinutos(Date fecha) {
        return TimeZone.getDefault().getOffset(fecha.getTime()) / 60000;
    }

    private void bloquear(boolean leyendo, ExtendedFloatingActionButton btnRead,
                          ExtendedFloatingActionButton btnNext, MaterialCheckBox manual,
                          LinearLayout progress) {
        btnRead.setEnabled(!leyendo);
        btnNext.setEnabled(!leyendo);
        manual.setEnabled(!leyendo);
        progress.setVisibility(leyendo ? View.VISIBLE : View.GONE);
    }

    private void mostrarError(String mensaje, Exception e) {
        new AlertDialog.Builder(this)
                .setTitle("Error (IEC-870)")
                .setMessage(mensaje + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .show();
    }

    /** DatePicker -> TimePicker encadenados; actualiza fechaSeleccionada. */
    private void mostrarDateTimePicker(EditText editDateTime) {
        Calendar calInicial = Calendar.getInstance();
        calInicial.setTime(fechaSeleccionada);

        new DatePickerDialog(
                this,
                (dateView, year, month, day) -> new TimePickerDialog(
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
                ).show(),
                calInicial.get(Calendar.YEAR),
                calInicial.get(Calendar.MONTH),
                calInicial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}