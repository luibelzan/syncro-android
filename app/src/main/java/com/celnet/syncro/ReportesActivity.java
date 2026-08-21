package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.ReportesAdapter;
import com.celnet.syncro.models.ReportFile;
import com.celnet.syncro.utils.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;

public class ReportesActivity extends BaseActivity {

    private ArrayList<ReportFile> lista = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reportes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvReportes);
        ExtendedFloatingActionButton btnEnviar = findViewById(R.id.btnEnviar);

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

                    if (file.isFile()) {
                        lista.add(new ReportFile(file));
                    }
                }
            }
        }
    }

    private void enviarSeleccionados() {

        boolean algunoSeleccionado = false;
        for (ReportFile report : lista) {
            if (report.isSeleccionado()) {
                algunoSeleccionado = true;
                break;
            }
        }

        if (!algunoSeleccionado) {
            Toast.makeText(this, "Selecciona al menos un reporte", Toast.LENGTH_SHORT).show();
            return;
        }

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