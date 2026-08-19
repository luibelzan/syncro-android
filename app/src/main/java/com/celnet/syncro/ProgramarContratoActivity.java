package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.objects.contracts.ProgramContract;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProgramarContratoActivity extends AppCompatActivity {

    // CheckBoxes
    private CheckBox checkBoxTarif;
    private CheckBox checkBoxPowerLimit;
    private CheckBox checkBoxFechaAct;
    private CheckBox checkBoxFechaFact;

    // Desplegables
    private AutoCompleteTextView spinnerContrato;
    private AutoCompleteTextView spinnerTarifa;
    private String[] contratosOptions;
    private String[] tarifasOptions;

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
    private EditText editFechaFactMonth;
    private EditText editFechaFactDay;
    private FrameLayout progressOverlay;


    /** Devuelve la fecha en formato interno yyyy/MM/dd guardada en el tag del EditText. */
    private String obtenerFechaInterna(EditText editText) {
        Object tag = editText.getTag();
        return tag != null ? tag.toString() : "";
    }

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

                        // Valor interno (el que espera ProgramContract.programarContrato): sin tocar
                        String fechaInterna = String.format(
                                Locale.getDefault(),
                                "%04d/%02d/%02d",
                                selectedYear,
                                selectedMonth + 1,
                                selectedDay
                        );

                        // Valor mostrado en pantalla: dd/MM/yyyy
                        String fechaMostrada = String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%04d",
                                selectedDay,
                                selectedMonth + 1,
                                selectedYear
                        );

                        editFechaAct.setText(fechaMostrada);
                        editFechaAct.setTag(fechaInterna);

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
        // Mes de facturación
        editFechaFactMonth.setOnClickListener(v -> {
            String[] meses = {
                    "01","02","03","04","05","06",
                    "07","08","09","10","11","12",
                    "FD","FE","FF"
            };
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Selecciona mes")
                    .setItems(meses, (dialog, which) ->
                            editFechaFactMonth.setText(meses[which]))
                    .show();
        });

        // Día de facturación
        editFechaFactDay.setOnClickListener(v -> {
            String[] dias = new String[34];
            for (int i = 0; i < 31; i++) {
                dias[i] = String.format(Locale.getDefault(), "%02d", i + 1);
            }
            dias[31] = "FD";
            dias[32] = "FE";
            dias[33] = "FF";

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Selecciona día")
                    .setItems(dias, (dialog, which) ->
                            editFechaFactDay.setText(dias[which]))
                    .show();
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

        // Desplegable de contrato
        spinnerContrato = findViewById(R.id.spinnerContrato);
        contratosOptions = getResources().getStringArray(R.array.program_contracts_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, contratosOptions);
        spinnerContrato.setAdapter(adapter);
        if (contratosOptions.length > 0) {
            spinnerContrato.setText(contratosOptions[0], false);
        }

        // Desplegable de tarifa
        spinnerTarifa = findViewById(R.id.spinnerTarif);
        tarifasOptions = getResources().getStringArray(R.array.tarifas_array);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tarifasOptions);
        spinnerTarifa.setAdapter(adapter2);
        if (tarifasOptions.length > 0) {
            spinnerTarifa.setText(tarifasOptions[0], false);
        }

        initViews();
        configurarPickers();

        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // ── Contrato ────────────────────────────────────────────────────────
            int posicionContrato = Arrays.asList(contratosOptions)
                    .indexOf(spinnerContrato.getText().toString());
            int contract = (posicionContrato >= 0 ? posicionContrato : 0) + 1;

            // ── Tarifa ──────────────────────────────────────────────────────────
            String tarifaSeleccionada = null;
            if (checkBoxTarif.isChecked()) {
                tarifaSeleccionada = spinnerTarifa.getText().toString();
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
                String fechaStr = obtenerFechaInterna(editFechaAct);
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
                String month = editFechaFactMonth.getText().toString().trim();
                String day   = editFechaFactDay.getText().toString().trim();

                if (TextUtils.isEmpty(month) || TextUtils.isEmpty(day)) {
                    Toast.makeText(this, "Selecciona mes y día de facturación", Toast.LENGTH_SHORT).show();
                    return;
                }

                cierreMes = "FFFF/" + month + "/" + day;
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
                showProgress(true);
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(ProgramarContratoActivity.this);


                    ProgramContract.programarContrato(
                            res.reader,
                            contractFinal,
                            tarifaFinal,
                            thresholdsFinal,
                            activacionFinal,
                            cierreFinal
                    );

                    conn.close();
                    showProgress(false);

                    // Volver al hilo UI para mostrar resultado
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Contrato " + contractFinal + " programado correctamente",
                                    Toast.LENGTH_LONG).show()
                    );

                } catch (Exception e) {
                    showProgress(false);
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Error al programar contrato: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                } finally {
                    conn.close();
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
        // Fechas facturación
        editFechaFactMonth = findViewById(R.id.editFechaFactMonth);
        editFechaFactDay   = findViewById(R.id.editFechaFactDay);
        progressOverlay = findViewById(R.id.progressOverlay);
    }

    private void showProgress(boolean show) {
        runOnUiThread(() ->
                progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE)
        );
    }


}