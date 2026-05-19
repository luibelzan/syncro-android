package com.example.syncro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.objects.contracts.ProgramContract;
import com.example.syncro.objects.params.SerialNumberReader;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProgramarContratoActivity extends AppCompatActivity {

    // CheckBoxes
    private CheckBox checkBoxTarif;
    private CheckBox checkBoxPowerLimit;
    private CheckBox checkBoxFechaAct;
    private CheckBox checkBoxFechaFact;

    // Spinners
    private Spinner spinnerContrato;
    private Spinner spinnerTarifa;

    // Límites de potencia
    private EditText editPowerLimitT1;
    private EditText editPowerLimitT2;
    private EditText editPowerLimitT3;
    private EditText editPowerLimitT4;
    private EditText editPowerLimitT5;
    private EditText editPowerLimitT6;

    // Fecha activación
    private EditText editFechaAct;
    private EditText editHourAct;

    // Fecha facturación
    private EditText editFechaFact;

    private void configurarPickers() {

        // Fecha activación
        editFechaAct.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {

                        String fecha = String.format(
                                Locale.getDefault(),
                                "%04d/%02d/%02d",
                                selectedYear,
                                selectedMonth + 1,
                                selectedDay
                        );

                        editFechaAct.setText(fecha);

                    },
                    year,
                    month,
                    day
            );

            datePickerDialog.show();
        });

        // Hora activación
        editHourAct.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, selectedHour, selectedMinute) -> {

                        String hora = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                        );

                        editHourAct.setText(hora);

                    },
                    hour,
                    minute,
                    true
            );

            timePickerDialog.show();
        });

        // Cierre facturación
        editFechaFact.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {

                        String fecha = String.format(
                                Locale.getDefault(),
                                "%04d/%02d/%02d",
                                selectedYear,
                                selectedMonth + 1,
                                selectedDay
                        );

                        editFechaFact.setText(fecha);

                    },
                    year,
                    month,
                    day
            );

            datePickerDialog.show();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_programar_contrato);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Spinner de contrato
        spinnerContrato = findViewById(R.id.spinnerContrato);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.program_contracts_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContrato.setAdapter(adapter);
        spinnerContrato.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Spinner tarifa
        spinnerTarifa = findViewById(R.id.spinnerTarif);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,
                R.array.tarifas_array,
                android.R.layout.simple_spinner_item
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTarifa.setAdapter(adapter2);
        spinnerTarifa.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        initViews();
        configurarPickers();

        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // ── Contrato ────────────────────────────────────────────────────────
            int contract = spinnerContrato.getSelectedItemPosition() + 1;

            // ── Tarifa ──────────────────────────────────────────────────────────
            String tarifaSeleccionada = null;
            if (checkBoxTarif.isChecked()) {
                tarifaSeleccionada = spinnerTarifa.getSelectedItem().toString();
            }

            // ── Límites de potencia ─────────────────────────────────────────────
            long[] thresholds = null;
            if (checkBoxPowerLimit.isChecked()) {
                String t1 = editPowerLimitT1.getText().toString().trim();
                String t2 = editPowerLimitT2.getText().toString().trim();
                String t3 = editPowerLimitT3.getText().toString().trim();
                String t4 = editPowerLimitT4.getText().toString().trim();
                String t5 = editPowerLimitT5.getText().toString().trim();
                String t6 = editPowerLimitT6.getText().toString().trim();

                if (TextUtils.isEmpty(t1) || TextUtils.isEmpty(t2) || TextUtils.isEmpty(t3) ||
                        TextUtils.isEmpty(t4) || TextUtils.isEmpty(t5) || TextUtils.isEmpty(t6)) {
                    Toast.makeText(this, "Completa todos los límites de potencia", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    thresholds = new long[]{
                            (long) Double.parseDouble(t1),
                            (long) Double.parseDouble(t2),
                            (long) Double.parseDouble(t3),
                            (long) Double.parseDouble(t4),
                            (long) Double.parseDouble(t5),
                            (long) Double.parseDouble(t6)
                    };
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Los límites de potencia deben ser numéricos", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // ── Fecha de activación ─────────────────────────────────────────────
            Date fechaActivacion = null;
            if (checkBoxFechaAct.isChecked()) {
                String fechaStr = editFechaAct.getText().toString().trim();
                String horaStr  = editHourAct.getText().toString().trim();

                if (TextUtils.isEmpty(fechaStr) || TextUtils.isEmpty(horaStr)) {
                    Toast.makeText(this, "Introduce fecha y hora de activación", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                    fechaActivacion = sdf.parse(fechaStr + " " + horaStr);
                } catch (ParseException e) {
                    Toast.makeText(this, "Formato de fecha inválido. Usa yyyy/MM/dd y HH:mm", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // ── Cierre de facturación ───────────────────────────────────────────
            String cierreMes = null;
            if (checkBoxFechaFact.isChecked()) {
                cierreMes = editFechaFact.getText().toString().trim();

                if (TextUtils.isEmpty(cierreMes)) {
                    Toast.makeText(this, "Introduce fecha de facturación", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validar formato YYYY/MM/DD o FFFF/MM/DD
                if (!cierreMes.matches("(\\d{4}|FFFF)/\\d{2}/\\d{2}")) {
                    Toast.makeText(this, "Formato de cierre inválido. Usa YYYY/MM/DD o FFFF/MM/DD", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // ── Validar que hay algo que escribir ───────────────────────────────
            if (!checkBoxTarif.isChecked() && !checkBoxPowerLimit.isChecked() &&
                    !checkBoxFechaAct.isChecked() && !checkBoxFechaFact.isChecked()) {
                Toast.makeText(this, "Selecciona al menos un parámetro a programar", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── Capturar referencias finales para el hilo ───────────────────────
            final String   tarifaFinal      = tarifaSeleccionada;
            final long[]   thresholdsFinal  = thresholds;
            final Date activacionFinal  = fechaActivacion;
            final String   cierreFinal      = cierreMes;
            final int      contractFinal    = contract;

            // ── Ejecutar en hilo de fondo (DLMS no puede ir en el hilo UI) ──────
            new Thread(() -> {
                GXDLMSReader reader;
                DLMSConnection conn;
                try {

                    if (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH) {
                        conn = new DLMSConnection(config.getBluetoothDeviceName());
                        reader = conn.bluetoothConnnect(ProgramarContratoActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect();
                    }
                    //String cntId = SerialNumberReader.readSerialNumer(reader);

                    ProgramContract.programarContrato(
                            reader,
                            contractFinal,
                            tarifaFinal,
                            thresholdsFinal,
                            activacionFinal,
                            cierreFinal
                    );

                    conn.close();

                    // Volver al hilo UI para mostrar resultado
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Contrato " + contractFinal + " programado correctamente",
                                    Toast.LENGTH_LONG).show()
                    );

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Error al programar contrato: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        });
    }

    private void initViews() {

        // CheckBoxes
        checkBoxTarif = findViewById(R.id.checkBoxTarif);
        checkBoxPowerLimit = findViewById(R.id.checkBoxPowerLimit);
        checkBoxFechaAct = findViewById(R.id.checkBoxFechaAct);
        checkBoxFechaFact = findViewById(R.id.checkBoxFechaFact);

        // Spinners
        spinnerContrato = findViewById(R.id.spinnerContrato);
        spinnerTarifa = findViewById(R.id.spinnerTarif);

        // Potencias
        editPowerLimitT1 = findViewById(R.id.editPowerLimitT1);
        editPowerLimitT2 = findViewById(R.id.editPowerLimitT2);
        editPowerLimitT3 = findViewById(R.id.editPowerLimitT3);
        editPowerLimitT4 = findViewById(R.id.editPowerLimitT4);
        editPowerLimitT5 = findViewById(R.id.editPowerLimitT5);
        editPowerLimitT6 = findViewById(R.id.editPowerLimitT6);

        // Fechas
        editFechaAct = findViewById(R.id.editFechaAct);
        editHourAct = findViewById(R.id.editHourAct);
        editFechaFact = findViewById(R.id.editFechaFact);
    }


}