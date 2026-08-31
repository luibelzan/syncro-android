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

import com.celnet.syncro.adapters.EventAdapter;
import com.celnet.syncro.models.events.EventFila;
import com.celnet.syncro.utils.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosEventsActivity extends BaseActivity {

    private String generarEventsXML(ArrayList<EventFila> datos, String cntId, String cncId) {
        StringBuilder sb = new StringBuilder();

        sb.append("<Report IdRpt=\"S09\" IdPet=\"0\" Version=\"3.1.c\">\n");
        sb.append("  <Cnc Id=\"").append(cncId).append("\">\n");
        sb.append("    <Cnt Id=\"").append(cntId).append("\">\n");

        for (EventFila fila : datos) {
            boolean tieneDatosAdicionales = fila.d1 != null || fila.d2 != null;

            // Et = grupo/categoría del evento (fila.cod), C = código del evento
            // dentro de ese grupo (fila.id). Antes estaba invertido.
            sb.append("      <S09 ")
                    .append("Fh=\"").append(Utils.convertirFecha(fila.fh)).append("\" ")
                    .append("Et=\"").append(fila.cod).append("\" ")
                    .append("C=\"").append(fila.id).append("\"");

            if (!tieneDatosAdicionales) {
                sb.append("></S09>\n");
            } else {
                sb.append(">\n");
                if (fila.d1 != null) {
                    sb.append("        <D1>").append(fila.d1).append("</D1>\n");
                }
                if (fila.d2 != null) {
                    sb.append("        <D2>").append(fila.d2).append("</D2>\n");
                }
                sb.append("      </S09>\n");
            }
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
        setContentView(R.layout.activity_resultados_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ExtendedFloatingActionButton btnExport = findViewById(R.id.btnExport);
        String cntId = getIntent().getStringExtra("cntId");
        ArrayList<EventFila> datos = getIntent().getParcelableArrayListExtra("datos_event_tabla");

        if (datos != null) {
            EventAdapter adapter = new EventAdapter(datos);
            rv.setAdapter(adapter);
        }

        btnExport.setOnClickListener(v -> {
            if (datos != null && !datos.isEmpty()) {
                // 🔹 Obtener configuración guardada
                SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
                String cncName = prefs.getString("cncName", "Syncro");

                String xml = generarEventsXML(datos, cntId, cncName);

                // 🔹 Limpiar nombre (opcional pero recomendado)
                cncName = cncName.replaceAll("\\s+", "_");

                // 🔹 Fecha actual
                String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                        .format(new Date());

                // 🔹 Construir nombre del archivo
                String nombreFichero = cncName + "_0_S09_0_" + fechaActual;

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
                    new Thread(() -> {

                        boolean subidaCorrecta = false;

                        try {

                            String protocolo =
                                    prefs.getString("protocolo", "FTP");

                            if ("SFTP".equalsIgnoreCase(protocolo)) {

                                subidaCorrecta =
                                        Utils.subirArchivoSFTP(
                                                ResultadosEventsActivity.this,
                                                file
                                        );

                            } else if ("FTPS".equalsIgnoreCase(protocolo)) {

                                subidaCorrecta =
                                        Utils.subirArchivoFTPS(
                                                ResultadosEventsActivity.this,
                                                file
                                        );

                            } else {

                                subidaCorrecta =
                                        Utils.subirArchivoFTP(
                                                ResultadosEventsActivity.this,
                                                file
                                        );
                            }

                            if (subidaCorrecta) {

                                Utils.gestionarArchivoTrasEnvio(
                                        file,
                                        afterSend,
                                        ResultadosEventsActivity.this
                                );
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        boolean resultadoFinal = subidaCorrecta;

                        runOnUiThread(() -> {

                            if (resultadoFinal) {

                                Toast.makeText(
                                        ResultadosEventsActivity.this,
                                        "Archivo enviado correctamente",
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                Toast.makeText(
                                        ResultadosEventsActivity.this,
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