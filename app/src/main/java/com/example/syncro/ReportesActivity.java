package com.example.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.ReportesAdapter;
import com.example.syncro.models.ReportFile;
import com.example.syncro.utils.Utils;

import java.io.File;
import java.util.ArrayList;

public class ReportesActivity extends BaseActivity {

    private ArrayList<ReportFile> lista = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        RecyclerView rv = findViewById(R.id.rvReportes);
        Button btnEnviar = findViewById(R.id.btnEnviar);

        rv.setLayoutManager(new LinearLayoutManager(this));

        cargarReportes();

        ReportesAdapter adapter = new ReportesAdapter(lista);
        rv.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviarSeleccionados());
    }

    private void cargarReportes() {

        File downloads =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);

        File reportsFolder = new File(downloads, "Syncro/Reports");

        if (reportsFolder.exists()) {

            File[] archivos = reportsFolder.listFiles();

            if (archivos != null) {

                for (File file : archivos) {

                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        lista.add(new ReportFile(file));
                    }
                }
            }
        }
    }

    private void enviarSeleccionados() {

        SharedPreferences prefs =
                getSharedPreferences("ftp_config", MODE_PRIVATE);

        String protocolo = prefs.getString("protocolo", "FTP");

        int enviados = 0;

        for (ReportFile report : lista) {

            if (report.isSeleccionado()) {

                enviados++;

                if (protocolo.equalsIgnoreCase("SFTP")) {

                    Utils.subirArchivoSFTP(this, report.getFile());

                } else if (protocolo.equalsIgnoreCase("FTPS")) {

                    Utils.subirArchivoFTPS(this, report.getFile());

                } else {

                    Utils.subirArchivoFTP(this, report.getFile());
                }
            }
        }

        Toast.makeText(this,
                enviados + " reportes enviados",
                Toast.LENGTH_SHORT).show();
    }
}