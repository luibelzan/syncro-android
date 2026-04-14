package com.example.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.EventAdapter;
import com.example.syncro.models.CurvaFila;
import com.example.syncro.models.EventFila;
import com.example.syncro.utils.Utils;

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
            sb.append("      <S09 ")
                    .append("Fh=\"").append(fila.fh).append("\" ")
                    .append("Et=\"").append(fila.id).append("\" ")
                    .append("C=\"").append(fila.cod).append("\"/>\n");
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

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));

        LinearLayout btnExport = findViewById(R.id.btnExport);
        String cntId = getIntent().getStringExtra("cntId");
        ArrayList<EventFila> datos = getIntent().getParcelableArrayListExtra("datos_event_tabla");

        if(datos != null) {
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
                String nombreFichero = cncName + "_0_S09_" + fechaActual + ".xml";

                // 🔹 Crear archivo con ese nombre
                File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        nombreFichero);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(xml.getBytes());
                    fos.flush();
                    Log.d("FILE_PATH", file.getAbsolutePath());

                    String protocolo = prefs.getString("protocolo", "FTP");

                    // 🔥 SELECCIÓN AUTOMÁTICA
                    if (protocolo.equalsIgnoreCase("SFTP")) {
                        Utils.subirArchivoSFTP(this, file);
                    } else if (protocolo.equalsIgnoreCase("FTPS")) {
                        Utils.subirArchivoFTPS(this, file);
                    } else {
                        Utils.subirArchivoFTP(this, file);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}