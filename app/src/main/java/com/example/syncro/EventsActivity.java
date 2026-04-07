package com.example.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CurvaFila;
import com.example.syncro.models.EventFila;
import com.example.syncro.objects.events.CommonEventLog;
import com.example.syncro.objects.events.DemandMgmntEventLog;
import com.example.syncro.objects.events.DisconnectEventLog;
import com.example.syncro.objects.events.ExpPowContractEventLog;
import com.example.syncro.objects.events.FinishedPQEventLog;
import com.example.syncro.objects.events.FirmwareEventLog;
import com.example.syncro.objects.events.FraudEventLog;
import com.example.syncro.objects.events.PowContractEventLog;
import com.example.syncro.objects.events.PowerQualityEventLog;
import com.example.syncro.objects.events.StandarEventLogReader;
import com.example.syncro.objects.events.SyncEventLog;
import com.example.syncro.objects.loadProfiles.LoadProfileReader;
import com.example.syncro.objects.params.SerialNumberReader;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
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

            String fechaInicio = editFechaInicio.getText().toString();
            String fechaFin = editFechaFin.getText().toString();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // Mostrar valores en Toast de depuración
            Toast.makeText(EventsActivity.this,
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
                        reader = conn.bluetoothConnnect(EventsActivity.this);
                    } else {
                        conn = new DLMSConnection(config.getIp(), config.getPort());
                        reader = conn.tcpConnect();
                    }

                    ArrayList<EventFila> datos = new ArrayList<>();
                    String cntId = SerialNumberReader.readSerialNumer(reader);

                    SparseBooleanArray checked = listEvents.getCheckedItemPositions();
                    ArrayList<Integer> eventosSeleccionados = new ArrayList<>();

                    for (int i = 0; i < listEvents.getCount(); i++) {
                        if (checked.get(i)) {
                            eventosSeleccionados.add(i);
                        }
                    }

                    for(Integer event : eventosSeleccionados){

                        switch(event){

                            case 0:
                                datos.addAll(StandarEventLogReader.readStandardEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 1:
                                datos.addAll(FraudEventLog.readFraudEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 2:
                                datos.addAll(DisconnectEventLog.readDisconnectEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 3:
                                datos.addAll(PowContractEventLog.readImpPowContractEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 4:
                                datos.addAll(FirmwareEventLog.leerFirmwareEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 5:
                                datos.addAll(PowerQualityEventLog.readPowerQualityEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 6:
                                datos.addAll(DemandMgmntEventLog.readDemandMgmntEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 7:
                                datos.addAll(CommonEventLog.leerCommonEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 8:
                                datos.addAll(SyncEventLog.readSyncEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 9:
                                datos.addAll(FinishedPQEventLog.readFinishedPQEventLog(this, reader, fechaInicio, fechaFin));
                                break;

                            case 10:
                                datos.addAll(ExpPowContractEventLog.readExpPowContractEventLog(this, reader, fechaInicio, fechaFin));
                                break;
                        }
                    }

                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        Intent intent = new Intent(EventsActivity.this, ResultadosEventsActivity.class);
                        intent.putParcelableArrayListExtra("datos_event_tabla", datos);
                        intent.putExtra("cntId", cntId);
                        startActivity(intent);

                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EventsActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        });
    }
}