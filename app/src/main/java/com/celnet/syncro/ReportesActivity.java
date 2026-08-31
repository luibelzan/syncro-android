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
    private ReportesAdapter adapter;

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

        adapter = new ReportesAdapter(lista);
        rv.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviarSeleccionados(btnEnviar));
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

    private void enviarSeleccionados(ExtendedFloatingActionButton btnEnviar) {

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

        btnEnviar.setEnabled(false);

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
            int fallidos = 0;

            for (int i = 0; i < lista.size(); i++) {
                ReportFile report = lista.get(i);
                if (!report.isSeleccionado()) {
                    continue;
                }

                final int index = i;

                // Marca "subiendo" y refresca esa fila antes de intentar el envío
                report.setEstado(ReportFile.EstadoEnvio.SUBIENDO);
                runOnUiThread(() -> adapter.notifyItemChanged(index));

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

                    } else {
                        fallidos++;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    fallidos++;
                }

                // Marca el resultado final de ESTE archivo y refresca su fila
                report.setEstado(subidaCorrecta
                        ? ReportFile.EstadoEnvio.EXITO
                        : ReportFile.EstadoEnvio.ERROR);
                final boolean fueExitoso = subidaCorrecta;
                runOnUiThread(() -> adapter.notifyItemChanged(index));
            }

            int totalEnviados = enviados;
            int totalFallidos = fallidos;

            runOnUiThread(() -> {

                btnEnviar.setEnabled(true);

                String resumen = totalEnviados + " reportes enviados";
                if (totalFallidos > 0) {
                    resumen += ", " + totalFallidos + " con error";
                }

                Toast.makeText(
                        ReportesActivity.this,
                        resumen,
                        Toast.LENGTH_LONG
                ).show();

                // No se recarga la lista automáticamente: así el usuario ve
                // el resultado (✓ verde / ✗ rojo) de cada reporte en su fila,
                // en vez de que desaparezcan de golpe los que se movieron o
                // borraron tras un envío exitoso (gestionarArchivoTrasEnvio).
            });

        }).start();
    }
}