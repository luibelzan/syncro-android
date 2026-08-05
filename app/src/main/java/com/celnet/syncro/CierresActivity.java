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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.CierreEnCursoFila;
import com.celnet.syncro.models.CierreFila;
import com.celnet.syncro.models.CierreMensualFila;
import com.celnet.syncro.objects.pricing.CurrentBillingReader;
import com.celnet.syncro.objects.pricing.DailyBillingS05;
import com.celnet.syncro.objects.pricing.MonthlyBillingS04;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CierresActivity extends BaseActivity {

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedYear + "/" +
                            String.format(Locale.US, "%02d", selectedMonth + 1) + "/" +
                            String.format(Locale.US, "%02d", selectedDay);
                    editText.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void actualizarVisibilidadFechas(String tipo,
                                             TextInputLayout layoutFechaInicio,
                                             TextInputLayout layoutFechaFin) {
        boolean esEnCurso = tipo.equals("Actuales (S27)");
        layoutFechaInicio.setVisibility(esEnCurso ? View.GONE : View.VISIBLE);
        layoutFechaFin.setVisibility(esEnCurso ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cierres);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextInputLayout layoutFechaInicio = findViewById(R.id.layoutFechaInicio);
        TextInputLayout layoutFechaFin = findViewById(R.id.layoutFechaFin);

        // Desplegable de tipo de cierre
        AutoCompleteTextView spinnerTipoCierre = findViewById(R.id.spinnerTipoCierre);
        String[] tiposCierre = getResources().getStringArray(R.array.cierres_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tiposCierre);
        spinnerTipoCierre.setAdapter(adapter);

        // Selección por defecto (equivalente al comportamiento del Spinner original)
        if (tiposCierre.length > 0) {
            spinnerTipoCierre.setText(tiposCierre[0], false);
            actualizarVisibilidadFechas(tiposCierre[0], layoutFechaInicio, layoutFechaFin);
        }

        spinnerTipoCierre.setOnItemClickListener((parent, view, position, id) -> {
            String tipo = parent.getItemAtPosition(position).toString();
            actualizarVisibilidadFechas(tipo, layoutFechaInicio, layoutFechaFin);
        });

        // Desplegable de contratos
        AutoCompleteTextView spinnerContrato = findViewById(R.id.spinnerContrato);
        String[] contratosArray = getResources().getStringArray(R.array.contratos_array);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, contratosArray);
        spinnerContrato.setAdapter(adapter2);
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

            if (!validarFechasCierre(
                    tipoCierre,
                    editFechaInicio,
                    editFechaFin)) {

                return;
            }

            String fechaInicio = editFechaInicio.getText().toString();
            String fechaFin = editFechaFin.getText().toString();

            int posicionContrato = Arrays.asList(contratosArray).indexOf(spinnerContrato.getText().toString());
            int contrato = posicionContrato + 1;

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // Mostrar valores en Toast de depuración
            Toast.makeText(CierresActivity.this,
                    "Conexión: " + config.getType() + "\n" +
                            "IP: " + config.getIp() + "\n" +
                            "Puerto: " + config.getPort() + "\n" +
                            "Dispositivo: " + config.getBluetoothDeviceName() + "\n" +
                            "Fecha Inicio: " + fechaInicio + "\n" +
                            "Fecha Fin: " + fechaFin,
                    Toast.LENGTH_LONG).show();

            // Bloquear interacción mientras carga
            btnNext.setEnabled(false);
            spinnerTipoCierre.setEnabled(false);
            spinnerContrato.setEnabled(false);
            editFechaInicio.setEnabled(false);
            editFechaFin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            int contratoFinal = contrato;
            int posicionContratoFinal = posicionContrato;

            // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(CierresActivity.this);

                    // Leer curvas
                    if (tipoCierre.equals("Diarios (S05)")) {
                        ArrayList<CierreFila> datos;

                        if (posicionContratoFinal == 3) {
                            datos = DailyBillingS05.leerS05Todos(res.reader, fechaInicio, fechaFin);
                        } else {
                            datos = DailyBillingS05.leerS05(res.reader, fechaInicio, fechaFin, contratoFinal);
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            reactivarControles(btnNext, spinnerTipoCierre, spinnerContrato, editFechaInicio, editFechaFin);
                            Intent intent = new Intent(CierresActivity.this,
                                    ResultadosCierresActivity.class);
                            intent.putParcelableArrayListExtra("datos_cierres_tabla", datos);
                            intent.putExtra("cntId", res.serialNumber);
                            startActivity(intent);
                        });
                    } else if (tipoCierre.equals("Mensuales (S04)")) {
                        ArrayList<CierreMensualFila> datos;

                        if (posicionContratoFinal == 3) {
                            datos = MonthlyBillingS04.leerS04Todos(res.reader, fechaInicio, fechaFin);
                        } else {
                            datos = MonthlyBillingS04.leerS04(res.reader, fechaInicio, fechaFin, contratoFinal);
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            reactivarControles(btnNext, spinnerTipoCierre, spinnerContrato, editFechaInicio, editFechaFin);
                            Intent intent = new Intent(CierresActivity.this,
                                    ResultadosCierresMensualesActivity.class);
                            intent.putParcelableArrayListExtra("datos_cierres_tabla", datos);
                            intent.putExtra("cntId", res.serialNumber);
                            startActivity(intent);
                        });
                    } else if (tipoCierre.equals("Actuales (S27)")) {
                        ArrayList<CierreEnCursoFila> datos;

                        if (posicionContratoFinal == 3) {
                            datos = CurrentBillingReader.readCurrentBilling(res.reader);
                        } else {
                            datos = CurrentBillingReader.readCurrentBilling(res.reader, contratoFinal);
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            reactivarControles(btnNext, spinnerTipoCierre, spinnerContrato, editFechaInicio, editFechaFin);
                            Intent intent = new Intent(CierresActivity.this, ResultadosCierresEnCursoActivity.class);
                            intent.putParcelableArrayListExtra("datos_cierres_tabla", datos);
                            intent.putExtra("cntId", res.serialNumber);
                            startActivity(intent);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        reactivarControles(btnNext, spinnerTipoCierre, spinnerContrato, editFechaInicio, editFechaFin);

                        new androidx.appcompat.app.AlertDialog.Builder(CierresActivity.this)
                                .setTitle("Error de lectura")
                                .setMessage("No se pudieron leer los cierres del contador.\n\n"
                                        + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .setCancelable(true)
                                .show();
                    });
                } finally {
                    conn.close();
                }
            }).start();
        });
    }

    private void reactivarControles(ExtendedFloatingActionButton btnNext,
                                    AutoCompleteTextView spinnerTipoCierre,
                                    AutoCompleteTextView spinnerContrato,
                                    EditText editFechaInicio,
                                    EditText editFechaFin) {
        btnNext.setEnabled(true);
        spinnerTipoCierre.setEnabled(true);
        spinnerContrato.setEnabled(true);
        editFechaInicio.setEnabled(true);
        editFechaFin.setEnabled(true);
    }

    private boolean validarFechasCierre(String tipoCierre,
                                        EditText editFechaInicio,
                                        EditText editFechaFin) {

        // S27 no necesita fechas
        if (tipoCierre.equals("Actuales (S27)")) {
            return true;
        }

        String fechaInicio = editFechaInicio.getText().toString().trim();
        String fechaFin = editFechaFin.getText().toString().trim();

        if (fechaInicio.isEmpty()) {
            Toast.makeText(
                    this,
                    "Debe seleccionar una fecha de inicio",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        if (fechaFin.isEmpty()) {
            Toast.makeText(
                    this,
                    "Debe seleccionar una fecha de fin",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        try {

            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy/MM/dd", Locale.US);

            sdf.setLenient(false);

            Date inicio = sdf.parse(fechaInicio);
            Date fin = sdf.parse(fechaFin);

            if (inicio.after(fin)) {

                Toast.makeText(
                        this,
                        "La fecha inicio no puede ser posterior a la fecha fin",
                        Toast.LENGTH_LONG
                ).show();

                return false;
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Formato de fecha inválido",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }
}