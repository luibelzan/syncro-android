package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.objects.iec870.cierres.Iec870BillingReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Tarificación por IEC-870 (cierres). Tipos: Memorizados (contrato y fechas) y En
 * curso (solo contrato, sin fechas). Contratos: los mismos desplegables que en DLMS (contratos 1..3 y
 * "todos", este último en la posición 3 de {@code contratos_array}).
 */
public class IecCierresActivity extends BaseActivity {

    private static final String MEMORIZADOS = "Memorizados";
    private static final String EN_CURSO = "En curso";

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_cierres);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextInputLayout layoutFechaInicio = findViewById(R.id.layoutFechaInicio);
        TextInputLayout layoutFechaFin = findViewById(R.id.layoutFechaFin);

        // Tipo de cierre
        AutoCompleteTextView spinnerTipoCierre = findViewById(R.id.spinnerTipoCierre);
        String[] tipos = {MEMORIZADOS, EN_CURSO};
        spinnerTipoCierre.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tipos));
        spinnerTipoCierre.setText(tipos[0], false);
        spinnerTipoCierre.setOnItemClickListener((parent, view, position, id) ->
                actualizarVisibilidadFechas(parent.getItemAtPosition(position).toString(),
                        layoutFechaInicio, layoutFechaFin));

        // Contrato
        AutoCompleteTextView spinnerContrato = findViewById(R.id.spinnerContrato);
        String[] contratosArray = getResources().getStringArray(R.array.contratos_array);
        spinnerContrato.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, contratosArray));
        if (contratosArray.length > 0) {
            spinnerContrato.setText(contratosArray[0], false);
        }

        EditText editFechaInicio = findViewById(R.id.editFechaInicio);
        EditText editFechaFin = findViewById(R.id.editFechaFin);
        editFechaInicio.setOnClickListener(v -> showDatePicker(editFechaInicio));
        editFechaFin.setOnClickListener(v -> showDatePicker(editFechaFin));

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String tipoCierre = spinnerTipoCierre.getText().toString();

            boolean enCurso = EN_CURSO.equals(tipoCierre);
            if (!enCurso && !validarFechas(editFechaInicio, editFechaFin)) {
                return;
            }

            String fechaInicio = obtenerFechaInterna(editFechaInicio);
            String fechaFin = obtenerFechaInterna(editFechaFin);

            int posicion = Arrays.asList(contratosArray)
                    .indexOf(spinnerContrato.getText().toString());
            // Posición 3 = todos los contratos (misma convención que en DLMS)
            int[] contratos = (posicion == 3) ? new int[]{1, 2, 3} : new int[]{posicion + 1};

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            bloquear(true, btnNext, spinnerTipoCierre, spinnerContrato,
                    editFechaInicio, editFechaFin, progressBar);

            new Thread(() -> {
                try {
                    Iec870BillingReader.Resultado res = enCurso
                            ? Iec870BillingReader.leerEnCurso(
                            IecCierresActivity.this, config, contratos)
                            : Iec870BillingReader.leerMemorizados(
                            IecCierresActivity.this, config, contratos,
                            fechaInicio, fechaFin);

                    runOnUiThread(() -> {
                        bloquear(false, btnNext, spinnerTipoCierre, spinnerContrato,
                                editFechaInicio, editFechaFin, progressBar);
                        if (res.registros.isEmpty()) {
                            new AlertDialog.Builder(IecCierresActivity.this)
                                    .setTitle("Sin cierres")
                                    .setMessage(enCurso
                                            ? "El contador no devolvió valores en curso."
                                            : "El contador no tiene cierres memorizados entre "
                                            + editFechaInicio.getText() + " y "
                                            + editFechaFin.getText() + ".")
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                            return;
                        }
                        Intent intent = new Intent(IecCierresActivity.this,
                                ResultadosIecCierresActivity.class);
                        intent.putExtra(ResultadosIecCierresActivity.EXTRA_REGISTROS, res.registros);
                        intent.putExtra("cntId", res.serie);
                        intent.putExtra(ResultadosIecCierresActivity.EXTRA_TITULO,
                                enCurso ? "Cierres en curso" : "Cierres memorizados");
                        intent.putExtra(ResultadosIecCierresActivity.EXTRA_EN_CURSO, enCurso);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnNext, spinnerTipoCierre, spinnerContrato,
                                editFechaInicio, editFechaFin, progressBar);
                        new AlertDialog.Builder(IecCierresActivity.this)
                                .setTitle("Error de lectura (IEC-870)")
                                .setMessage("No se pudieron leer los cierres del contador.\n\n"
                                        + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .show();
                    });
                }
            }).start();
        });
    }

    private void actualizarVisibilidadFechas(String tipo, TextInputLayout inicio, TextInputLayout fin) {
        int vis = EN_CURSO.equals(tipo) ? View.GONE : View.VISIBLE;
        inicio.setVisibility(vis);
        fin.setVisibility(vis);
    }

    private void bloquear(boolean leyendo, ExtendedFloatingActionButton btnNext,
                          AutoCompleteTextView tipo, AutoCompleteTextView contrato,
                          EditText inicio, EditText fin, LinearLayout progress) {
        btnNext.setEnabled(!leyendo);
        tipo.setEnabled(!leyendo);
        contrato.setEnabled(!leyendo);
        inicio.setEnabled(!leyendo);
        fin.setEnabled(!leyendo);
        progress.setVisibility(leyendo ? View.VISIBLE : View.GONE);
    }

    private boolean validarFechas(EditText editFechaInicio, EditText editFechaFin) {
        String fechaInicio = obtenerFechaInterna(editFechaInicio);
        String fechaFin = obtenerFechaInterna(editFechaFin);

        if (fechaInicio.isEmpty()) {
            Toast.makeText(this, "Debe seleccionar una fecha de inicio", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (fechaFin.isEmpty()) {
            Toast.makeText(this, "Debe seleccionar una fecha de fin", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
            sdf.setLenient(false);
            Date inicio = sdf.parse(fechaInicio);
            Date fin = sdf.parse(fechaFin);
            if (inicio.after(fin)) {
                Toast.makeText(this, "La fecha inicio no puede ser posterior a la fecha fin",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}