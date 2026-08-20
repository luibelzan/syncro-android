package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.parameters.MeterInfo;
import com.celnet.syncro.models.parameters.MeterInfoDisplay;
import com.celnet.syncro.objects.ids.MeterInfoReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.card.MaterialCardView;

public class IdsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ids);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        MaterialCardView layoutResultados = findViewById(R.id.layoutResultados);

        TextView tvSerial = findViewById(R.id.tvSerial);
        ImageView ivFabricanteLogo = findViewById(R.id.ivFabricanteLogo);
        TextView tvFabricante = findViewById(R.id.tvFabricante);
        TextView tvModelo = findViewById(R.id.tvModelo);
        TextView tvFabricadoEn = findViewById(R.id.tvFabricadoEn);
        TextView tvFirmwareDlms = findViewById(R.id.tvFirmwareDlms);
        TextView tvTipoEquipo = findViewById(R.id.tvTipoEquipo);
        TextView tvCompanion = findViewById(R.id.tvCompanion);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);

        // 🔹 Hilo secundario para evitar NetworkOnMainThreadException
        new Thread(() -> {
            DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                    ? new DLMSConnection(config.getBluetoothDeviceName())
                    : new DLMSConnection(config.getIp(), config.getPort());

            try {
                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(IdsActivity.this);

                // Leer Identificadores
                MeterInfo datos = MeterInfoReader.leerIdentificadores(res.reader);

                MeterInfoDisplay display = MeterInfoDisplay.parse(
                        res.serialNumber, datos.equipo, datos.tipo, datos.firmware);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutResultados.setVisibility(View.VISIBLE);

                    tvSerial.setText(display.numeroSerie);
                    tvFabricante.setText(display.fabricanteNombre);
                    tvModelo.setText(display.modelo);
                    tvFabricadoEn.setText(display.fabricadoEn);
                    tvFirmwareDlms.setText(display.firmwareDlms);
                    tvTipoEquipo.setText(display.tipoEquipo);
                    tvCompanion.setText(display.companion);

                    cargarLogoFabricante(ivFabricanteLogo, display.logoResourceName());
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Toda actualización de UI dentro de runOnUiThread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    new androidx.appcompat.app.AlertDialog.Builder(IdsActivity.this)
                            .setTitle("Error de lectura")
                            .setMessage("No se pudieron leer los identificadores del contador.\n\n"
                                    + e.getMessage())
                            .setPositiveButton("Aceptar", null)
                            .setCancelable(true)
                            .show();
                });
            } finally {
                conn.close();
            }
        }).start();
    }

    /**
     * Busca en res/drawable un recurso con el nombre indicado (p.ej. "logo_sagemcom").
     * Si no existe (fabricante sin logo añadido todavía), usa un icono de repuesto.
     */
    private void cargarLogoFabricante(ImageView imageView, String resourceName) {
        int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
        if (resId != 0) {
            imageView.setImageResource(resId);
        } else {
            // ⚠️ TODO: sustituye por un icono de "fabricante genérico" propio si tienes uno
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
}