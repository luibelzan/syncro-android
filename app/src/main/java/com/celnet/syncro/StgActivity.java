package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StgActivity extends AppCompatActivity {

    private Spinner spinnerAfterGenerate, spinnerAfterSend;

    private EditText editCncName;

    private ImageButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stg);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerAfterGenerate = findViewById(R.id.spinnerAfterGenerate);
        spinnerAfterSend = findViewById(R.id.spinnerAfterSend);
        editCncName = findViewById(R.id.editCncName);
        btnNext = findViewById(R.id.btnNext);

        //Configurar Spinner 1
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.afterGenerate_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAfterGenerate.setAdapter(adapter);

        spinnerAfterGenerate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // opcional: manejar cambios
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Configurar Spinner 2
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,
                R.array.afterSend_array,
                android.R.layout.simple_spinner_item
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAfterSend.setAdapter(adapter2);

        spinnerAfterSend.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // opcional: manejar cambios
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 🔹 Cargar datos guardados
        cargarConfiguracion();

        // 🔹 Guardar datos al pulsar botón
        btnNext.setOnClickListener(v -> {
            guardarConfiguracion();
            finish();
        });
    }

    private void guardarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("cncName", editCncName.getText().toString());
        editor.putString("afterGenerate", spinnerAfterGenerate.getSelectedItem().toString());
        editor.putString("afterSend", spinnerAfterSend.getSelectedItem().toString());

        editor.apply();
    }

    private void cargarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);

        String cncName = prefs.getString("cncName", "Syncro");
        String afterGenerate = prefs.getString("afterGenerate", null);
        String afterSend = prefs.getString("afterSend", null);

        editCncName.setText(cncName);

        // Restaurar spinnerAfterGenerate
        if (afterGenerate != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerAfterGenerate.getAdapter();
            int position = adapter.getPosition(afterGenerate);
            if (position >= 0) {
                spinnerAfterGenerate.setSelection(position);
            }
        }

        // Restaurar spinnerAfterSend
        if (afterSend != null) {
            ArrayAdapter adapter2 = (ArrayAdapter) spinnerAfterSend.getAdapter();
            int position2 = adapter2.getPosition(afterSend);
            if (position2 >= 0) {
                spinnerAfterSend.setSelection(position2);
            }
        }
    }
}