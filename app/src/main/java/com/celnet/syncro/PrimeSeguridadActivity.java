package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.prime.ConstellationCoding;
import com.celnet.syncro.models.prime.PrimeSecurityInfo;
import com.celnet.syncro.objects.prime.PrimeSecurityReader;
import com.celnet.syncro.objects.prime.PrimeSecurityWriter;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.celnet.syncro.utils.AppLogger;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class PrimeSeguridadActivity extends BaseActivity {

    private MaterialCheckBox cbMaster;
    private List<MaterialCheckBox> bitCheckBoxes;

    private final CompoundButton.OnCheckedChangeListener listenerMaster =
            (buttonView, isChecked) -> actualizarHabilitacionHijos(isChecked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prime_seguridad);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cbMaster = findViewById(R.id.cbConstellationMaster);

        bitCheckBoxes = Arrays.asList(
                (MaterialCheckBox) findViewById(R.id.cbBit0),
                (MaterialCheckBox) findViewById(R.id.cbBit1),
                (MaterialCheckBox) findViewById(R.id.cbBit2),
                (MaterialCheckBox) findViewById(R.id.cbBit3),
                (MaterialCheckBox) findViewById(R.id.cbBit4),
                (MaterialCheckBox) findViewById(R.id.cbBit5),
                (MaterialCheckBox) findViewById(R.id.cbBit6),
                (MaterialCheckBox) findViewById(R.id.cbBit7),
                (MaterialCheckBox) findViewById(R.id.cbBit8),
                (MaterialCheckBox) findViewById(R.id.cbBit9),
                (MaterialCheckBox) findViewById(R.id.cbBit10),
                (MaterialCheckBox) findViewById(R.id.cbBit11),
                (MaterialCheckBox) findViewById(R.id.cbBit12),
                (MaterialCheckBox) findViewById(R.id.cbBit13),
                (MaterialCheckBox) findViewById(R.id.cbBit14),
                (MaterialCheckBox) findViewById(R.id.cbBit15)
        );

        // Bits marcados por defecto: 0,1,2,4,5,6,12,13
        int[] bitsPorDefecto = {0, 1, 2, 4, 5, 6, 12, 13};
        boolean[] estadoInicial = new boolean[bitCheckBoxes.size()];
        for (int bit : bitsPorDefecto) {
            estadoInicial[bit] = true;
        }
        for (int i = 0; i < bitCheckBoxes.size(); i++) {
            bitCheckBoxes.get(i).setChecked(estadoInicial[i]);
        }

        cbMaster.setChecked(false);
        actualizarHabilitacionHijos(false);
        cbMaster.setOnCheckedChangeListener(listenerMaster);

        // ===== macMaxBandSearchTime / macMinBandSearchTime (movidas desde Canal PRIME) =====
        MaterialCheckBox checkBoxMacMax = findViewById(R.id.checkBoxMacMax);
        TextInputEditText editMacMaxBandSearchTime = findViewById(R.id.editMacMaxBandSearchTime);
        checkBoxMacMax.setOnCheckedChangeListener((buttonView, isChecked) ->
                editMacMaxBandSearchTime.setEnabled(isChecked));

        MaterialCheckBox checkBoxMacMin = findViewById(R.id.checkBoxMacMin);
        TextInputEditText editMacMinBandSearchTime = findViewById(R.id.editMacMinBandSearchTime);
        checkBoxMacMin.setOnCheckedChangeListener((buttonView, isChecked) ->
                editMacMinBandSearchTime.setEnabled(isChecked));

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnLeerActual = findViewById(R.id.btnLeerActual);
        ExtendedFloatingActionButton btnProgramar = findViewById(R.id.btnProgramar);

        btnLeerActual.setOnClickListener(v -> leerActual(progressBar, btnLeerActual, btnProgramar));

        btnProgramar.setOnClickListener(v -> programar(progressBar, btnLeerActual, btnProgramar,
                checkBoxMacMin, editMacMinBandSearchTime,
                checkBoxMacMax, editMacMaxBandSearchTime));
    }

    private void leerActual(LinearLayout progressBar,
                            ExtendedFloatingActionButton btnLeerActual,
                            ExtendedFloatingActionButton btnProgramar) {

        progressBar.setVisibility(View.VISIBLE);
        btnLeerActual.setEnabled(false);
        btnProgramar.setEnabled(false);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        new Thread(() -> {
            DLMSConnection conn = null;
            try {
                conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(this);
                PrimeSecurityInfo datos = PrimeSecurityReader.leerSeguridadPrime(res.reader);

                AppLogger.i("PrimeSeguridad", "Resultado lectura seguridad PRIME:\n" + datos.toString());

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);

                    Intent intent = new Intent(PrimeSeguridadActivity.this,
                            ResultadosPrimeSeguridadActivity.class);
                    intent.putExtra(ResultadosPrimeSeguridadActivity.EXTRA_INFO, datos);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);
                    mostrarError("No se pudo leer la seguridad PRIME.", e);
                });
            } finally {
                if (conn != null) conn.close();
            }
        }).start();
    }

    private void programar(LinearLayout progressBar,
                           ExtendedFloatingActionButton btnLeerActual,
                           ExtendedFloatingActionButton btnProgramar,
                           MaterialCheckBox checkBoxMacMin, TextInputEditText editMacMin,
                           MaterialCheckBox checkBoxMacMax, TextInputEditText editMacMax) {

        boolean enviarConstellationCoding = cbMaster.isChecked();

        boolean[] bits = new boolean[bitCheckBoxes.size()];
        for (int i = 0; i < bitCheckBoxes.size(); i++) {
            bits[i] = bitCheckBoxes.get(i).isChecked();
        }
        ConstellationCoding constellation = ConstellationCoding.fromBitArray(bits);

        boolean escribirMacMin = checkBoxMacMin.isChecked();
        int macMin = 0;
        if (escribirMacMin) {
            try {
                macMin = Integer.parseInt(editMacMin.getText().toString().trim());
            } catch (Exception e) {
                editMacMin.setError("Valor inválido");
                editMacMin.requestFocus();
                return;
            }
        }

        boolean escribirMacMax = checkBoxMacMax.isChecked();
        int macMax = 0;
        if (escribirMacMax) {
            try {
                macMax = Integer.parseInt(editMacMax.getText().toString().trim());
            } catch (Exception e) {
                editMacMax.setError("Valor inválido");
                editMacMax.requestFocus();
                return;
            }
        }

        if (!enviarConstellationCoding && !escribirMacMin && !escribirMacMax) {
            Toast.makeText(this, "Marca al menos una sección para programar", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean escribirMacMinFinal = escribirMacMin;
        int macMinFinal = macMin;
        boolean escribirMacMaxFinal = escribirMacMax;
        int macMaxFinal = macMax;

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        btnLeerActual.setEnabled(false);
        btnProgramar.setEnabled(false);

        new Thread(() -> {
            DLMSConnection conn = null;
            try {
                conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(this);

                PrimeSecurityWriter.programarSeguridadPrime(
                        res.reader, constellation, enviarConstellationCoding,
                        escribirMacMinFinal, macMinFinal,
                        escribirMacMaxFinal, macMaxFinal);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);
                    Toast.makeText(this, "Seguridad PRIME programada correctamente", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLeerActual.setEnabled(true);
                    btnProgramar.setEnabled(true);
                    mostrarError("No se pudo programar la seguridad PRIME.", e);
                });
            } finally {
                if (conn != null) conn.close();
            }
        }).start();
    }

    private void mostrarError(String titulo, Exception e) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(titulo + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .setCancelable(true)
                .show();
    }

    private void actualizarHabilitacionHijos(boolean habilitados) {
        for (MaterialCheckBox cb : bitCheckBoxes) {
            cb.setEnabled(habilitados);
        }
    }
}