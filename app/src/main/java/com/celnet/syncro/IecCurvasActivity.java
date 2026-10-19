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

import com.celnet.syncro.models.curvas.TipoCurva;
import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.objects.iec870.curvas.Iec870LoadProfileReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Curvas de carga por IEC-870. Periodos: Horaria y Cuartihoraria (implementadas),
 * Diaria (pendiente).
 */
public class IecCurvasActivity extends BaseActivity {

    private static final String HORARIA = "Horaria";
    private static final String CUARTIHORARIA = "Cuartihoraria";
    private static final String DIARIA = "Diaria";

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    // Interno (yyyy/MM/dd) en el tag; visible dd/MM/yyyy
                    editText.setText(String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y));
                    editText.setTag(String.format(Locale.US, "%04d/%02d/%02d", y, m + 1, d));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
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
        setContentView(R.layout.activity_iec_curvas);
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

        TextInputLayout layoutTipo = findViewById(R.id.layoutTipoCurva);
        layoutTipo.setHint("Periodo de la curva");

        AutoCompleteTextView spinnerTipo = findViewById(R.id.spinnerTipoCurvaIec);
        String[] periodos = {HORARIA, CUARTIHORARIA, DIARIA};
        spinnerTipo.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, periodos));
        spinnerTipo.setText(periodos[0], false);

        btnNext.setOnClickListener(v -> {
            if (!validarFechas(editFechaInicio, editFechaFin)) return;

            String periodo = spinnerTipo.getText().toString();
            if (DIARIA.equals(periodo)) {
                Toast.makeText(this, "La curva " + periodo.toLowerCase(Locale.ROOT)
                        + " aún no está disponible por IEC-870", Toast.LENGTH_LONG).show();
                return;
            }

            Iec870Client.Periodo periodoIec = CUARTIHORARIA.equals(periodo)
                    ? Iec870Client.Periodo.CUARTOHORARIA
                    : Iec870Client.Periodo.HORARIA;
            String fechaInicio = obtenerFechaInterna(editFechaInicio);
            String fechaFin = obtenerFechaInterna(editFechaFin);
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            btnNext.setEnabled(false);
            editFechaInicio.setEnabled(false);
            editFechaFin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                try {
                    Iec870LoadProfileReader.Resultado res = Iec870LoadProfileReader
                            .leerCurvaCarga(IecCurvasActivity.this, config, fechaInicio, fechaFin, periodoIec);

                    Intent intent = new Intent(IecCurvasActivity.this, ResultadosCurvasActivity.class);
                    intent.putExtra("tipoCurva", TipoCurva.INCREMENTAL_S02.name());
                    intent.putExtra("cntId", res.serie);
                    intent.putParcelableArrayListExtra("datos_curva_tabla", res.filas);

                    runOnUiThread(() -> {
                        restaurarUi(progressBar, btnNext, editFechaInicio, editFechaFin);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        restaurarUi(progressBar, btnNext, editFechaInicio, editFechaFin);
                        new androidx.appcompat.app.AlertDialog.Builder(IecCurvasActivity.this)
                                .setTitle("Error de lectura (IEC-870)")
                                .setMessage("No se pudo leer la curva.\n\n" + e.getMessage())
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