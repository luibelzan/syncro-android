package com.example.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.CurvaAdapter;
import com.example.syncro.models.CurvaFila;
import com.example.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosCurvasActivity extends BaseActivity {

    // =========================
    // GENERAR XML
    // =========================
    private String generarCurvasXML(ArrayList<CurvaFila> datos, String cntId, String cncId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Report IdRpt=\"S02\" IdPet=\"0\" Version=\"3.1.c\">\n");
        sb.append("  <Cnc Id=\"").append(cncId).append("\">\n");
        sb.append(" <Cnt Id=\"").append(cntId).append("\" Magn=\"1\">\n");

        for (CurvaFila fila : datos) {
            sb.append("      <S02 ")
                    .append("Fh=\"").append(Utils.convertirFecha(fila.fechaHora)).append("\" ")
                    .append("Bc=\"").append(fila.bc).append("\" ")
                    .append("AI=\"").append(fila.ai).append("\" ")
                    .append("AE=\"").append(fila.ae).append("\" ")
                    .append("R1=\"").append(fila.r1).append("\" ")
                    .append("R2=\"").append(fila.r2).append("\" ")
                    .append("R3=\"").append(fila.r3).append("\" ")
                    .append("R4=\"").append(fila.r4).append("\"/>\n");
        }

        sb.append("    </Cnt>\n");
        sb.append("  </Cnc>\n");
        sb.append("</Report>");

        return sb.toString();
    }


    // =========================
    // ONCREATE
    // =========================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados_curvas);

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<CurvaFila> datos = getIntent().getParcelableArrayListExtra("datos_curva_tabla");
        String cntId = getIntent().getStringExtra("cntId");

        LinearLayout btnExport = findViewById(R.id.btnExport);

        if (datos != null) {
            rv.setAdapter(new CurvaAdapter(datos));
        }

        btnExport.setOnClickListener(v -> {
            if (datos != null && !datos.isEmpty()) {
                // 🔹 Obtener configuración guardada
                SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
                String cncName = prefs.getString("cncName", "Syncro");

                String xml = generarCurvasXML(datos, cntId, cncName);

                // 🔹 Limpiar nombre (opcional pero recomendado)
                cncName = cncName.replaceAll("\\s+", "_");

                // 🔹 Fecha actual
                String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                        .format(new Date());

                // 🔹 Construir nombre del archivo
                String nombreFichero = cncName + "_0_S02_0_" + fechaActual + ".xml";

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