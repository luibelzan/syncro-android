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

import com.example.syncro.adapters.CierreAdapter;
import com.example.syncro.models.CierreFila;
import com.example.syncro.models.CurvaFila;
import com.example.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ResultadosCierresActivity extends BaseActivity {

    private String generarCierresXML(ArrayList<CierreFila> datos, String cntId) {
        StringBuilder sb = new StringBuilder();

        sb.append("<Report IdRpt=\"S05\" IdPet=\"0\" Version=\"4.0\">\n");
        sb.append("  <Cnc Id=\"Syncro\">\n");
        sb.append("    <Cnt Id=\"").append(cntId).append("\">\n");

        for (CierreFila fila : datos) {
            sb.append("      <S05 ")
                    .append("Fh=\"").append(fila.fecha).append("\" ")
                    .append("Ctr=\"").append(fila.contrato).append("\" ")
                    .append("Pt=\"").append(fila.periodo).append("\">\n");

            sb.append("        <Value ")
                    .append("AIa=\"").append(fila.activeImport).append("\" ")
                    .append("AEa=\"").append(fila.activeExport).append("\" ")
                    .append("R1a=\"").append(fila.r1).append("\" ")
                    .append("R2a=\"").append(fila.r2).append("\" ")
                    .append("R3a=\"").append(fila.r3).append("\" ")
                    .append("R4a=\"").append(fila.r4).append("\"")
                    .append("></Value>\n");

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
        LinearLayout btnExport = findViewById(R.id.btnExport);

        if(datos != null) {
            CierreAdapter adapter = new CierreAdapter(datos);
            rv.setAdapter(adapter);
        }

        btnExport.setOnClickListener(v -> {
            if (datos != null && !datos.isEmpty()) {
                String xml = generarCierresXML(datos, cntId);

                File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "cierres.xml"
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