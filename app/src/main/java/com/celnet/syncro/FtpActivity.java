package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class FtpActivity extends BaseActivity {

    private AutoCompleteTextView spinnerProtocolo;
    private EditText editDirFtp, editPortFtp, editFolderFtp, editUserFtp, editPassFtp;
    private ExtendedFloatingActionButton btnNext;

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

        // Configurar desplegable de protocolo
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.protocolo_array,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerProtocolo.setAdapter(adapter);

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

        editor.putString("protocolo", spinnerProtocolo.getText().toString());
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

        // Seleccionar protocolo en el desplegable
        if (!protocolo.isEmpty()) {
            spinnerProtocolo.setText(protocolo, false);
        }
    }
}