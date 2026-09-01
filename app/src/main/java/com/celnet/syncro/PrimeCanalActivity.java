package com.celnet.syncro;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrimeCanalActivity extends BaseActivity {

    // Código de la opción de Dual Stack que se oculta del desplegable
    // ("3: Dynamic communications - 1.3.6 or 1.4"). Movido desde PrimeSeguridadActivity.
    private static final String CODIGO_DUAL_STACK_OCULTO = "3";

    private MaterialCheckBox cbAuto;
    private List<MaterialCheckBox> canales;

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

        // ===== Dual stack Prime version (movida desde Seguridad PRIME) =====
        MaterialCheckBox cbDualStackSelection = findViewById(R.id.cbDualStackSelection);
        AutoCompleteTextView spinnerDualStackVersion = findViewById(R.id.spinnerDualStackVersion);

        String[] opcionesDualStackCompletas = getResources().getStringArray(R.array.dual_stack_prime_version_array);
        String[] opcionesDualStackVisibles = filtrarOpcionOculta(opcionesDualStackCompletas);

        ArrayAdapter<String> adapterDualStack = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, opcionesDualStackVisibles);
        spinnerDualStackVersion.setAdapter(adapterDualStack);
        // Por defecto: "2: Communications in Prime 1.4 mode"
        spinnerDualStackVersion.setText(opcionesDualStackVisibles[1], false);

        cbDualStackSelection.setOnCheckedChangeListener((buttonView, isChecked) ->
                spinnerDualStackVersion.setEnabled(isChecked));

        // ===== Vistas de resultado / progreso =====
        MaterialCardView cardResultado = findViewById(R.id.cardResultado);
        TextView tvResultado = findViewById(R.id.tvResultado);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

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

            boolean escribirDualStack = cbDualStackSelection.isChecked();
            int dualStackOpcion = 0;
            if (escribirDualStack) {
                String versionSeleccionada = spinnerDualStackVersion.getText().toString();
                try {
                    dualStackOpcion = Integer.parseInt(versionSeleccionada.split(":")[0].trim());
                } catch (Exception e) {
                    new androidx.appcompat.app.AlertDialog.Builder(PrimeCanalActivity.this)
                            .setTitle("Error")
                            .setMessage("Selecciona una versión de Dual Stack válida.")
                            .setPositiveButton("Aceptar", null)
                            .setCancelable(true)
                            .show();
                    return;
                }
            }

            if (!escribirCanal && !escribirDualStack) {
                android.widget.Toast.makeText(this,
                        "Marca al menos una sección para programar",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            boolean escribirCanalFinal = escribirCanal;
            boolean escribirDualStackFinal = escribirDualStack;
            int dualStackOpcionFinal = dualStackOpcion;

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
                            escribirDualStackFinal, dualStackOpcionFinal);

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
     * Elimina del array de opciones la que empieza por
     * {@link #CODIGO_DUAL_STACK_OCULTO} + ":" (p.ej. "3: Dynamic communications...").
     */
    private String[] filtrarOpcionOculta(String[] opcionesOriginales) {
        List<String> filtradas = new ArrayList<>();
        String prefijoOculto = CODIGO_DUAL_STACK_OCULTO + ":";
        for (String opcion : opcionesOriginales) {
            if (!opcion.trim().startsWith(prefijoOculto)) {
                filtradas.add(opcion);
            }
        }
        return filtradas.toArray(new String[0]);
    }

    /**
     * Construye el informe de resultado con un ✓ verde para cada canal activo
     * y muestra la versión de Dual Stack en vez de macMin/macMax (movidos a Seguridad).
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

        sb.append("Dual stack Prime version : ").append(resultado.dualStackVersion);

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