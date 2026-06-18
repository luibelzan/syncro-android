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

public class FtpActivity extends BaseActivity {

    private Spinner spinnerProtocolo;
    private EditText editDirFtp, editPortFtp, editFolderFtp, editUserFtp, editPassFtp;
    private ImageButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ftp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencias UI
        spinnerProtocolo = findViewById(R.id.spinnerProtocolo);
        editDirFtp = findViewById(R.id.editDirFtp);
        editPortFtp = findViewById(R.id.editPortFtp);
        editFolderFtp = findViewById(R.id.editFolderFtp);
        editUserFtp = findViewById(R.id.editUserFtp);
        editPassFtp = findViewById(R.id.editPassFtp);
        btnNext = findViewById(R.id.btnNext);

        // Configurar Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.protocolo_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocolo.setAdapter(adapter);

        spinnerProtocolo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        editor.putString("protocolo", spinnerProtocolo.getSelectedItem().toString());
        editor.putString("dir", editDirFtp.getText().toString());
        editor.putString("port", editPortFtp.getText().toString());
        editor.putString("folder", editFolderFtp.getText().toString());
        editor.putString("user", editUserFtp.getText().toString());
        editor.putString("pass", editPassFtp.getText().toString());

        editor.apply();
    }

    private void cargarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);

        String protocolo = prefs.getString("protocolo", "");
        String dir = prefs.getString("dir", "");
        String port = prefs.getString("port", "22");
        String folder = prefs.getString("folder", "/");
        String user = prefs.getString("user", "");
        String pass = prefs.getString("pass", "");

        editDirFtp.setText(dir);
        editPortFtp.setText(port);
        editFolderFtp.setText(folder);
        editUserFtp.setText(user);
        editPassFtp.setText(pass);

        // Seleccionar protocolo en spinner
        ArrayAdapter adapter = (ArrayAdapter) spinnerProtocolo.getAdapter();
        int position = adapter.getPosition(protocolo);
        if (position >= 0) {
            spinnerProtocolo.setSelection(position);
        }
    }
}