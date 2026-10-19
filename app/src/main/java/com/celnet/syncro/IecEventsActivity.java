package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.eventos.Iec870Events;
import com.celnet.syncro.objects.iec870.events.Iec870EventsReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Lectura de eventos por IEC-870. De momento: "Arranques y tensión bajo límites" (registro 52). */
public class IecEventsActivity extends BaseActivity {

    private static final String[] TIPOS = {"Arranques y tensión bajo límites"};
    private static final int[] REGISTROS = {Iec870Events.REG_ARRANQUES_TENSION};

    private void showDatePicker(EditText editText) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            editText.setText(String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y));
            editText.setTag(String.format(Locale.US, "%04d/%02d/%02d", y, m + 1, d));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String obtenerFechaInterna(EditText editText) {
        Object tag = editText.getTag();
        return tag != null ? tag.toString() : "";
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        findViewById(R.id.main).requestApplyInsets();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText editFechaInicio = findViewById(R.id.editFechaInicio);
        EditText editFechaFin = findViewById(R.id.editFechaFin);
        editFechaInicio.setOnClickListener(v -> showDatePicker(editFechaInicio));
        editFechaFin.setOnClickListener(v -> showDatePicker(editFechaFin));

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        TextInputLayout layoutTipo = findViewById(R.id.layoutTipoEvento);
        layoutTipo.setHint("Tipo de evento");

        AutoCompleteTextView spinnerTipo = findViewById(R.id.spinnerTipoEventoIec);
        spinnerTipo.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, TIPOS));
        spinnerTipo.setText(TIPOS[0], false);

        btnNext.setOnClickListener(v -> {
            if (!validarFechas(editFechaInicio, editFechaFin)) return;

            int registro = REGISTROS[0];
            String seleccion = spinnerTipo.getText().toString();
            for (int i = 0; i < TIPOS.length; i++) {
                if (TIPOS[i].equals(seleccion)) {
                    registro = REGISTROS[i];
                    break;
                }
            }
            final int registroFinal = registro;

            String fechaInicio = obtenerFechaInterna(editFechaInicio);
            String fechaFin = obtenerFechaInterna(editFechaFin);
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            btnNext.setEnabled(false);
            editFechaInicio.setEnabled(false);
            editFechaFin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                try {
                    Iec870EventsReader.Resultado res = Iec870EventsReader.leerEventos(
                            IecEventsActivity.this, config, registroFinal, fechaInicio, fechaFin);

                    Intent intent = new Intent(IecEventsActivity.this, ResultadosEventsActivity.class);
                    intent.putParcelableArrayListExtra("datos_event_tabla", res.filas);
                    intent.putExtra("cntId", res.serie);

                    runOnUiThread(() -> {
                        restaurarUi(progressBar, btnNext, editFechaInicio, editFechaFin);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        restaurarUi(progressBar, btnNext, editFechaInicio, editFechaFin);
                        new androidx.appcompat.app.AlertDialog.Builder(IecEventsActivity.this)
                                .setTitle("Error de lectura (IEC-870)")
                                .setMessage("No se pudieron leer los eventos.\n\n" + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .show();
                    });
                }
            }).start();
        });
    }

    private void restaurarUi(LinearLayout progressBar, ExtendedFloatingActionButton btnNext,
                             EditText ini, EditText fin) {
        progressBar.setVisibility(View.GONE);
        btnNext.setEnabled(true);
        ini.setEnabled(true);
        fin.setEnabled(true);
    }

    private boolean validarFechas(EditText editFechaInicio, EditText editFechaFin) {
        String fechaInicio = obtenerFechaInterna(editFechaInicio);
        String fechaFin = obtenerFechaInterna(editFechaFin);

        if (fechaInicio.isEmpty()) {
            editFechaInicio.setError("Seleccione una fecha de inicio");
            editFechaInicio.requestFocus();
            return false;
        }
        if (fechaFin.isEmpty()) {
            editFechaFin.setError("Seleccione una fecha de fin");
            editFechaFin.requestFocus();
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
            sdf.setLenient(false);
            Date inicio = sdf.parse(fechaInicio);
            Date fin = sdf.parse(fechaFin);
            if (inicio.after(fin)) {
                editFechaInicio.setError("La fecha de inicio no puede ser posterior a la fecha fin");
                return false;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}