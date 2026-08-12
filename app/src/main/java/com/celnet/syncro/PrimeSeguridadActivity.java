package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.CurvaFila;
import com.celnet.syncro.models.PrimeSecurityData;
import com.celnet.syncro.models.prime.ConstellationCoding;
import com.celnet.syncro.models.prime.PrimeSecurityInfo;
import com.celnet.syncro.objects.loadProfiles.LoadProfileReader;
import com.celnet.syncro.objects.prime.PrimeSecurityReader;
import com.celnet.syncro.objects.prime.PrimeSecurityWriter;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.celnet.syncro.utils.AppLogger;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrimeSeguridadActivity extends BaseActivity {

    private MaterialCheckBox cbMaster;
    private List<MaterialCheckBox> bitCheckBoxes;

    // Evita bucles infinitos entre el listener del maestro y el de los hijos
    private boolean actualizandoDesdeMaster = false;

    private final CompoundButton.OnCheckedChangeListener listenerMaster =
            (buttonView, isChecked) -> {
                actualizarHabilitacionHijos(isChecked);
            };

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

        //actualizarEstadoMaster();

        // Bits marcados por defecto: 0,1,2,4,5,6,12,13
        int[] bitsPorDefecto = {0, 1, 2, 4, 5, 6, 12, 13};
        boolean[] estadoInicial = new boolean[bitCheckBoxes.size()];
        for (int bit : bitsPorDefecto) {
            estadoInicial[bit] = true;
        }
        for (int i = 0; i < bitCheckBoxes.size(); i++) {
            bitCheckBoxes.get(i).setChecked(estadoInicial[i]);
        }

        // El maestro empieza activado: se enviará esta configuración por defecto al programar
        cbMaster.setChecked(true);
        actualizarHabilitacionHijos(true);

        // El maestro marca/desmarca todos los hijos
        cbMaster.setOnCheckedChangeListener(listenerMaster);

        // Desplegable de versión Dual Stack Prime
        AutoCompleteTextView spinnerDualStackVersion = findViewById(R.id.spinnerDualStackVersion);
        String[] opcionesDualStack = getResources().getStringArray(R.array.dual_stack_prime_version_array);
        ArrayAdapter<String> adapterDualStack = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, opcionesDualStack);
        spinnerDualStackVersion.setAdapter(adapterDualStack);
        // Por defecto: "3: Dynamic communications - 1.3.6 or 1.4"
        spinnerDualStackVersion.setText(opcionesDualStack[2], false);

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        ExtendedFloatingActionButton btnLeerActual = findViewById(R.id.btnLeerActual);
        ExtendedFloatingActionButton btnProgramar = findViewById(R.id.btnProgramar);

        btnLeerActual.setOnClickListener(v -> leerActual(progressBar, btnLeerActual, btnProgramar,
                spinnerDualStackVersion, opcionesDualStack));

        btnProgramar.setOnClickListener(v -> programar(progressBar, btnLeerActual, btnProgramar,
                spinnerDualStackVersion));
    }

    private void leerActual(LinearLayout progressBar,
                            ExtendedFloatingActionButton btnLeerActual,
                            ExtendedFloatingActionButton btnProgramar,
                            AutoCompleteTextView spinnerDualStackVersion,
                            String[] opcionesDualStack) {

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
                    volcarEnFormulario(datos, spinnerDualStackVersion, opcionesDualStack);
                });

            } catch (Exception e) {
                e.printStackTrace();
                DLMSConnection finalConn = conn;
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
                           AutoCompleteTextView spinnerDualStackVersion) {

        boolean enviarConstellationCoding = cbMaster.isChecked();

        boolean[] bits = new boolean[bitCheckBoxes.size()];
        for (int i = 0; i < bitCheckBoxes.size(); i++) {
            bits[i] = bitCheckBoxes.get(i).isChecked();
        }
        ConstellationCoding constellation = ConstellationCoding.fromBitArray(bits);

        String versionSeleccionada = spinnerDualStackVersion.getText().toString();
        int opcionDualStack;
        try {
            opcionDualStack = Integer.parseInt(versionSeleccionada.split(":")[0].trim());
        } catch (Exception e) {
            mostrarError("Selecciona una versión de Dual Stack válida.", e);
            return;
        }

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
                        res.reader, constellation, enviarConstellationCoding, opcionDualStack);

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

    /** Rellena checkboxes y spinner con lo leído del contador */
    private void volcarEnFormulario(PrimeSecurityInfo datos,
                                    AutoCompleteTextView spinnerDualStackVersion,
                                    String[] opcionesDualStack) {

        boolean[] bits = datos.getConstellationCoding().toBitArray();
        for (int i = 0; i < bitCheckBoxes.size(); i++) {
            bitCheckBoxes.get(i).setChecked(bits[i]);
        }

        cbMaster.setChecked(true);
        actualizarHabilitacionHijos(true);

        for (String opcion : opcionesDualStack) {
            if (opcion.startsWith(datos.getDualStackVersionCode() + ":")) {
                spinnerDualStackVersion.setText(opcion, false);
                break;
            }
        }
    }

    private void actualizarEstadoMaster() {
        boolean todosMarcados = true;
        for (MaterialCheckBox cb : bitCheckBoxes) {
            if (!cb.isChecked()) {
                todosMarcados = false;
                break;
            }
        }
        cbMaster.setOnCheckedChangeListener(null);
        cbMaster.setChecked(todosMarcados);
        cbMaster.setOnCheckedChangeListener(listenerMaster);
    }

    private void actualizarHabilitacionHijos(boolean habilitados) {
        for (MaterialCheckBox cb : bitCheckBoxes) {
            cb.setEnabled(habilitados);
        }
    }
}