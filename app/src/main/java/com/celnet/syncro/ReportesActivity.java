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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class ReportesActivity extends BaseActivity {

    // Carpetas de reportes (dentro de Descargas)
    private static final String CARPETA_RAIZ = "Syncro/Reports";
    private static final String CARPETA_DLMS = CARPETA_RAIZ + "/DLMS";
    private static final String CARPETA_IEC = CARPETA_RAIZ + "/IEC";

    private enum Filtro { TODOS, DLMS, IEC }

    /** Todos los reportes encontrados. */
    private final ArrayList<ReportFile> todos = new ArrayList<>();
    /** Los que se ven según el filtro; es la lista que enseña el adaptador y sobre la que actúan Enviar y Eliminar. */
    private ArrayList<ReportFile> lista = new ArrayList<>();
    private final java.util.HashSet<String> vistos = new java.util.HashSet<>();
    private ReportesAdapter adapter;
    private Filtro filtro = Filtro.TODOS;

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
        aplicarFiltro();

        adapter = new ReportesAdapter(lista);
        rv.setAdapter(adapter);

        ChipGroup chips = findViewById(R.id.chipsFiltro);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipDlms) {
                filtro = Filtro.DLMS;
            } else if (id == R.id.chipIec) {
                filtro = Filtro.IEC;
            } else {
                filtro = Filtro.TODOS;
            }
            aplicarFiltro();
            adapter.notifyDataSetChanged();
        });

        btnEnviar.setOnClickListener(v -> enviarSeleccionados(btnEnviar));
        btnEliminar.setOnClickListener(v -> confirmarEliminarSeleccionados());
    }

    private void cargarReportes() {

        File downloads =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);

        todos.clear();
        vistos.clear();

        listarCarpeta(new File(downloads, CARPETA_DLMS), ReportFile.Origen.DLMS, false);
        listarCarpeta(new File(downloads, CARPETA_IEC), ReportFile.Origen.IEC, false);
        // Variantes con mayúsculas por si una versión anterior creó "dlms" / "iec"
        // (el sistema de ficheros distingue mayúsculas); los duplicados se descartan.
        listarCarpeta(new File(downloads, CARPETA_RAIZ + "/dlms"), ReportFile.Origen.DLMS, false);
        listarCarpeta(new File(downloads, CARPETA_RAIZ + "/iec"), ReportFile.Origen.IEC, false);

        // Reportes antiguos, guardados directamente en Syncro/Reports antes de separar por
        // protocolo: los de IEC son siempre .xlsx; el resto (XML) son de DLMS.
        listarCarpeta(new File(downloads, CARPETA_RAIZ), null, true);
    }

    /**
     * Añade a "todos" los ficheros de una carpeta. Con origen == null (carpeta raíz heredada) se
     * deduce por la extensión.
     */
    private void listarCarpeta(File carpeta, ReportFile.Origen origen, boolean heredada) {

        if (!carpeta.exists()) {
            return;
        }

        File[] archivos = carpeta.listFiles();
        if (archivos == null) {
            return;
        }

        for (File file : archivos) {
            if (!file.isFile()) {
                continue;
            }
            if (!vistos.add(file.getAbsolutePath().toLowerCase())) {
                continue;
            }
            ReportFile.Origen o = origen;
            if (heredada) {
                o = file.getName().toLowerCase().endsWith(".xlsx")
                        ? ReportFile.Origen.IEC
                        : ReportFile.Origen.DLMS;
            }
            todos.add(new ReportFile(file, o));
        }
    }

    /** Rellena "lista" con los reportes que cumplen el filtro actual. */
    private void aplicarFiltro() {
        lista.clear();
        for (ReportFile report : todos) {
            if (filtro == Filtro.TODOS
                    || (filtro == Filtro.DLMS && report.getOrigen() == ReportFile.Origen.DLMS)
                    || (filtro == Filtro.IEC && report.getOrigen() == ReportFile.Origen.IEC)) {
                lista.add(report);
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
                todos.remove(report);
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

                    // Cada origen usa su propia configuración: FTP y STG de DLMS o de IEC
                    boolean esIec = report.getOrigen() == ReportFile.Origen.IEC;
                    String prefsName = esIec ? IecFtpActivity.PREFS_NAME : Utils.PREFS_DLMS;
                    SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
                    String protocolo = prefs.getString("protocolo", esIec ? "" : "FTP");
                    String afterSend = prefs.getString("afterSend",
                            "Mover a la carpeta de backup del dispositivo");

                    UploadResult resultado = Utils.subirArchivoSegunProtocolo(
                            ReportesActivity.this, report.getFile(), protocolo, prefsName);

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