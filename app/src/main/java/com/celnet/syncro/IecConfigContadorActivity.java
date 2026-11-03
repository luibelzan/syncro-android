package com.celnet.syncro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.params.Iec870PortConfig;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/**
 * Configuración serial del contador IEC-870: Baud, DataBits, Paridad y StopBits (igual que la
 * "CONFIGURACIÓN SERIAL" del software funcional, sin el campo Puerto: aquí el transporte es la
 * sonda Bluetooth o TCP). Se guarda en las preferencias "iec_config".
 *
 * OJO: de momento solo se guarda; {@link #cargar(Context)} la devuelve, pero la conexión
 * (Iec870Connection) todavía no la aplica a la sonda.
 */
public class IecConfigContadorActivity extends BaseActivity {

    public static final String PREFS_NAME = "iec_config";
    private static final String KEY_BAUD = "baud";
    private static final String KEY_DATA_BITS = "data_bits";
    private static final String KEY_PARITY = "parity";
    private static final String KEY_STOP_BITS = "stop_bits_txt";

    private static final int[] DATA_BITS = {7, 8};
    private static final String[] PARIDADES = {"None", "Even", "Odd"};
    private static final String[] STOP_BITS = {"1", "1.5", "2"};

    private static final int DEFAULT_BAUD = 9600;
    private static final int DEFAULT_DATA_BITS = 8;
    private static final String DEFAULT_PARITY = "None";
    private static final String DEFAULT_STOP_BITS = "1";

    /** Parámetros seriales guardados. */
    public static class Config {
        public int baud = DEFAULT_BAUD;
        public int dataBits = DEFAULT_DATA_BITS;
        public String parity = DEFAULT_PARITY;   // None / Even / Odd
        public String stopBits = DEFAULT_STOP_BITS;   // "1", "1.5" o "2"

        /** 0 = None, 1 = Even, 2 = Odd (como Iec870PortConfig.codigoParams). */
        public int parityIndex() {
            for (int i = 0; i < PARIDADES.length; i++) {
                if (PARIDADES[i].equalsIgnoreCase(parity)) {
                    return i;
                }
            }
            return 0;
        }
    }

    public static Config cargar(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Config c = new Config();
        c.baud = p.getInt(KEY_BAUD, DEFAULT_BAUD);
        c.dataBits = p.getInt(KEY_DATA_BITS, DEFAULT_DATA_BITS);
        c.parity = p.getString(KEY_PARITY, DEFAULT_PARITY); 
        c.stopBits = p.getString(KEY_STOP_BITS, DEFAULT_STOP_BITS);
        return c;
    }

    private AutoCompleteTextView spinnerBaud, spinnerDataBits, spinnerParity, spinnerStopBits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_config_contador);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerBaud = findViewById(R.id.spinnerBaud);
        spinnerDataBits = findViewById(R.id.spinnerDataBits);
        spinnerParity = findViewById(R.id.spinnerParity);
        spinnerStopBits = findViewById(R.id.spinnerStopBits);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        configurar(spinnerBaud, textos(Iec870PortConfig.BAUDS));
        configurar(spinnerDataBits, textos(DATA_BITS));
        configurar(spinnerParity, PARIDADES);
        configurar(spinnerStopBits, STOP_BITS);

        Config c = cargar(this);
        spinnerBaud.setText(String.valueOf(c.baud), false);
        spinnerDataBits.setText(String.valueOf(c.dataBits), false);
        spinnerParity.setText(PARIDADES[c.parityIndex()], false);
        spinnerStopBits.setText(c.stopBits, false);

        btnNext.setOnClickListener(v -> {
            guardar();
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void configurar(AutoCompleteTextView spinner, String[] opciones) {
        spinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, opciones));
    }

    private static String[] textos(int[] valores) {
        String[] t = new String[valores.length];
        for (int i = 0; i < valores.length; i++) {
            t[i] = String.valueOf(valores[i]);
        }
        return t;
    }

    private void guardar() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_BAUD, entero(spinnerBaud, DEFAULT_BAUD))
                .putInt(KEY_DATA_BITS, entero(spinnerDataBits, DEFAULT_DATA_BITS))
                .putString(KEY_PARITY, spinnerParity.getText().toString())
                .putString(KEY_STOP_BITS, spinnerStopBits.getText().toString().trim())
                .apply();
    }

    private static int entero(AutoCompleteTextView spinner, int porDefecto) {
        try {
            return Integer.parseInt(spinner.getText().toString().trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}