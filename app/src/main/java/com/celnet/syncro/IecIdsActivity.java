package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.params.Iec870DeviceInfo;
import com.celnet.syncro.objects.iec870.params.Iec870InfoReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Locale;

/**
 * Información del equipo por IEC-870 (solo lectura): nº de serie, fabricante, modelo, firmware,
 * fechas, batería, relaciones de transformación, periodos de integración y contratos activos.
 * Antes se mostraba en IecInfoActivity (configuración de puertos).
 */
public class IecIdsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_ids);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ExtendedFloatingActionButton btnRead = findViewById(R.id.btnRead);
        LinearLayout progressBar = findViewById(R.id.progressContainer);
        MaterialCardView cardInfo = findViewById(R.id.cardInfo);
        TextView tvInfo = findViewById(R.id.tvInfoEquipo);
        TextView tvAvisoLectura = findViewById(R.id.tvAvisoLectura);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        btnRead.setOnClickListener(v -> {
            bloquear(true, btnRead, progressBar);

            new Thread(() -> {
                try {
                    Iec870DeviceInfo info = Iec870InfoReader.leer(IecIdsActivity.this, config);

                    runOnUiThread(() -> {
                        bloquear(false, btnRead, progressBar);
                        tvInfo.setText(textoInfo(info));
                        cardInfo.setVisibility(View.VISIBLE);
                        tvAvisoLectura.setVisibility(View.GONE);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnRead, progressBar);
                        mostrarError("No se pudo leer la información del equipo.", e);
                    });
                }
            }).start();
        });
    }

    private String textoInfo(Iec870DeviceInfo i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nº de serie: ").append(i.serie).append('\n');
        sb.append("Código de fabricante: ").append(i.codigoFabricante).append('\n');
        sb.append("Modelo de registrador: ").append(i.modelo).append('\n');
        sb.append("Versión de firmware: ").append(i.firmware).append('\n');
        sb.append("Fecha de la versión del protocolo: ").append(i.fechaProtocolo).append('\n');
        sb.append("Fecha de versión del equipo: ").append(i.fechaVersion).append('\n');
        sb.append("Estado de carga de la batería: ").append(i.bateria).append('\n');
        if (!i.puerto1.cadena.isEmpty()) {
            sb.append("Cadena de inicialización del módem: ").append(i.puerto1.cadena).append('\n');
        }
        sb.append(String.format(Locale.getDefault(), "Relación de tensión: %d / %d dV\n",
                i.trafo.tensionPrimariaDv, i.trafo.tensionSecundariaDv));
        sb.append(String.format(Locale.getDefault(), "Relación de corriente: %d / %d dA\n",
                i.trafo.corrientePrimariaDa, i.trafo.corrienteSecundariaDa));
        for (int k = 0; k < 3; k++) {
            sb.append("Periodo de integración curva ").append(k + 1).append(": ")
                    .append(i.periodoIntegracion[k]).append(" min\n");
        }
        for (int k = 0; k < 3; k++) {
            sb.append("Contrato ").append(k + 1).append(": ")
                    .append(i.contratoActivo[k] ? "activo" : "inactivo");
            if (k < 2) sb.append('\n');
        }
        return sb.toString();
    }

    private void bloquear(boolean leyendo, ExtendedFloatingActionButton btnRead,
                          LinearLayout progress) {
        btnRead.setEnabled(!leyendo);
        progress.setVisibility(leyendo ? View.VISIBLE : View.GONE);
    }

    private void mostrarError(String mensaje, Exception e) {
        new AlertDialog.Builder(this)
                .setTitle("Error (IEC-870)")
                .setMessage(mensaje + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .show();
    }
}