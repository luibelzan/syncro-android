package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.CierreEnCursoFila;
import com.celnet.syncro.models.CierreFila;
import com.celnet.syncro.models.CierreMensualFila;
import com.celnet.syncro.models.CurvaFila;
import com.celnet.syncro.objects.loadProfiles.LoadProfileReader;
import com.celnet.syncro.objects.params.SerialNumberReader;
import com.celnet.syncro.objects.pricing.BillingDataReader;
import com.celnet.syncro.objects.pricing.CurrentBillingReader;
import com.celnet.syncro.objects.pricing.DailyBillingS05;
import com.celnet.syncro.objects.pricing.MonthlyBillingS04;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
                            String.format(Locale.US,"%02d", selectedMonth + 1) + "/" +
                            String.format(Locale.US,"%02d", selectedDay);
                    editText.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
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

        // Spinner de tipo de curva
        Spinner spinnerTipoCierre = findViewById(R.id.spinnerTipoCierre);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.cierres_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoCierre.setAdapter(adapter);
        spinnerTipoCierre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipo = parent.getItemAtPosition(position).toString();
                boolean esEnCurso = tipo.equals("Actuales (S27)");

                findViewById(R.id.textFechaInicio).setVisibility(esEnCurso ? View.GONE : View.VISIBLE);
                findViewById(R.id.editFechaInicio).setVisibility(esEnCurso ? View.GONE : View.VISIBLE);
                findViewById(R.id.textFechaFin).setVisibility(esEnCurso ? View.GONE : View.VISIBLE);
                findViewById(R.id.editFechaFin).setVisibility(esEnCurso ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner de contratos
        Spinner spinnerContrato = findViewById(R.id.spinnerContrato);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,
                R.array.contratos_array,
                android.R.layout.simple_spinner_item
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContrato.setAdapter(adapter2);
        spinnerContrato.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        EditText editFechaInicio = findViewById(R.id.editFechaInicio);
        EditText editFechaFin = findViewById(R.id.editFechaFin);
        editFechaInicio.setOnClickListener(v -> showDatePicker(editFechaInicio));
        editFechaFin.setOnClickListener(v -> showDatePicker(editFechaFin));


        LinearLayout progressBar = findViewById(R.id.progressContainer);

        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            String tipoCierre =
                    spinnerTipoCierre.getSelectedItem().toString();

            if (!validarFechasCierre(
                    tipoCierre,
                    editFechaInicio,
                    editFechaFin)) {

                return;
            }

            String fechaInicio = editFechaInicio.getText().toString();
            String fechaFin = editFechaFin.getText().toString();
            int contrato = spinnerContrato.getSelectedItemPosition() + 1;
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

            // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
            new Thread(() -> {
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(CierresActivity.this);

                    // Leer curvas
                    if (tipoCierre.equals("Diarios (S05)")) {
                        ArrayList<CierreFila> datos;

                        if (spinnerContrato.getSelectedItemPosition() == 3) {
                            datos = DailyBillingS05.leerS05Todos(res.reader, fechaInicio, fechaFin);
                        } else {
                            datos = DailyBillingS05.leerS05(res.reader, fechaInicio, fechaFin, contrato);
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

                        if (spinnerContrato.getSelectedItemPosition() == 3) {
                            datos = MonthlyBillingS04.leerS04Todos(res.reader, fechaInicio, fechaFin);
                        } else {
                            datos = MonthlyBillingS04.leerS04(res.reader, fechaInicio, fechaFin, contrato);
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
                        int posicion = spinnerContrato.getSelectedItemPosition();
                        ArrayList<CierreEnCursoFila> datos;

                        if (posicion == 3) {
                            datos = CurrentBillingReader.readCurrentBilling(res.reader);
                        } else {
                            datos = CurrentBillingReader.readCurrentBilling(res.reader, contrato);
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

                    conn.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        reactivarControles(btnNext, spinnerTipoCierre, spinnerContrato, editFechaInicio, editFechaFin);
                        Toast.makeText(CierresActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        });
    }

    private void reactivarControles(ImageButton btnNext,
                                    Spinner spinnerTipoCierre,
                                    Spinner spinnerContrato,
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