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

import com.example.syncro.adapters.CierreMensualAdapter;
import com.example.syncro.models.CierreFila;
import com.example.syncro.models.CierreMensualFila;
import com.example.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosCierresMensualesActivity extends AppCompatActivity {

    private String generarCierresMensualesXML(ArrayList<CierreMensualFila> datos, String cntId, String cncId) {

        StringBuilder sb = new StringBuilder();

        sb.append("<Report IdRpt=\"S04\" IdPet=\"0\" Version=\"3.1.c\">\n");
        sb.append("    <Cnc Id=\"").append(cncId).append("\">\n");
        sb.append("        <Cnt Id=\"").append(cntId).append("\">\n");

        for (CierreMensualFila fila : datos) {

            sb.append("            <S04 ")
                    .append("Fhi=\"").append(Utils.convertirFecha(fila.fechaInicio)).append("\" ")
                    .append("Fhf=\"").append(Utils.convertirFecha(fila.fechaFin)).append("\" ")
                    .append("Ctr=\"").append(fila.contrato).append("\" ")
                    .append("Pt=\"").append(fila.periodo).append("\" ")
                    .append("Mx=\"").append(fila.maxAIi).append("\" ")
                    .append("Fx=\"").append(Utils.convertirFecha(fila.fechaMaxAIi)).append("\">\n");

            // VALUE ACTIVA
            sb.append("                <Value ")
                    .append("AIa=\"").append(fila.activeImportAbs).append("\" ")
                    .append("AEa=\"").append(fila.activeExportAbs).append("\" ")
                    .append("R1a=\"").append(fila.r1).append("\" ")
                    .append("R2a=\"").append(fila.r2).append("\" ")
                    .append("R3a=\"").append(fila.r3).append("\" ")
                    .append("R4a=\"").append(fila.r4).append("\"/>\n");

            // VALUE INDUCTIVA
            sb.append("                <Value ")
                    .append("AIi=\"").append(fila.activeImportInc).append("\" ")
                    .append("AEi=\"").append(fila.activeExportInc).append("\" ")
                    .append("R1i=\"").append(fila.r1Inc).append("\" ")
                    .append("R2i=\"").append(fila.r2Inc).append("\" ")
                    .append("R3i=\"").append(fila.r3Inc).append("\" ")
                    .append("R4i=\"").append(fila.r4Inc).append("\"/>\n");

            sb.append("            </S04>\n");
        }

        sb.append("        </Cnt>\n");
        sb.append("    </Cnc>\n");
        sb.append("</Report>");

        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_cierres_mensuales);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultadosCierresMensuales);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<CierreMensualFila> datos = getIntent().getParcelableArrayListExtra("datos_cierres_tabla");
        String cntId = getIntent().getStringExtra("cntId");
        LinearLayout btnExport = findViewById(R.id.btnExport);

        if(datos != null) {
            CierreMensualAdapter adapter = new CierreMensualAdapter(datos);
            rv.setAdapter(adapter);
        }

        btnExport.setOnClickListener(v -> {
            if(datos != null && !datos.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
                String cncName = prefs.getString("cncName", "Syncro");

                String xml = generarCierresMensualesXML(datos, cntId, cncName);

                // 🔹 Limpiar nombre (opcional pero recomendado)
                cncName = cncName.replaceAll("\\s+", "_");

                // 🔹 Fecha actual
                String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                        .format(new Date());

                // 🔹 Construir nombre del archivo
                String nombreFichero = cncName + "_0_S04_0_" + fechaActual + ".xml";

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