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

import com.celnet.syncro.adapters.CierreAdapter;
import com.celnet.syncro.models.cierres.CierreFila;
import com.celnet.syncro.utils.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosCierresActivity extends BaseActivity {

    private String generarCierresXML(ArrayList<CierreFila> datos, String cntId, String cncId) {
        StringBuilder sb = new StringBuilder();

        sb.append("<Report IdRpt=\"S05\" IdPet=\"0\" Version=\"4.0\">\n");
        sb.append("  <Cnc Id=\"").append(cncId).append("\">\n");
        sb.append("    <Cnt Id=\"").append(cntId).append("\">\n");

        for (CierreFila fila : datos) {
            sb.append("      <S05 ")
                    .append("Fh=\"").append(Utils.convertirFecha(fila.fecha)).append("\" ")
                    .append("Ctr=\"").append(fila.contrato).append("\" ")
                    .append("Pt=\"").append(fila.periodo).append("\">\n");

            sb.append("        <Value ")
                    .append("AIa=\"").append(fila.activeImport).append("\" ")
                    .append("AEa=\"").append(fila.activeExport).append("\" ")
                    .append("R1a=\"").append(fila.r1).append("\" ")
                    .append("R2a=\"").append(fila.r2).append("\" ")
                    .append("R3a=\"").append(fila.r3).append("\" ")
                    .append("R4a=\"").append(fila.r4).append("\"")
                    .append("/>\n");

            sb.append("      </S05>\n");
        }

        sb.append("    </Cnt>\n");
        sb.append("  </Cnc>\n");
        sb.append("</Report>");

        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_cierres);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultadosCierres);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<CierreFila> datos = getIntent().getParcelableArrayListExtra("datos_cierres_tabla");
        String cntId = getIntent().getStringExtra("cntId");
        ExtendedFloatingActionButton btnExport = findViewById(R.id.btnExport);

        if (datos != null) {
            CierreAdapter adapter = new CierreAdapter(datos);
            rv.setAdapter(adapter);
        }

        btnExport.setOnClickListener(v -> {
            if (datos != null && !datos.isEmpty()) {
                // 🔹 Obtener configuración guardada
                SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
                String cncName = prefs.getString("cncName", "Syncro");

                String xml = generarCierresXML(datos, cntId, cncName);

                // 🔹 Limpiar nombre (opcional pero recomendado)
                cncName = cncName.replaceAll("\\s+", "_");

                // 🔹 Fecha actual
                String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                        .format(new Date());

                // 🔹 Construir nombre del archivo
                String nombreFichero = cncName + "_0_S05_0_" + fechaActual;

                // 🔹 Carpeta Downloads
                File downloadsFolder =
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);

                // 🔹 Carpeta específica app
                File syncroFolder = new File(downloadsFolder, "Syncro/Reports");

                // 🔹 Crear carpetas si no existen
                if (!syncroFolder.exists()) {
                    syncroFolder.mkdirs();
                }

                // 🔹 Archivo final
                File file = new File(syncroFolder, nombreFichero);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(xml.getBytes());
                    fos.flush();

                    String afterGenerate = prefs.getString(
                            "afterGenerate",
                            "Guardar e intentar enviar al FTP inmediatamente"
                    );

                    String afterSend = prefs.getString(
                            "afterSend",
                            "Mover a la carpeta de backup del dispositivo"
                    );

                    // ==========================
                    // SOLO GUARDAR
                    // ==========================
                    if (afterGenerate.equals(
                            "Solo guardar para enviar al FTP mas tarde")) {

                        Toast.makeText(
                                this,
                                "Archivo guardado para envío posterior",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // ==========================
                    // SUBIR EN SEGUNDO PLANO
                    // ==========================
                    new Thread(() -> {

                        boolean subidaCorrecta = false;

                        try {

                            String protocolo =
                                    prefs.getString("protocolo", "FTP");

                            if ("SFTP".equalsIgnoreCase(protocolo)) {

                                subidaCorrecta =
                                        Utils.subirArchivoSFTP(
                                                ResultadosCierresActivity.this,
                                                file
                                        );

                            } else if ("FTPS".equalsIgnoreCase(protocolo)) {

                                subidaCorrecta =
                                        Utils.subirArchivoFTPS(
                                                ResultadosCierresActivity.this,
                                                file
                                        );

                            } else {

                                subidaCorrecta =
                                        Utils.subirArchivoFTP(
                                                ResultadosCierresActivity.this,
                                                file
                                        );
                            }

                            if (subidaCorrecta) {

                                Utils.gestionarArchivoTrasEnvio(
                                        file,
                                        afterSend,
                                        ResultadosCierresActivity.this
                                );
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        boolean resultadoFinal = subidaCorrecta;

                        runOnUiThread(() -> {

                            if (resultadoFinal) {

                                Toast.makeText(
                                        ResultadosCierresActivity.this,
                                        "Archivo enviado correctamente",
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                Toast.makeText(
                                        ResultadosCierresActivity.this,
                                        "Error al enviar archivo",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });

                    }).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}