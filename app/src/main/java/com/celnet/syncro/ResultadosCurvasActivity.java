package com.celnet.syncro;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.CurvaAdapter;
import com.celnet.syncro.adapters.CurvaCorrienteAdapter;
import com.celnet.syncro.adapters.CurvaVoltajeAdapter;
import com.celnet.syncro.models.curvas.CurvaCorrienteFila;
import com.celnet.syncro.models.curvas.CurvaFila;
import com.celnet.syncro.models.curvas.CurvaVoltajeFila;
import com.celnet.syncro.models.curvas.TipoCurva;
import com.celnet.syncro.utils.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosCurvasActivity extends BaseActivity {

    // =========================
    // GENERAR XML (solo S02 por ahora)
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_curvas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));

        LinearLayout headerLayout = findViewById(R.id.headerLayout);
        ExtendedFloatingActionButton btnExport = findViewById(R.id.btnExport);

        String tipoCurvaStr = getIntent().getStringExtra("tipoCurva");
        TipoCurva tipoCurva = tipoCurvaStr != null ? TipoCurva.valueOf(tipoCurvaStr) : TipoCurva.INCREMENTAL_S02;
        String cntId = getIntent().getStringExtra("cntId");

        ArrayList<CurvaFila> datosS02 = null;
        ArrayList<CurvaVoltajeFila> datosS44 = null;
        ArrayList<CurvaCorrienteFila> datosS45 = null;

        switch (tipoCurva) {
            case VOLTAGE_S44:
                datosS44 = getIntent().getParcelableArrayListExtra("datos_curva_voltaje");
                construirCabeceraS44(headerLayout);
                if (datosS44 != null) rv.setAdapter(new CurvaVoltajeAdapter(datosS44));
                break;
            case CURRENT_S45:
                datosS45 = getIntent().getParcelableArrayListExtra("datos_curva_corriente");
                construirCabeceraS45(headerLayout);
                if (datosS45 != null) rv.setAdapter(new CurvaCorrienteAdapter(datosS45));
                break;
            case INCREMENTAL_S02:
            default:
                datosS02 = getIntent().getParcelableArrayListExtra("datos_curva_tabla");
                construirCabeceraS02(headerLayout);
                if (datosS02 != null) rv.setAdapter(new CurvaAdapter(datosS02));
                break;
        }

        TipoCurva tipoFinal = tipoCurva;
        ArrayList<CurvaFila> finalDatosS02 = datosS02;

        btnExport.setOnClickListener(v -> {
            if (tipoFinal != TipoCurva.INCREMENTAL_S02) {
                Toast.makeText(this, "Exportación no disponible todavía para este tipo de curva", Toast.LENGTH_SHORT).show();
                return;
            }

            if (finalDatosS02 == null || finalDatosS02.isEmpty()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
            String cncName = prefs.getString("cncName", "Syncro");

            String xml = generarCurvasXML(finalDatosS02, cntId, cncName);

            cncName = cncName.replaceAll("\\s+", "_");

            String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            String nombreFichero = cncName + "_0_S02_0_" + fechaActual;

            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File syncroFolder = new File(downloadsFolder, "Syncro/Reports");
            if (!syncroFolder.exists()) syncroFolder.mkdirs();

            File file = new File(syncroFolder, nombreFichero);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(xml.getBytes());
                fos.flush();

                String afterGenerate = prefs.getString("afterGenerate", "Guardar e intentar enviar al FTP inmediatamente");
                String afterSend = prefs.getString("afterSend", "Mover a la carpeta de backup del dispositivo");

                if (afterGenerate.equals("Solo guardar para enviar al FTP mas tarde")) {
                    Toast.makeText(this, "Archivo guardado para envío posterior", Toast.LENGTH_LONG).show();
                    return;
                }

                new Thread(() -> {
                    boolean subidaCorrecta = false;
                    try {
                        String protocolo = prefs.getString("protocolo", "FTP");
                        if ("SFTP".equalsIgnoreCase(protocolo)) {
                            subidaCorrecta = Utils.subirArchivoSFTP(ResultadosCurvasActivity.this, file);
                        } else if ("FTPS".equalsIgnoreCase(protocolo)) {
                            subidaCorrecta = Utils.subirArchivoFTPS(ResultadosCurvasActivity.this, file);
                        } else {
                            subidaCorrecta = Utils.subirArchivoFTP(ResultadosCurvasActivity.this, file);
                        }
                        if (subidaCorrecta) {
                            Utils.gestionarArchivoTrasEnvio(file, afterSend, ResultadosCurvasActivity.this);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    boolean resultadoFinal = subidaCorrecta;
                    runOnUiThread(() -> {
                        Toast.makeText(ResultadosCurvasActivity.this,
                                resultadoFinal ? "Archivo enviado correctamente" : "Error al enviar archivo",
                                Toast.LENGTH_LONG).show();
                    });
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al generar el fichero XML", Toast.LENGTH_LONG).show();
            }
        });
    }

    // =========================
    // CABECERAS DINÁMICAS
    // =========================
    private void construirCabeceraS02(LinearLayout header) {
        header.removeAllViews();
        addHeaderCell(header, "Fecha/Hora", 2, false);
        addHeaderCell(header, "Bc", 1, true);
        addHeaderCell(header, "AI", 1, true);
        addHeaderCell(header, "AE", 1, true);
        addHeaderCell(header, "R1", 1, true);
        addHeaderCell(header, "R2", 1, true);
        addHeaderCell(header, "R3", 1, true);
        addHeaderCell(header, "R4", 1, true);
    }

    private void construirCabeceraS44(LinearLayout header) {
        header.removeAllViews();
        addHeaderCell(header, "Fecha/Hora", 2, false);
        addHeaderCell(header, "Max_L1v", 1, true);
        addHeaderCell(header, "Max_L2v", 1, true);
        addHeaderCell(header, "Max_L3v", 1, true);
        addHeaderCell(header, "Av_L1v", 1, true);
        addHeaderCell(header, "Av_L2v", 1, true);
        addHeaderCell(header, "Av_L3v", 1, true);
        addHeaderCell(header, "Min_L1v", 1, true);
        addHeaderCell(header, "Min_L2v", 1, true);
        addHeaderCell(header, "Min_L3v", 1, true);
        addHeaderCell(header, "Status", 1, true);
    }

    private void construirCabeceraS45(LinearLayout header) {
        header.removeAllViews();
        addHeaderCell(header, "Fecha/Hora", 2, false);
        addHeaderCell(header, "Max_L1i", 1, true);
        addHeaderCell(header, "Max_L2i", 1, true);
        addHeaderCell(header, "Max_L3i", 1, true);
        addHeaderCell(header, "Max_Ni", 1, true);
        addHeaderCell(header, "Av_L1i", 1, true);
        addHeaderCell(header, "Av_L2i", 1, true);
        addHeaderCell(header, "Av_L3i", 1, true);
        addHeaderCell(header, "Av_Ni", 1, true);
        addHeaderCell(header, "Min_L1i", 1, true);
        addHeaderCell(header, "Min_L2i", 1, true);
        addHeaderCell(header, "Min_L3i", 1, true);
        addHeaderCell(header, "Min_Ni", 1, true);
        addHeaderCell(header, "Status", 1, true);
    }

    private void addHeaderCell(LinearLayout header, String texto, float peso, boolean alinearFin) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setAllCaps(true);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextSize(12);
        tv.setTextColor(getResources().getColor(R.color.primary_dark, getTheme()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, peso);
        tv.setLayoutParams(lp);
        if (alinearFin) tv.setGravity(Gravity.END);
        header.addView(tv);
    }
}