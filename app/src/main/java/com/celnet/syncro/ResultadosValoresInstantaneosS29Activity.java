package com.celnet.syncro;

import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.ResultadosS29Adapter;
import com.celnet.syncro.models.instantvalues.RegistroS29;
import com.celnet.syncro.utils.Utils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultadosValoresInstantaneosS29Activity extends BaseActivity {

    public static final String EXTRA_REGISTROS = "extra_registros_s29";

    private static final int ANCHO_MIN_DP = 90;
    private static final int PADDING_CELDA_DP = 10;
    private static final float TEXT_SIZE_HEADER_SP = 12f;
    private static final float TEXT_SIZE_DATA_SP = 13f;
    // Margen de seguridad extra para evitar cualquier recorte por
    // redondeos de medición entre distintas densidades de pantalla.
    private static final int MARGEN_SEGURIDAD_DP = 8;

    // =========================
    // GENERAR XML (S29)
    // =========================
    private String generarS29XML(List<RegistroS29> datos, String cntId, String cncId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Report IdRpt=\"S29\" IdPet=\"0\" Version=\"3.1.c\">\n");
        sb.append("  <Cnc Id=\"").append(cncId).append("\">\n");
        sb.append(" <Cnt Id=\"").append(cntId).append("\">\n");

        for (RegistroS29 fila : datos) {
            sb.append("      <S29 ")
                    .append("Fh=\"").append(Utils.convertirFecha(fila.fechaHora)).append("\" ")
                    .append("L1v=\"").append(fila.voltageL1).append("\" ")
                    .append("L1i=\"").append(fila.currentL1).append("\" ")
                    .append("L2v=\"").append(fila.voltageL2).append("\" ")
                    .append("L2i=\"").append(fila.currentL2).append("\" ")
                    .append("L3v=\"").append(fila.voltageL3).append("\" ")
                    .append("L3i=\"").append(fila.currentL3).append("\" ")
                    .append("Isum=\"").append(fila.currentSum).append("\" ")
                    .append("In=\"").append(fila.neutralCurrent).append("\" ")
                    .append("Id=\"").append(fila.differentialCurrent).append("\" ")
                    .append("Psigned=\"").append(fila.activePowerTotal).append("\" ")
                    .append("L1P=\"").append(fila.activePowerL1).append("\" ")
                    .append("L2P=\"").append(fila.activePowerL2).append("\" ")
                    .append("L3P=\"").append(fila.activePowerL3).append("\" ")
                    .append("Qsigned=\"").append(fila.reactivePowerTotal).append("\" ")
                    .append("L1Q=\"").append(fila.reactivePowerL1).append("\" ")
                    .append("L2Q=\"").append(fila.reactivePowerL2).append("\" ")
                    .append("L3Q=\"").append(fila.reactivePowerL3).append("\" ")
                    .append("PF=\"").append(fila.powerFactorTotal).append("\" ")
                    .append("L1PF=\"").append(fila.powerFactorL1).append("\" ")
                    .append("L2PF=\"").append(fila.powerFactorL2).append("\" ")
                    .append("L3PF=\"").append(fila.powerFactorL3).append("\" ")
                    .append("PhaseSeq=\"").append(fila.phaseSequence).append("\" ")
                    .append("L1vAng=\"").append(fila.angleU1).append("\" ")
                    .append("L2vAng=\"").append(fila.angleU2).append("\" ")
                    .append("L3vAng=\"").append(fila.angleU3).append("\" ")
                    .append("L1iAng=\"").append(fila.angleI1).append("\" ")
                    .append("L2iAng=\"").append(fila.angleI2).append("\" ")
                    .append("L3iAng=\"").append(fila.angleI3).append("\" ")
                    .append("InAng=\"").append(fila.angleIN).append("\" ")
                    .append("IdAng=\"").append(fila.angleIdif).append("\"/>\n");
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
        setContentView(R.layout.activity_resultados_valores_instantaneos_s29);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout rowHeader = findViewById(R.id.rowHeader);
        RecyclerView rvResultados = findViewById(R.id.rvResultados);
        TextView tvSinDatos = findViewById(R.id.tvSinDatos);
        ExtendedFloatingActionButton btnExport = findViewById(R.id.btnExport);

        List<RegistroS29> registros = getIntent().getParcelableArrayListExtra(EXTRA_REGISTROS);
        String cntId = getIntent().getStringExtra("cntId");

        if (registros == null || registros.isEmpty()) {
            tvSinDatos.setVisibility(View.VISIBLE);
            rvResultados.setVisibility(View.GONE);
            btnExport.setVisibility(View.GONE);
            return;
        }

        int[] anchosColumnas = calcularAnchosColumnas(registros);

        pintarFilaCabecera(rowHeader, RegistroS29.CABECERAS, anchosColumnas);

        rvResultados.setLayoutManager(new LinearLayoutManager(this));
        rvResultados.setHasFixedSize(false);
        rvResultados.setAdapter(new ResultadosS29Adapter(registros, anchosColumnas));

        btnExport.setOnClickListener(v -> {

            SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
            String cncName = prefs.getString("cncName", "Syncro");

            String xml = generarS29XML(registros, cntId, cncName);

            cncName = cncName.replaceAll("\\s+", "_");

            String fechaActual = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            String nombreFichero = cncName + "_0_S29_0_" + fechaActual;

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
                            subidaCorrecta = Utils.subirArchivoSFTP(ResultadosValoresInstantaneosS29Activity.this, file);
                        } else if ("FTPS".equalsIgnoreCase(protocolo)) {
                            subidaCorrecta = Utils.subirArchivoFTPS(ResultadosValoresInstantaneosS29Activity.this, file);
                        } else {
                            subidaCorrecta = Utils.subirArchivoFTP(ResultadosValoresInstantaneosS29Activity.this, file);
                        }
                        if (subidaCorrecta) {
                            Utils.gestionarArchivoTrasEnvio(file, afterSend, ResultadosValoresInstantaneosS29Activity.this);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    boolean resultadoFinal = subidaCorrecta;
                    runOnUiThread(() -> {
                        Toast.makeText(ResultadosValoresInstantaneosS29Activity.this,
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

    /**
     * Ancho de cada columna en px, midiendo el ancho REAL del texto más
     * largo (cabecera o cualquier valor de la columna) con la tipografía
     * y tamaño que realmente se van a usar al pintar, más padding y un
     * margen de seguridad. Estimar por "caracteres x dp fijo" no sirve:
     * el monospace real es más ancho que esa aproximación y el texto,
     * al ir alineado a la derecha (Gravity.END), se recorta por la
     * izquierda cuando la columna queda un poco corta.
     */
    private int[] calcularAnchosColumnas(List<RegistroS29> registros) {
        String[] cabeceras = RegistroS29.CABECERAS;
        int columnas = cabeceras.length;

        Paint paintHeader = crearPaintMedicion(TEXT_SIZE_HEADER_SP, Typeface.BOLD);
        Paint paintData = crearPaintMedicion(TEXT_SIZE_DATA_SP, Typeface.NORMAL);

        float[] maxAnchoTextoPx = new float[columnas];
        for (int i = 0; i < columnas; i++) {
            maxAnchoTextoPx[i] = paintHeader.measureText(cabeceras[i]);
        }

        for (RegistroS29 registro : registros) {
            String[] valores = registro.toRowValues();
            for (int i = 0; i < valores.length && i < columnas; i++) {
                String valor = (valores[i] != null) ? valores[i] : "-";
                float ancho = paintData.measureText(valor);
                if (ancho > maxAnchoTextoPx[i]) {
                    maxAnchoTextoPx[i] = ancho;
                }
            }
        }

        int paddingPx = dpToPx(PADDING_CELDA_DP) * 2; // padding izquierdo + derecho de la celda
        int margenSeguridadPx = dpToPx(MARGEN_SEGURIDAD_DP);
        int anchoMinPx = dpToPx(ANCHO_MIN_DP);

        int[] anchos = new int[columnas];
        for (int i = 0; i < columnas; i++) {
            int anchoContenido = Math.round(maxAnchoTextoPx[i]) + paddingPx + margenSeguridadPx;
            anchos[i] = Math.max(anchoMinPx, anchoContenido);
        }
        return anchos;
    }

    private Paint crearPaintMedicion(float textSizeSp, int estiloTipografia) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, estiloTipografia));
        paint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSizeSp, getResources().getDisplayMetrics()));
        return paint;
    }

    private void pintarFilaCabecera(LinearLayout contenedor, String[] valores, int[] anchosColumnas) {
        contenedor.removeAllViews();
        for (int i = 0; i < valores.length; i++) {
            contenedor.addView(crearCeldaCabecera(valores[i], anchosColumnas[i]));
        }
    }

    private TextView crearCeldaCabecera(String texto, int anchoPx) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(anchoPx, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setText(texto == null ? "-" : texto);
        int padding = dpToPx(PADDING_CELDA_DP);
        tv.setPadding(padding, padding, padding, padding);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_HEADER_SP);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setSingleLine(true);
        tv.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        tv.setBackgroundResource(R.drawable.bg_celda_cabecera);
        return tv;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}