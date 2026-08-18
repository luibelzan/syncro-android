package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
                            String.format(Locale.US, "%02d", selectedMonth + 1) + "/" +
                            String.format(Locale.US, "%02d", selectedDay);
                    editText.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
    }

    /**
     * Alterna la visibilidad de un grupo desplegable con una animación sencilla
     * y rota la flecha del botón que lo controla.
     */
    private void alternarDespliegue(ViewGroup contenedorAnimado, View grupo, ImageButton flecha) {
        boolean expandiendo = grupo.getVisibility() != View.VISIBLE;
        TransitionManager.beginDelayedTransition(contenedorAnimado, new AutoTransition());
        grupo.setVisibility(expandiendo ? View.VISIBLE : View.GONE);
        flecha.animate().rotation(expandiendo ? 180f : 0f).setDuration(200).start();
    }

    /**
     * Sincroniza un checkbox "maestro" de categoría con sus checkboxes hijos:
     * marcar/desmarcar el maestro marca/desmarca todos los hijos, y marcar/desmarcar
     * cualquier hijo recalcula el estado del maestro (marcado solo si están todos).
     */
    private void sincronizarGrupo(MaterialCheckBox master, List<MaterialCheckBox> hijos) {
        final boolean[] actualizandoDesdeMaster = {false};

        CompoundButton.OnCheckedChangeListener listenerMaster = (buttonView, isChecked) -> {
            actualizandoDesdeMaster[0] = true;
            for (MaterialCheckBox cb : hijos) {
                cb.setChecked(isChecked);
            }
            actualizandoDesdeMaster[0] = false;
        };
        master.setOnCheckedChangeListener(listenerMaster);

        CompoundButton.OnCheckedChangeListener listenerHijo = (buttonView, isChecked) -> {
            if (actualizandoDesdeMaster[0]) return;
            boolean todosMarcados = true;
            for (MaterialCheckBox cb : hijos) {
                if (!cb.isChecked()) {
                    todosMarcados = false;
                    break;
                }
            }
            master.setOnCheckedChangeListener(null);
            master.setChecked(todosMarcados);
            master.setOnCheckedChangeListener(listenerMaster);
        };
        for (MaterialCheckBox cb : hijos) {
            cb.setOnCheckedChangeListener(listenerHijo);
        }
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

        // ===== Estándar (desplegable) =====
        ViewGroup wrapperEstandar = findViewById(R.id.wrapperEstandar);
        MaterialCheckBox cbEstandar = findViewById(R.id.cbEstandar);
        View groupEstandar = findViewById(R.id.groupEstandar);
        ImageButton btnExpandEstandar = findViewById(R.id.btnExpandEstandar);
        MaterialCheckBox cbStandard = findViewById(R.id.cbStandard);
        MaterialCheckBox cbImpPowContract = findViewById(R.id.cbImpPowContract);
        MaterialCheckBox cbFirmware = findViewById(R.id.cbFirmware);
        MaterialCheckBox cbSync = findViewById(R.id.cbSync);
        MaterialCheckBox cbExpPowContract = findViewById(R.id.cbExpPowContract);

        btnExpandEstandar.setOnClickListener(v ->
                alternarDespliegue(wrapperEstandar, groupEstandar, btnExpandEstandar));
        sincronizarGrupo(cbEstandar, Arrays.asList(
                cbStandard, cbImpPowContract, cbFirmware, cbSync, cbExpPowContract));

        // ===== Calidad (desplegable) =====
        ViewGroup wrapperCalidad = findViewById(R.id.wrapperCalidad);
        MaterialCheckBox cbCalidad = findViewById(R.id.cbCalidad);
        View groupCalidad = findViewById(R.id.groupCalidad);
        ImageButton btnExpandCalidad = findViewById(R.id.btnExpandCalidad);
        MaterialCheckBox cbPowerQuality = findViewById(R.id.cbPowerQuality);
        MaterialCheckBox cbFinishedPQ = findViewById(R.id.cbFinishedPQ);

        btnExpandCalidad.setOnClickListener(v ->
                alternarDespliegue(wrapperCalidad, groupCalidad, btnExpandCalidad));
        sincronizarGrupo(cbCalidad, Arrays.asList(cbPowerQuality, cbFinishedPQ));

        // ===== Categorías simples (1 subgrupo = la propia categoría) =====
        MaterialCheckBox cbIcp = findViewById(R.id.cbIcp);
        MaterialCheckBox cbFraude = findViewById(R.id.cbFraude);
        MaterialCheckBox cbDemanda = findViewById(R.id.cbDemanda);
        MaterialCheckBox cbComunicaciones = findViewById(R.id.cbComunicaciones);

        EditText editFechaInicio = findViewById(R.id.editFechaInicio);
        EditText editFechaFin = findViewById(R.id.editFechaFin);
        editFechaInicio.setOnClickListener(v -> showDatePicker(editFechaInicio));
        editFechaFin.setOnClickListener(v -> showDatePicker(editFechaFin));

        LinearLayout progressBar = findViewById(R.id.progressContainer);

        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        // Todos los checkboxes hoja (11), para validar que al menos uno esté marcado
        List<MaterialCheckBox> checkboxesHoja = Arrays.asList(
                cbStandard, cbFraude, cbIcp, cbImpPowContract, cbFirmware,
                cbPowerQuality, cbDemanda, cbComunicaciones, cbSync, cbFinishedPQ, cbExpPowContract);

        btnNext.setOnClickListener(v -> {

            if (!validarFormulario(editFechaInicio, editFechaFin, checkboxesHoja)) {
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

            // Cada checkbox hoja marcado añade su índice original, con flexibilidad total
            List<Integer> eventosSeleccionados = new ArrayList<>();
            if (cbStandard.isChecked()) eventosSeleccionados.add(0);
            if (cbFraude.isChecked()) eventosSeleccionados.add(1);
            if (cbIcp.isChecked()) eventosSeleccionados.add(2);
            if (cbImpPowContract.isChecked()) eventosSeleccionados.add(3);
            if (cbFirmware.isChecked()) eventosSeleccionados.add(4);
            if (cbPowerQuality.isChecked()) eventosSeleccionados.add(5);
            if (cbDemanda.isChecked()) eventosSeleccionados.add(6);
            if (cbComunicaciones.isChecked()) eventosSeleccionados.add(7);
            if (cbSync.isChecked()) eventosSeleccionados.add(8);
            if (cbFinishedPQ.isChecked()) eventosSeleccionados.add(9);
            if (cbExpPowContract.isChecked()) eventosSeleccionados.add(10);

            new Thread(() -> {
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(EventsActivity.this);

                    ArrayList<EventFila> datos = new ArrayList<>();

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
                                      List<MaterialCheckBox> checkboxesHoja) {

        String fechaInicio = editFechaInicio.getText().toString().trim();
        String fechaFin = editFechaFin.getText().toString().trim();

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
                Toast.makeText(this,
                        "La fecha inicio no puede ser posterior a la fecha fin",
                        Toast.LENGTH_LONG).show();
                return false;
            }

        } catch (Exception e) {
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean algunaSeleccionada = false;
        for (MaterialCheckBox cb : checkboxesHoja) {
            if (cb.isChecked()) {
                algunaSeleccionada = true;
                break;
            }
        }

        if (!algunaSeleccionada) {
            Toast.makeText(this, "Seleccione al menos un tipo de evento", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}