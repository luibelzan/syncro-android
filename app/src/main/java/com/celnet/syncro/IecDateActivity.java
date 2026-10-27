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
import com.celnet.syncro.models.iec870.params.Iec870DaylightChange;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Fecha y hora del contador por IEC-870.
 * - "Leer": lee la fecha del contador y la muestra en {@link ResultadosDateActivity}
 *   (con la del sistema y la diferencia); también lee las fechas oficiales de cambio de
 *   hora del contador y las pasa a esa pantalla para mostrarlas.
 * - "Programar": escribe en el contador, según las casillas marcadas, la fecha y hora (la hora
 *   actual del móvil, o la elegida con los pickers si se marca la configuración manual) y/o las
 *   fechas oficiales de cambio de hora (por defecto 26/03 02:00 y 29/10 03:00 del año actual).
 */
public class IecDateActivity extends BaseActivity {

    // Solo para mostrar en pantalla
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Fecha elegida con los pickers (solo se usa con el checkbox marcado)
    private Date fechaSeleccionada = new Date();

    // Fechas de cambio de hora oficial (solo se usan con su checkbox marcado)
    private Date dstVerano;
    private Date dstInvierno;

    private static Date fecha(int anio, int mes, int dia, int hora) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, hora, 0, 0);
        return c.getTime();
    }

    private static Iec870DateTime aIecSinSU(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Iec870DateTime t = new Iec870DateTime();
        t.year = c.get(Calendar.YEAR);
        t.month = c.get(Calendar.MONTH) + 1;
        t.day = c.get(Calendar.DAY_OF_MONTH);
        t.hour = c.get(Calendar.HOUR_OF_DAY);
        t.minute = c.get(Calendar.MINUTE);
        t.summer = false; // como el software funcional: ambas entradas con SU = 0
        return t;
    }

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
        MaterialCheckBox checkProgramDate = findViewById(R.id.checkProgramDate);
        MaterialCheckBox checkProgramDst = findViewById(R.id.checkProgramDst);
        EditText editDstSummer = findViewById(R.id.editDstSummer);
        EditText editDstWinter = findViewById(R.id.editDstWinter);
        ExtendedFloatingActionButton btnRead = findViewById(R.id.btnRead);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        editDateTime.setText(SDF.format(fechaSeleccionada));
        editTimezone.setText(String.valueOf(offsetSistemaMinutos(fechaSeleccionada)));
        editDateTime.setEnabled(false);
        editTimezone.setEnabled(false);

        int anio = Calendar.getInstance().get(Calendar.YEAR);
        dstVerano = fecha(anio, 3, 26, 2);
        dstInvierno = fecha(anio, 10, 29, 3);
        editDstSummer.setText(SDF.format(dstVerano));
        editDstWinter.setText(SDF.format(dstInvierno));

        checkProgramDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkBoxManual.setEnabled(isChecked);
            if (!isChecked) {
                checkBoxManual.setChecked(false);
            }
        });

        checkProgramDst.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editDstSummer.setEnabled(isChecked);
            editDstWinter.setEnabled(isChecked);
        });

        editDstSummer.setOnClickListener(v -> mostrarDateTimePicker(dstVerano, d -> {
            dstVerano = d;
            editDstSummer.setText(SDF.format(d));
        }));
        editDstWinter.setOnClickListener(v -> mostrarDateTimePicker(dstInvierno, d -> {
            dstInvierno = d;
            editDstWinter.setText(SDF.format(d));
        }));

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

        editDateTime.setOnClickListener(v -> mostrarDateTimePicker(fechaSeleccionada, d -> {
            fechaSeleccionada = d;
            editDateTime.setText(SDF.format(d));
        }));

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

        // ---------------- Programar ----------------
        btnNext.setOnClickListener(v -> {
            boolean progFecha = checkProgramDate.isChecked();
            boolean progDst = checkProgramDst.isChecked();
            if (!progFecha && !progDst) {
                Toast.makeText(this, "Marque al menos una de las dos opciones a programar",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Iec870DateTime nueva = null;
            if (progFecha) {
                // Sin modo manual: la hora del móvil en este momento (no la de cuando se abrió la pantalla)
                boolean manual = checkBoxManual.isChecked();
                Date fecha = manual ? fechaSeleccionada : new Date();
                nueva = Iec870DateTime.fromDate(fecha, TimeZone.getDefault());

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
            }

            Iec870DaylightChange cambio = null;
            if (progDst) {
                cambio = new Iec870DaylightChange();
                cambio.inicioVerano = aIecSinSU(dstVerano);
                cambio.inicioInvierno = aIecSinSU(dstInvierno);
            }

            final Iec870DateTime fNueva = nueva;
            final Iec870DaylightChange fCambio = cambio;
            bloquear(true, btnRead, btnNext, checkBoxManual, progressBar);

            new Thread(() -> {
                try {
                    Iec870DateTime leida = Iec870DateReader
                            .programar(IecDateActivity.this, config, fNueva, fCambio);

                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, checkBoxManual, progressBar);
                        StringBuilder msg = new StringBuilder();
                        if (leida != null) {
                            msg.append("Fecha y hora programada correctamente.\nHora del contador: ")
                                    .append(leida.toText());
                        }
                        if (fCambio != null) {
                            if (msg.length() > 0) msg.append('\n');
                            msg.append("Fechas de cambio de hora oficial programadas.");
                        }
                        Toast.makeText(IecDateActivity.this, msg.toString(),
                                Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, checkBoxManual, progressBar);
                        mostrarError("No se pudo programar el contador.", e);
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
        findViewById(R.id.checkProgramDate).setEnabled(!leyendo);
        findViewById(R.id.checkProgramDst).setEnabled(!leyendo);
        progress.setVisibility(leyendo ? View.VISIBLE : View.GONE);
    }

    private void mostrarError(String mensaje, Exception e) {
        new AlertDialog.Builder(this)
                .setTitle("Error (IEC-870)")
                .setMessage(mensaje + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .show();
    }

    /** DatePicker -> TimePicker encadenados; entrega la fecha elegida con {@code alElegir}. */
    private void mostrarDateTimePicker(Date inicial, Consumer<Date> alElegir) {
        Calendar calInicial = Calendar.getInstance();
        calInicial.setTime(inicial);

        new DatePickerDialog(
                this,
                (dateView, year, month, day) -> new TimePickerDialog(
                        this,
                        (timeView, hour, minute) -> {
                            Calendar cal = Calendar.getInstance();
                            cal.set(year, month, day, hour, minute, 0);
                            cal.set(Calendar.MILLISECOND, 0);
                            alElegir.accept(cal.getTime());
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