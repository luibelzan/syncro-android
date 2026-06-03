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

        new Thread(() -> {

            SharedPreferences prefs =
                    getSharedPreferences("ftp_config", MODE_PRIVATE);

            String protocolo =
                    prefs.getString("protocolo", "FTP");

            String afterSend =
                    prefs.getString(
                            "afterSend",
                            "Mover a la carpeta de backup del dispositivo"
                    );

            int enviados = 0;

            for (ReportFile report : lista) {

                if (!report.isSeleccionado()) {
                    continue;
                }

                boolean subidaCorrecta = false;

                try {

                    if ("SFTP".equalsIgnoreCase(protocolo)) {

                        subidaCorrecta =
                                Utils.subirArchivoSFTP(
                                        ReportesActivity.this,
                                        report.getFile()
                                );

                    } else if ("FTPS".equalsIgnoreCase(protocolo)) {

                        subidaCorrecta =
                                Utils.subirArchivoFTPS(
                                        ReportesActivity.this,
                                        report.getFile()
                                );

                    } else {

                        subidaCorrecta =
                                Utils.subirArchivoFTP(
                                        ReportesActivity.this,
                                        report.getFile()
                                );
                    }

                    if (subidaCorrecta) {

                        enviados++;

                        Utils.gestionarArchivoTrasEnvio(
                                report.getFile(),
                                afterSend,
                                ReportesActivity.this
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int totalEnviados = enviados;

            runOnUiThread(() -> {

                Toast.makeText(
                        ReportesActivity.this,
                        totalEnviados + " reportes enviados",
                        Toast.LENGTH_LONG
                ).show();

                lista.clear();
                cargarReportes();

                RecyclerView rv = findViewById(R.id.rvReportes);
                rv.setAdapter(new ReportesAdapter(lista));
            });

        }).start();
    }
}