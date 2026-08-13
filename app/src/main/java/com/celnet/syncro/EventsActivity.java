package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.EventFila;
import com.celnet.syncro.objects.events.CommonEventLog;
import com.celnet.syncro.objects.events.DemandMgmntEventLog;
import com.celnet.syncro.objects.events.DisconnectEventLog;
import com.celnet.syncro.objects.events.ExpPowContractEventLog;
import com.celnet.syncro.objects.events.FinishedPQEventLog;
import com.celnet.syncro.objects.events.FirmwareEventLog;
import com.celnet.syncro.objects.events.FraudEventLog;
import com.celnet.syncro.objects.events.PowContractEventLog;
import com.celnet.syncro.objects.events.PowerQualityEventLog;
import com.celnet.syncro.objects.events.StandarEventLogReader;
import com.celnet.syncro.objects.events.SyncEventLog;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventsActivity extends BaseActivity {

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
        setContentView(R.layout.activity_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ListView listEvents = findViewById(R.id.listEvents);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.events_array,
                android.R.layout.simple_list_item_multiple_choice
        );

        listEvents.setAdapter(adapter);

        EditText editFechaInicio = findViewById(R.id.editFechaInicio);
        EditText editFechaFin = findViewById(R.id.editFechaFin);
        editFechaInicio.setOnClickListener(v -> showDatePicker(editFechaInicio));
        editFechaFin.setOnClickListener(v -> showDatePicker(editFechaFin));

        LinearLayout progressBar = findViewById(R.id.progressContainer);

        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            if (!validarFormulario(
                    editFechaInicio,
                    editFechaFin,
                    listEvents)) {
                return;
            }

            String fechaInicio = editFechaInicio.getText().toString();
            String fechaFin = editFechaFin.getText().toString();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            Toast.makeText(EventsActivity.this,
                    "Conexión: " + config.getType() + "\n" +
                            "IP: " + config.getIp() + "\n" +
                            "Puerto: " + config.getPort() + "\n" +
                            "Dispositivo: " + config.getBluetoothDeviceName() + "\n" +
                            "Fecha Inicio: " + fechaInicio + "\n" +
                            "Fecha Fin: " + fechaFin,
                    Toast.LENGTH_LONG).show();

            // Bloquear interacción mientras carga
            btnNext.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(EventsActivity.this);

                    ArrayList<EventFila> datos = new ArrayList<>();

                    SparseBooleanArray checked = listEvents.getCheckedItemPositions();
                    ArrayList<Integer> eventosSeleccionados = new ArrayList<>();

                    for (int i = 0; i < listEvents.getCount(); i++) {
                        if (checked.get(i)) {
                            eventosSeleccionados.add(i);
                        }
                    }

                    for (Integer event : eventosSeleccionados) {
                        switch (event) {
                            case 0: datos.addAll(StandarEventLogReader.readStandardEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 1: datos.addAll(FraudEventLog.readFraudEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 2: datos.addAll(DisconnectEventLog.readDisconnectEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 3: datos.addAll(PowContractEventLog.readImpPowContractEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 4: datos.addAll(FirmwareEventLog.leerFirmwareEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 5: datos.addAll(PowerQualityEventLog.readPowerQualityEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 6: datos.addAll(DemandMgmntEventLog.readDemandMgmntEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 7: datos.addAll(CommonEventLog.leerCommonEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 8: datos.addAll(SyncEventLog.readSyncEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 9: datos.addAll(FinishedPQEventLog.readFinishedPQEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                            case 10: datos.addAll(ExpPowContractEventLog.readExpPowContractEventLog(this, res.reader, fechaInicio, fechaFin)); break;
                        }
                    }

                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);

                        Intent intent = new Intent(EventsActivity.this, ResultadosEventsActivity.class);
                        intent.putParcelableArrayListExtra("datos_event_tabla", datos);
                        intent.putExtra("cntId", res.serialNumber);
                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);

                        new androidx.appcompat.app.AlertDialog.Builder(EventsActivity.this)
                                .setTitle("Error de lectura")
                                .setMessage("No se pudieron leer los eventos del contador.\n\n"
                                        + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .setCancelable(true)
                                .show();
                    });
                }
            }).start();

        });
    }

    private boolean validarFormulario(EditText editFechaInicio,
                                      EditText editFechaFin,
                                      ListView listEvents) {

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

        // Validar eventos seleccionados
        SparseBooleanArray checked =
                listEvents.getCheckedItemPositions();

        boolean algunoSeleccionado = false;

        for (int i = 0; i < listEvents.getCount(); i++) {
            if (checked.get(i)) {
                algunoSeleccionado = true;
                break;
            }
        }

        if (!algunoSeleccionado) {

            Toast.makeText(
                    this,
                    "Seleccione al menos un tipo de evento",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        return true;
    }
}