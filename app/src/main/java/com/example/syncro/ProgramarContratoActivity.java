package com.example.syncro;

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
        Spinner spinnerContrato = findViewById(R.id.spinnerContrato);
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
        Spinner spinnerTarifa = findViewById(R.id.spinnerTarif);
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

        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            // Contrato seleccionado
            String contratoSeleccionado =
                    spinnerContrato.getSelectedItem().toString();

            //Tarifa seleccionada
            String tarifaSeleccionada = null;

            if (checkBoxTarif.isChecked()) {

                tarifaSeleccionada =
                        spinnerTarifa.getSelectedItem().toString();
            }

            //Limites de potencia
            if (checkBoxPowerLimit.isChecked()) {

                String t1 = editPowerLimitT1.getText().toString().trim();
                String t2 = editPowerLimitT2.getText().toString().trim();
                String t3 = editPowerLimitT3.getText().toString().trim();
                String t4 = editPowerLimitT4.getText().toString().trim();
                String t5 = editPowerLimitT5.getText().toString().trim();
                String t6 = editPowerLimitT6.getText().toString().trim();

                // Validación opcional
                if (TextUtils.isEmpty(t1) ||
                        TextUtils.isEmpty(t2) ||
                        TextUtils.isEmpty(t3) ||
                        TextUtils.isEmpty(t4) ||
                        TextUtils.isEmpty(t5) ||
                        TextUtils.isEmpty(t6)) {

                    Toast.makeText(
                            this,
                            "Completa todos los límites de potencia",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                double potenciaT1 = Double.parseDouble(t1);
                double potenciaT2 = Double.parseDouble(t2);
                double potenciaT3 = Double.parseDouble(t3);
                double potenciaT4 = Double.parseDouble(t4);
                double potenciaT5 = Double.parseDouble(t5);
                double potenciaT6 = Double.parseDouble(t6);

            }


            //Fecha de activacion
            if (checkBoxFechaAct.isChecked()) {
                String fechaActivacion =
                        editFechaAct.getText().toString().trim();

                String horaActivacion =
                        editHourAct.getText().toString().trim();

                if (TextUtils.isEmpty(fechaActivacion) ||
                        TextUtils.isEmpty(horaActivacion)) {

                    Toast.makeText(
                            this,
                            "Introduce fecha y hora de activación",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                String fechaHoraActivacion = fechaActivacion + " " + horaActivacion;
            }

            //Cierre de facturacion
            if (checkBoxFechaFact.isChecked()) {

                String fechaFacturacion =
                        editFechaFact.getText().toString().trim();

                if (TextUtils.isEmpty(fechaFacturacion)) {
                    Toast.makeText(
                            this,
                            "Introduce fecha de facturación",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
            }


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