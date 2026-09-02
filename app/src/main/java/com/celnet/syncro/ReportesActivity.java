package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.ReportesAdapter;
import com.celnet.syncro.models.ReportFile;
import com.celnet.syncro.models.UploadResult;
import com.celnet.syncro.utils.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

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
        ExtendedFloatingActionButton btnEliminar = findViewById(R.id.btnEliminar);

        rv.setLayoutManager(new LinearLayoutManager(this));

        cargarReportes();

        adapter = new ReportesAdapter(lista);
        rv.setAdapter(adapter);

        btnEnviar.setOnClickListener(v -> enviarSeleccionados(btnEnviar));
        btnEliminar.setOnClickListener(v -> confirmarEliminarSeleccionados());
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

    private void confirmarEliminarSeleccionados() {

        int cantidadSeleccionada = 0;
        for (ReportFile report : lista) {
            if (report.isSeleccionado()) {
                cantidadSeleccionada++;
            }
        }

        if (cantidadSeleccionada == 0) {
            Toast.makeText(this, "Selecciona al menos un reporte", Toast.LENGTH_SHORT).show();
            return;
        }

        final int cantidadFinal = cantidadSeleccionada;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar reportes")
                .setMessage("¿Seguro que quieres eliminar " + cantidadFinal
                        + " reporte" + (cantidadFinal == 1 ? "" : "s")
                        + " del dispositivo? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarSeleccionados())
                .setNegativeButton("Cancelar", null)
                .setCancelable(true)
                .show();
    }

    private void eliminarSeleccionados() {

        int eliminados = 0;
        int fallidos = 0;

        Iterator<ReportFile> iterator = lista.iterator();
        while (iterator.hasNext()) {
            ReportFile report = iterator.next();
            if (!report.isSeleccionado()) {
                continue;
            }

            boolean borrado = report.getFile().delete();
            if (borrado) {
                eliminados++;
                iterator.remove();
            } else {
                fallidos++;
                android.util.Log.e("ReportesActivity",
                        "No se pudo eliminar el archivo: " + report.getFile().getName());
            }
        }

        adapter.notifyDataSetChanged();

        String resumen = eliminados + " reporte" + (eliminados == 1 ? "" : "s") + " eliminados";
        if (fallidos > 0) {
            resumen += ", " + fallidos + " con error";
        }
        Toast.makeText(this, resumen, Toast.LENGTH_LONG).show();
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

                    UploadResult resultado;

                    if ("SFTP".equalsIgnoreCase(protocolo)) {
                        resultado = Utils.subirArchivoSFTPConDetalle(ReportesActivity.this, report.getFile());
                    } else if ("FTPS".equalsIgnoreCase(protocolo)) {
                        resultado = Utils.subirArchivoFTPSConDetalle(ReportesActivity.this, report.getFile());
                    } else {
                        resultado = Utils.subirArchivoFTPConDetalle(ReportesActivity.this, report.getFile());
                    }

                    subidaCorrecta = resultado.success;

                    if (subidaCorrecta) {
                        enviados++;
                        Utils.gestionarArchivoTrasEnvio(report.getFile(), afterSend, ReportesActivity.this);
                    } else {
                        fallidos++;
                        report.setMensajeError(resultado.errorMessage);
                        android.util.Log.e("ReportesActivity",
                                "Fallo al subir " + report.getFile().getName() + ": " + resultado.errorMessage);
                    }

                }  catch (Exception e) {
                    com.celnet.syncro.utils.AppLogger.e("ReportesActivity",
                            "Error al subir " + report.getFile().getName() + ": " + e.getMessage());
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