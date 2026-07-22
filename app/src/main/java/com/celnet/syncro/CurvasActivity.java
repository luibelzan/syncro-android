package com.celnet.syncro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.CurvaFila;
import com.celnet.syncro.objects.loadProfiles.LoadProfileReader;
import com.celnet.syncro.objects.params.SerialNumberReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

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
                    String date = selectedYear + "/" +
                            String.format(Locale.US,"%02d", selectedMonth + 1) + "/" +
                            String.format(Locale.US,"%02d", selectedDay);
                    editText.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
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

        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            if (!validarFechas(editFechaInicio, editFechaFin)) {
                return;
            }

            String fechaInicio = editFechaInicio.getText().toString();
            String fechaFin = editFechaFin.getText().toString();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            // Mostrar valores en Toast de depuración
            Toast.makeText(CurvasActivity.this,
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
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(CurvasActivity.this);

                    // Leer curvas
                    ArrayList<CurvaFila> datos = LoadProfileReader.leerCurvaCarga(res.reader, fechaInicio, fechaFin);
                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        Intent intent = new Intent(CurvasActivity.this, ResultadosCurvasActivity.class);
                        intent.putParcelableArrayListExtra("datos_curva_tabla", datos);
                        intent.putExtra("cntId", res.serialNumber);
                        startActivity(intent);

                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CurvasActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        });
    }

    private boolean validarFechas(EditText editFechaInicio,
                                  EditText editFechaFin) {

        String fechaInicio = editFechaInicio.getText().toString().trim();
        String fechaFin = editFechaFin.getText().toString().trim();

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