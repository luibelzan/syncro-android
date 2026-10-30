package com.celnet.syncro;

import android.content.Context;
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

/**
 * Configuración del FTP/SFTP para IEC-870. Igual que FtpActivity (DLMS), pero guarda sus datos en
 * un fichero de preferencias propio ("iec_ftp_config"), para que ambas configuraciones sean
 * independientes. Quien suba ficheros por FTP en IEC debe leerlas con {@link #cargar(Context)}.
 */
public class IecFtpActivity extends BaseActivity {

    public static final String PREFS_NAME = "iec_ftp_config";

    /** Valores guardados de la configuración FTP de IEC. */
    public static class Config {
        public String protocolo = "";
        public String dir = "";
        public String port = "22";
        public String folder = "/";
        public String user = "";
        public String pass = "";
    }

    /** Lee la configuración FTP de IEC (con los mismos valores por defecto que la pantalla). */
    public static Config cargar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Config c = new Config();
        c.protocolo = prefs.getString("protocolo", "");
        c.dir = prefs.getString("dir", "");
        c.port = prefs.getString("port", "22");
        c.folder = prefs.getString("folder", "/");
        c.user = prefs.getString("user", "");
        c.pass = prefs.getString("pass", "");
        return c;
    }

    private AutoCompleteTextView spinnerProtocolo;
    private EditText editDirFtp, editPortFtp, editFolderFtp, editUserFtp, editPassFtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_ftp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerProtocolo = findViewById(R.id.spinnerProtocolo);
        editDirFtp = findViewById(R.id.editDirFtp);
        editPortFtp = findViewById(R.id.editPortFtp);
        editFolderFtp = findViewById(R.id.editFolderFtp);
        editUserFtp = findViewById(R.id.editUserFtp);
        editPassFtp = findViewById(R.id.editPassFtp);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.protocolo_array, android.R.layout.simple_dropdown_item_1line);
        spinnerProtocolo.setAdapter(adapter);

        mostrarConfiguracion();

        btnNext.setOnClickListener(v -> {
            guardarConfiguracion();
            finish();
        });
    }

    private void guardarConfiguracion() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString("protocolo", spinnerProtocolo.getText().toString())
                .putString("dir", editDirFtp.getText().toString())
                .putString("port", editPortFtp.getText().toString())
                .putString("folder", editFolderFtp.getText().toString())
                .putString("user", editUserFtp.getText().toString())
                .putString("pass", editPassFtp.getText().toString())
                .apply();
    }

    private void mostrarConfiguracion() {
        Config c = cargar(this);
        editDirFtp.setText(c.dir);
        editPortFtp.setText(c.port);
        editFolderFtp.setText(c.folder);
        editUserFtp.setText(c.user);
        editPassFtp.setText(c.pass);
        if (!c.protocolo.isEmpty()) {
            spinnerProtocolo.setText(c.protocolo, false);
        }
    }
}