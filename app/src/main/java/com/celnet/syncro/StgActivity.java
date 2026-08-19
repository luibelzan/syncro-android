package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class StgActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerAfterGenerate, spinnerAfterSend;

    private EditText editCncName;

    private ExtendedFloatingActionButton btnNext;

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

        // Configurar desplegable 1
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.afterGenerate_array,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerAfterGenerate.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            spinnerAfterGenerate.setText(adapter.getItem(0).toString(), false);
        }

        // Configurar desplegable 2
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,
                R.array.afterSend_array,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerAfterSend.setAdapter(adapter2);
        if (adapter2.getCount() > 0) {
            spinnerAfterSend.setText(adapter2.getItem(0).toString(), false);
        }

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
        editor.putString("afterGenerate", spinnerAfterGenerate.getText().toString());
        editor.putString("afterSend", spinnerAfterSend.getText().toString());

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
            spinnerAfterGenerate.setText(afterGenerate, false);
        }

        // Restaurar spinnerAfterSend
        if (afterSend != null) {
            spinnerAfterSend.setText(afterSend, false);
        }
    }
}