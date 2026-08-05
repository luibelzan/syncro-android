package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Arrays;
import java.util.List;

public class PrimeSeguridadActivity extends BaseActivity {

    private MaterialCheckBox cbMaster;
    private List<MaterialCheckBox> bitCheckBoxes;

    // Evita bucles infinitos entre el listener del maestro y el de los hijos
    private boolean actualizandoDesdeMaster = false;

    private final CompoundButton.OnCheckedChangeListener listenerMaster =
            (buttonView, isChecked) -> {
                actualizandoDesdeMaster = true;
                for (MaterialCheckBox cb : bitCheckBoxes) {
                    cb.setChecked(isChecked);
                }
                actualizandoDesdeMaster = false;
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prime_seguridad);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cbMaster = findViewById(R.id.cbConstellationMaster);

        bitCheckBoxes = Arrays.asList(
                (MaterialCheckBox) findViewById(R.id.cbBit0),
                (MaterialCheckBox) findViewById(R.id.cbBit1),
                (MaterialCheckBox) findViewById(R.id.cbBit2),
                (MaterialCheckBox) findViewById(R.id.cbBit3),
                (MaterialCheckBox) findViewById(R.id.cbBit4),
                (MaterialCheckBox) findViewById(R.id.cbBit5),
                (MaterialCheckBox) findViewById(R.id.cbBit6),
                (MaterialCheckBox) findViewById(R.id.cbBit7),
                (MaterialCheckBox) findViewById(R.id.cbBit8),
                (MaterialCheckBox) findViewById(R.id.cbBit9),
                (MaterialCheckBox) findViewById(R.id.cbBit10),
                (MaterialCheckBox) findViewById(R.id.cbBit11),
                (MaterialCheckBox) findViewById(R.id.cbBit12),
                (MaterialCheckBox) findViewById(R.id.cbBit13),
                (MaterialCheckBox) findViewById(R.id.cbBit14),
                (MaterialCheckBox) findViewById(R.id.cbBit15)
        );

        actualizarEstadoMaster();

        // El maestro marca/desmarca todos los hijos
        cbMaster.setOnCheckedChangeListener(listenerMaster);

        // Cada hijo, al cambiar, recalcula el estado del maestro
        CompoundButton.OnCheckedChangeListener listenerHijo = (buttonView, isChecked) -> {
            if (!actualizandoDesdeMaster) {
                actualizarEstadoMaster();
            }
        };
        for (MaterialCheckBox cb : bitCheckBoxes) {
            cb.setOnCheckedChangeListener(listenerHijo);
        }

        // Desplegable de versión Dual Stack Prime
        AutoCompleteTextView spinnerDualStackVersion = findViewById(R.id.spinnerDualStackVersion);
        String[] opcionesDualStack = getResources().getStringArray(R.array.dual_stack_prime_version_array);
        ArrayAdapter<String> adapterDualStack = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, opcionesDualStack);
        spinnerDualStackVersion.setAdapter(adapterDualStack);
        // Por defecto: "3: Dynamic communications - 1.3.6 or 1.4"
        spinnerDualStackVersion.setText(opcionesDualStack[2], false);

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> {
            int mascara = 0;
            for (int i = 0; i < bitCheckBoxes.size(); i++) {
                if (bitCheckBoxes.get(i).isChecked()) {
                    mascara |= (1 << i);
                }
            }
            String versionSeleccionada = spinnerDualStackVersion.getText().toString();

            // TODO: sustituir por la escritura real sobre el objeto DLMS correspondiente
            Toast.makeText(this,
                    "Máscara constelación: " + mascara + "\nVersión: " + versionSeleccionada,
                    Toast.LENGTH_LONG).show();
        });
    }

    private void actualizarEstadoMaster() {
        boolean todosMarcados = true;
        for (MaterialCheckBox cb : bitCheckBoxes) {
            if (!cb.isChecked()) {
                todosMarcados = false;
                break;
            }
        }
        cbMaster.setOnCheckedChangeListener(null);
        cbMaster.setChecked(todosMarcados);
        cbMaster.setOnCheckedChangeListener(listenerMaster);
    }
}