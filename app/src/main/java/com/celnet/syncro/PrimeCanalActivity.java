package com.celnet.syncro;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.objects.prime.PrimeChannelReader;
import com.celnet.syncro.objects.prime.PrimeChannelWriter;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class PrimeCanalActivity extends BaseActivity {

    private MaterialCheckBox cbAuto;
    private List<MaterialCheckBox> canales;

    // Evita bucles infinitos entre el listener de AUTO y el de los canales
    private boolean actualizandoDesdeAuto = false;
    private boolean actualizandoDesdeCanal = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prime_canal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ===== Selección de canal: AUTO y Canal 1-8 son excluyentes =====
        MaterialCheckBox cbChannelSelection = findViewById(R.id.cbChannelSelection);
        cbAuto = findViewById(R.id.cbAuto);
        canales = Arrays.asList(
                (MaterialCheckBox) findViewById(R.id.cbChannel1),
                (MaterialCheckBox) findViewById(R.id.cbChannel2),
                (MaterialCheckBox) findViewById(R.id.cbChannel3),
                (MaterialCheckBox) findViewById(R.id.cbChannel4),
                (MaterialCheckBox) findViewById(R.id.cbChannel5),
                (MaterialCheckBox) findViewById(R.id.cbChannel6),
                (MaterialCheckBox) findViewById(R.id.cbChannel7),
                (MaterialCheckBox) findViewById(R.id.cbChannel8)
        );

        cbAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actualizandoDesdeCanal) return;
            if (isChecked) {
                actualizandoDesdeAuto = true;
                for (MaterialCheckBox canal : canales) {
                    canal.setChecked(false);
                }
                actualizandoDesdeAuto = false;
            }
        });

        CompoundButton.OnCheckedChangeListener listenerCanal = (buttonView, isChecked) -> {
            if (actualizandoDesdeAuto) return;
            if (isChecked) {
                actualizandoDesdeCanal = true;
                cbAuto.setChecked(false);
                actualizandoDesdeCanal = false;
            }
        };
        for (MaterialCheckBox canal : canales) {
            canal.setOnCheckedChangeListener(listenerCanal);
        }

        // ===== macMaxBandSearchTime / macMinBandSearchTime =====
        MaterialCheckBox checkBoxMacMax = findViewById(R.id.checkBoxMacMax);
        TextInputEditText editMacMaxBandSearchTime = findViewById(R.id.editMacMaxBandSearchTime);
        checkBoxMacMax.setOnCheckedChangeListener((buttonView, isChecked) ->
                editMacMaxBandSearchTime.setEnabled(isChecked));

        MaterialCheckBox checkBoxMacMin = findViewById(R.id.checkBoxMacMin);
        TextInputEditText editMacMinBandSearchTime = findViewById(R.id.editMacMinBandSearchTime);
        checkBoxMacMin.setOnCheckedChangeListener((buttonView, isChecked) ->
                editMacMinBandSearchTime.setEnabled(isChecked));

        // ===== Vistas de resultado / progreso =====
        MaterialCardView cardResultado = findViewById(R.id.cardResultado);
        TextView tvResultado = findViewById(R.id.tvResultado);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        // ⚠️ El botón de leer se llama btnLeerActual en tu layout, no btnLeer
        ExtendedFloatingActionButton btnLeerActual = findViewById(R.id.btnLeerActual);
        ExtendedFloatingActionButton btnProgramar = findViewById(R.id.btnProgramar);

        // ===== Leer =====
        btnLeerActual.setOnClickListener(v -> {
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            btnLeerActual.setEnabled(false);
            btnProgramar.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(PrimeCanalActivity.this);

                    PrimeChannelReader.Resultado resultado = PrimeChannelReader.leerCanalPrime(res.reader);
                    SpannableStringBuilder textoResultado = formatearResultadoCanalPrime(resultado);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnLeerActual.setEnabled(true);
                        btnProgramar.setEnabled(true);
                        cardResultado.setVisibility(View.VISIBLE);
                        tvResultado.setText(textoResultado);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnLeerActual.setEnabled(true);
                        btnProgramar.setEnabled(true);
                        new androidx.appcompat.app.AlertDialog.Builder(PrimeCanalActivity.this)
                                .setTitle("Error de lectura")
                                .setMessage("No se pudo leer el canal PRIME.\n\n" + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .setCancelable(true)
                                .show();
                    });
                } finally {
                    conn.close();
                }
            }).start();
        });

        // ===== Programar =====
        btnProgramar.setOnClickListener(v -> {

            boolean escribirCanal = cbChannelSelection.isChecked();
            boolean[] canalMascara = new boolean[8];
            for (int i = 0; i < canales.size(); i++) {
                canalMascara[i] = canales.get(i).isChecked();
            }
            // AUTO se programa como bit-string todo a cero (canalMascara ya queda
            // así por defecto si cbAuto está marcado, ya que ningún canal lo está).

            boolean escribirMacMin = checkBoxMacMin.isChecked();
            int macMin = 0;
            if (escribirMacMin) {
                try {
                    macMin = Integer.parseInt(editMacMinBandSearchTime.getText().toString().trim());
                } catch (Exception e) {
                    editMacMinBandSearchTime.setError("Valor inválido");
                    editMacMinBandSearchTime.requestFocus();
                    return;
                }
            }

            boolean escribirMacMax = checkBoxMacMax.isChecked();
            int macMax = 0;
            if (escribirMacMax) {
                try {
                    macMax = Integer.parseInt(editMacMaxBandSearchTime.getText().toString().trim());
                } catch (Exception e) {
                    editMacMaxBandSearchTime.setError("Valor inválido");
                    editMacMaxBandSearchTime.requestFocus();
                    return;
                }
            }

            if (!escribirCanal && !escribirMacMin && !escribirMacMax) {
                android.widget.Toast.makeText(this,
                        "Marca al menos una sección para programar",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            boolean escribirCanalFinal = escribirCanal;
            int macMinFinal = macMin;
            int macMaxFinal = macMax;
            boolean escribirMacMinFinal = escribirMacMin;
            boolean escribirMacMaxFinal = escribirMacMax;

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            btnLeerActual.setEnabled(false);
            btnProgramar.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(PrimeCanalActivity.this);

                    PrimeChannelWriter.programarCanalPrime(
                            res.reader, conn.getClient(),
                            escribirCanalFinal, canalMascara,
                            escribirMacMinFinal, macMinFinal,
                            escribirMacMaxFinal, macMaxFinal);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnLeerActual.setEnabled(true);
                        btnProgramar.setEnabled(true);
                        android.widget.Toast.makeText(PrimeCanalActivity.this,
                                "Canal PRIME programado correctamente",
                                android.widget.Toast.LENGTH_LONG).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnLeerActual.setEnabled(true);
                        btnProgramar.setEnabled(true);
                        new androidx.appcompat.app.AlertDialog.Builder(PrimeCanalActivity.this)
                                .setTitle("Error al programar")
                                .setMessage("No se pudo programar el canal PRIME.\n\n" + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .setCancelable(true)
                                .show();
                    });
                } finally {
                    conn.close();
                }
            }).start();
        });
    }

    /**
     * Construye el informe de resultado con un ✓ verde para cada canal activo
     * (en vez del "1" plano de antes) y un guion neutro para los inactivos.
     */
    private SpannableStringBuilder formatearResultadoCanalPrime(PrimeChannelReader.Resultado resultado) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        int colorActivo = ContextCompat.getColor(this, R.color.success);
        int colorInactivo = ContextCompat.getColor(this, R.color.on_surface_variant);

        sb.append("------------------------------\n");
        sb.append("Prime 1.4 Channel selection :\n");
        agregarCanalesConTick(sb, resultado.channelSelection, colorActivo, colorInactivo);

        sb.append("Prime 1.4 Active Channel\n");
        agregarCanalesConTick(sb, resultado.activeChannel, colorActivo, colorInactivo);

        sb.append("Prime 1.4 macMinBandSearchTime : ").append(String.valueOf(resultado.macMin)).append("\n");
        sb.append("Prime 1.4 macMaxBandSearchTime : ").append(String.valueOf(resultado.macMax));

        return sb;
    }

    private void agregarCanalesConTick(SpannableStringBuilder sb, boolean[] canales,
                                       int colorActivo, int colorInactivo) {
        for (int i = 0; i < 8; i++) {
            boolean marcado = (canales != null && i < canales.length) && canales[i];

            sb.append(String.format(" CH%d   : ", i + 1));

            int inicio = sb.length();
            sb.append(marcado ? "✓" : "–");
            int fin = sb.length();

            sb.setSpan(new ForegroundColorSpan(marcado ? colorActivo : colorInactivo),
                    inicio, fin, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (marcado) {
                sb.setSpan(new StyleSpan(Typeface.BOLD),
                        inicio, fin, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            sb.append("\n");
        }
    }
}