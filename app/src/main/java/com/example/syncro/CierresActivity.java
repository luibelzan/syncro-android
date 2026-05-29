package com.example.syncro;

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

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreFila;
import com.example.syncro.models.CierreMensualFila;
import com.example.syncro.models.CurvaFila;
import com.example.syncro.objects.loadProfiles.LoadProfileReader;
import com.example.syncro.objects.params.SerialNumberReader;
import com.example.syncro.objects.pricing.BillingDataReader;
import com.example.syncro.objects.pricing.CurrentBillingReader;
import com.example.syncro.objects.pricing.DailyBillingS05;
import com.example.syncro.objects.pricing.MonthlyBillingS04;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
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
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
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

            String fechaInicio = editFechaInicio.getText().toString();
            String fechaFin = editFechaFin.getText().toString();
            String tipoCierre = spinnerTipoCierre.getSelectedItem().toString();
            String contrato = spinnerContrato.getSelectedItem().toString();
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

            progressBar.setVisibility(View.VISIBLE);

            // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
            new Thread(() -> {
                try {
                    GXDLMSReader reader;
                    DLMSConnection conn;

                    if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                        conn = new DLMSConnection(config.getBluetoothDeviceName());
                        reader = conn.bluetoothConnnect(CierresActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect(this);
                    }

                    // Leer curvas
                    String cntId = SerialNumberReader.readSerialNumer(reader);
                    if(tipoCierre.equals("Diarios_S05")) {
                        ArrayList<CierreFila> datos = DailyBillingS05.leerS05(reader, fechaInicio, fechaFin, 1);

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                            Intent intent = new Intent(CierresActivity.this, ResultadosCierresActivity.class);
                            intent.putParcelableArrayListExtra("datos_cierres_tabla", datos);
                            intent.putExtra("cntId", cntId);
                            startActivity(intent);

                        });
                    } else if(tipoCierre.equals("Mensuales_S04")) {
                        ArrayList<CierreMensualFila> datos = MonthlyBillingS04.leerS04(reader, fechaInicio, fechaFin, 1);

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                            Intent intent = new Intent(CierresActivity.this, ResultadosCierresMensualesActivity.class);
                            intent.putParcelableArrayListExtra("datos_cierres_tabla", datos);
                            intent.putExtra("cntId", cntId);
                            startActivity(intent);

                        });
                    } else if(tipoCierre.equals("EnCurso_S27")) {
                        CurrentBillingReader.readCurrentBilling(reader);
                    }

                    conn.close();



                } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CierresActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        });


    }
}