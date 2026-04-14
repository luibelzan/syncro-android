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
import java.util.ArrayList;

public class ResultadosCurvasActivity extends BaseActivity {

    // =========================
    // GENERAR XML
    // =========================
    private String generarCurvasXML(ArrayList<CurvaFila> datos, String cntId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Report IdRpt=\"S02\" IdPet=\"0\" Version=\"3.1.c\">\n");
        sb.append("<Cnc Id=\"Syncro\">\n");
        sb.append(" <Cnt Id=\"").append(cntId).append("\" Magn=\"1\">\n");

        for (CurvaFila fila : datos) {
            sb.append("      <S02 ")
                    .append("Fh=\"").append(fila.fechaHora).append("\" ")
                    .append("Bc=\"").append(fila.bc).append("\" ")
                    .append("AI=\"").append(fila.ai).append("\" ")
                    .append("AE=\"").append(fila.ae).append("\" ")
                    .append("R1=\"").append(fila.r1).append("\" ")
                    .append("R2=\"").append(fila.r2).append("\" ")
                    .append("R3=\"").append(fila.r3).append("\" ")
                    .append("R4=\"").append(fila.r3).append("\"/>\n");
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

                String xml = generarCurvasXML(datos, cntId);

                File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "curvas.xml"
                );

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(xml.getBytes());
                    fos.flush();

                    Log.d("FILE_PATH", file.getAbsolutePath());

                    SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
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