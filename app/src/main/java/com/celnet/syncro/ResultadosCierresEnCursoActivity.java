package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.CierreEnCursoAdapter;
//import com.celnet.syncro.audit.AuditLog;
import com.celnet.syncro.models.cierres.CierreEnCursoFila;
import com.celnet.syncro.export.S27ReportXml;
import com.celnet.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosCierresEnCursoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_cierres_encurso);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        ArrayList<CierreEnCursoFila> datos =
                getIntent().getParcelableArrayListExtra("datos_cierres_tabla");
        String cntId = getIntent().getStringExtra("cntId");

        RecyclerView rv = findViewById(R.id.rvResultadosCierres);
        rv.setLayoutManager(new LinearLayoutManager(this));

        if (datos != null) {
            rv.setAdapter(new CierreEnCursoAdapter(datos));
        }

        LinearLayout btnExport = findViewById(R.id.btnExport);

        btnExport.setOnClickListener(v -> {
            if (datos == null || datos.isEmpty()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Obtener configuración guardada
            SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
            String cncName = prefs.getString("cncName", "Syncro");

            String xml = S27ReportXml.generar(datos, cntId, cncName, ZoneId.systemDefault());

            // 🔹 Limpiar nombre
            cncName = cncName.replaceAll("\\s+", "_");

            // 🔹 Fecha actual
            String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    .format(new Date());

            // 🔹 Nombre del archivo (mismo criterio que S05/S04)
            String nombreFichero = cncName + "_0_S27_0_" + fechaActual;

            // 🔹 Carpeta Downloads/Syncro/Reports/DLMS
            File downloadsFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File syncroFolder = new File(downloadsFolder, "Syncro/Reports/DLMS");
            if (!syncroFolder.exists()) {
                syncroFolder.mkdirs();
            }

            File file = new File(syncroFolder, nombreFichero);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(xml.getBytes(StandardCharsets.UTF_8));
                fos.flush();
                //AuditLog.log(ResultadosCierresEnCursoActivity.this,
                //        "GENERACION DE REPORTE DE CIERRES EN CURSO " + file.getName(), cntId);

                String afterGenerate = prefs.getString(
                        "afterGenerate",
                        "Guardar e intentar enviar al FTP inmediatamente");
                String afterSend = prefs.getString(
                        "afterSend",
                        "Mover a la carpeta de backup del dispositivo");

                // SOLO GUARDAR
                if (afterGenerate.equals("Solo guardar para enviar al FTP mas tarde")) {
                    Toast.makeText(this, "Archivo guardado para envío posterior",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // SUBIR EN SEGUNDO PLANO
                new Thread(() -> {
                    boolean subidaCorrecta = false;

                    try {
                        String protocolo = prefs.getString("protocolo", "FTP");

                        if ("SFTP".equalsIgnoreCase(protocolo)) {
                            subidaCorrecta = Utils.subirArchivoSFTP(
                                    ResultadosCierresEnCursoActivity.this, file);
                        } else if ("FTPS".equalsIgnoreCase(protocolo)) {
                            subidaCorrecta = Utils.subirArchivoFTPS(
                                    ResultadosCierresEnCursoActivity.this, file);
                        } else {
                            subidaCorrecta = Utils.subirArchivoFTP(
                                    ResultadosCierresEnCursoActivity.this, file);
                        }

                        //AuditLog.log(ResultadosCierresEnCursoActivity.this,
                        //        (subidaCorrecta ? "ENVIO DE REPORTE " : "ERROR AL ENVIAR REPORTE ")
                        //                + file.getName() + " (" + protocolo + ")", cntId);

                        if (subidaCorrecta) {
                            Utils.gestionarArchivoTrasEnvio(
                                    file, afterSend, ResultadosCierresEnCursoActivity.this);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        //AuditLog.log(ResultadosCierresEnCursoActivity.this,
                        //        "ERROR AL ENVIAR REPORTE " + file.getName(), cntId);
                    }

                    boolean resultadoFinal = subidaCorrecta;

                    runOnUiThread(() -> Toast.makeText(
                            ResultadosCierresEnCursoActivity.this,
                            resultadoFinal ? "Archivo enviado correctamente"
                                    : "Error al enviar archivo",
                            Toast.LENGTH_LONG).show());
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
                //AuditLog.log(ResultadosCierresEnCursoActivity.this,
                //        "ERROR AL GENERAR REPORTE DE CIERRES EN CURSO " + nombreFichero, cntId);
                Toast.makeText(this, "Error al generar el archivo", Toast.LENGTH_LONG).show();
            }
        });
    }
}