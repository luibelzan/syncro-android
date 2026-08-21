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

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.curvas.CurvaCorrienteFila;
import com.celnet.syncro.models.curvas.CurvaEnergiaFaseFila;
import com.celnet.syncro.models.curvas.CurvaFila;
import com.celnet.syncro.models.curvas.CurvaVoltajeFila;
import com.celnet.syncro.models.curvas.TipoCurva;
import com.celnet.syncro.objects.loadProfiles.LoadProfileReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CurvasActivity extends BaseActivity {

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Valor interno (el que esperan los métodos de lectura DLMS): sin tocar
                    String fechaInterna = selectedYear + "/" +
                            String.format(Locale.US, "%02d", selectedMonth + 1) + "/" +
                            String.format(Locale.US, "%02d", selectedDay);

                    // Valor mostrado en pantalla: dd/MM/yyyy
                    String fechaMostrada = String.format(Locale.US, "%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);

                    editText.setText(fechaMostrada);
                    editText.setTag(fechaInterna);
                },
                year, month, day);
        datePickerDialog.show();
    }

    /** Devuelve la fecha en formato interno yyyy/MM/dd guardada en el tag del EditText. */
    private String obtenerFechaInterna(EditText editText) {
        Object tag = editText.getTag();
        return tag != null ? tag.toString() : "";
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        View mainView = findViewById(R.id.main);
        mainView.requestApplyInsets();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_curvas);
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

        AutoCompleteTextView spinnerTipoCurva = findViewById(R.id.spinnerTipoCurva);
        TipoCurva[] tipos = TipoCurva.values();
        ArrayAdapter<TipoCurva> adapterTipos = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tipos);
        spinnerTipoCurva.setAdapter(adapterTipos);
        spinnerTipoCurva.setText(tipos[0].toString(), false);

        btnNext.setOnClickListener(v -> {

            if (!validarFechas(editFechaInicio, editFechaFin)) {
                return;
            }

            TipoCurva tipoSeleccionado = tipos[0];
            for (TipoCurva t : tipos) {
                if (t.toString().equals(spinnerTipoCurva.getText().toString())) {
                    tipoSeleccionado = t;
                    break;
                }
            }
            TipoCurva tipoFinal = tipoSeleccionado;

            String fechaInicio = obtenerFechaInterna(editFechaInicio);
            String fechaFin = obtenerFechaInterna(editFechaFin);
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            btnNext.setEnabled(false);
            editFechaInicio.setEnabled(false);
            editFechaFin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(CurvasActivity.this);

                    Intent intent = new Intent(CurvasActivity.this, ResultadosCurvasActivity.class);
                    intent.putExtra("tipoCurva", tipoFinal.name());
                    intent.putExtra("cntId", res.serialNumber);

                    if (tipoFinal == TipoCurva.INCREMENTAL_S02) {
                        ArrayList<CurvaFila> datos = LoadProfileReader.leerCurvaCarga(res.reader, fechaInicio, fechaFin);
                        intent.putParcelableArrayListExtra("datos_curva_tabla", datos);
                    } else if (tipoFinal == TipoCurva.VOLTAGE_S44) {
                        ArrayList<CurvaVoltajeFila> datos = LoadProfileReader.leerCurvaVoltaje(res.reader, fechaInicio, fechaFin);
                        intent.putParcelableArrayListExtra("datos_curva_voltaje", datos);
                    } else if (tipoFinal == TipoCurva.CURRENT_S45) {
                        ArrayList<CurvaCorrienteFila> datos = LoadProfileReader.leerCurvaCorriente(res.reader, fechaInicio, fechaFin);
                        intent.putParcelableArrayListExtra("datos_curva_corriente", datos);
                    } else {
                        ArrayList<CurvaEnergiaFaseFila> datos = LoadProfileReader.leerCurvaEnergiaPorFase(res.reader, fechaInicio, fechaFin);
                        intent.putParcelableArrayListExtra("datos_curva_energia_fase", datos);
                    }

                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        editFechaInicio.setEnabled(true);
                        editFechaFin.setEnabled(true);
                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);
                        editFechaInicio.setEnabled(true);
                        editFechaFin.setEnabled(true);

                        new androidx.appcompat.app.AlertDialog.Builder(CurvasActivity.this)
                                .setTitle("Error de lectura")
                                .setMessage("No se pudo leer la curva seleccionada.\n\n" + e.getMessage())
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

    private boolean validarFechas(EditText editFechaInicio,
                                  EditText editFechaFin) {

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

            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy/MM/dd", Locale.US);

            sdf.setLenient(false);

            Date inicio = sdf.parse(fechaInicio);
            Date fin = sdf.parse(fechaFin);

            if (inicio.after(fin)) {

                editFechaInicio.setError(
                        "La fecha de inicio no puede ser posterior a la fecha fin");

                return false;
            }

        } catch (ParseException e) {

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