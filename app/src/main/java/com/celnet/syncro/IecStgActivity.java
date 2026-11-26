package com.celnet.syncro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Configuración del STG para IEC-870. Igual que StgActivity (DLMS), pero guarda sus datos en el
 * fichero de preferencias de IEC ("iec_ftp_config", el mismo de IecFtpActivity; las claves no
 * coinciden), de modo que no se mezcla con la configuración de DLMS.
 */
public class IecStgActivity extends BaseActivity {

    private static final int CNC_NAME_MAX_LENGTH = 13;

    /** Formatos de los reportes IEC-870 que se generan y envían. */
    public static final String FORMATO_EXCEL = "EXCEL (xlsx)";
    public static final String FORMATO_GISCE = "GISCE (json)";

    /** Valores guardados de la configuración STG de IEC (null = no guardado aún). */
    public static class Config {
        public String cncName = "Syncro";
        public String stgVersion = null;
        public String afterGenerate = null;
        public String afterSend = null;
        public String formato = FORMATO_EXCEL;

        /** true si los reportes deben generarse en JSON (GISCE). */
        public boolean esJson() {
            return FORMATO_GISCE.equals(formato);
        }
    }

    /** Lee la configuración STG de IEC. */
    public static Config cargar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(IecFtpActivity.PREFS_NAME, MODE_PRIVATE);
        Config c = new Config();
        c.cncName = prefs.getString("cncName", "Syncro");
        c.stgVersion = prefs.getString("stgVersion", null);
        c.afterGenerate = prefs.getString("afterGenerate", null);
        c.afterSend = prefs.getString("afterSend", null);
        String f = prefs.getString("formato", FORMATO_EXCEL);
        c.formato = FORMATO_GISCE.equals(f) ? FORMATO_GISCE : FORMATO_EXCEL;
        return c;
    }

    private AutoCompleteTextView spinnerAfterGenerate, spinnerAfterSend, spinnerStgVersion, spinnerFormato;
    private EditText editCncName;
    private TextInputLayout layoutCncName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_stg);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerAfterGenerate = findViewById(R.id.spinnerAfterGenerate);
        spinnerAfterSend = findViewById(R.id.spinnerAfterSend);
        spinnerStgVersion = findViewById(R.id.spinnerStgVersion);
        spinnerFormato = findViewById(R.id.spinnerFormato);
        editCncName = findViewById(R.id.editCncName);
        layoutCncName = (TextInputLayout) editCncName.getParent().getParent();
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);

        configurarDesplegable(spinnerAfterGenerate, R.array.afterGenerate_array);
        configurarDesplegable(spinnerAfterSend, R.array.afterSend_array);
        configurarDesplegable(spinnerStgVersion, R.array.stgVersion_array);
        spinnerFormato.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{FORMATO_EXCEL, FORMATO_GISCE}));

        mostrarConfiguracion();

        btnNext.setOnClickListener(v -> {
            if (!validarFormulario()) {
                return;
            }
            guardarConfiguracion();
            finish();
        });
    }

    /** Asigna las opciones y deja seleccionada la primera. */
    private void configurarDesplegable(AutoCompleteTextView spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            spinner.setText(adapter.getItem(0).toString(), false);
        }
    }

    /** El nombre del concentrador no puede superar CNC_NAME_MAX_LENGTH caracteres. */
    private boolean validarFormulario() {
        String cncName = editCncName.getText() != null ? editCncName.getText().toString() : "";
        if (cncName.length() > CNC_NAME_MAX_LENGTH) {
            layoutCncName.setError("Máximo " + CNC_NAME_MAX_LENGTH + " caracteres");
            editCncName.requestFocus();
            return false;
        }
        layoutCncName.setError(null);
        return true;
    }

    private void guardarConfiguracion() {
        getSharedPreferences(IecFtpActivity.PREFS_NAME, MODE_PRIVATE).edit()
                .putString("cncName", editCncName.getText().toString())
                .putString("stgVersion", spinnerStgVersion.getText().toString())
                .putString("afterGenerate", spinnerAfterGenerate.getText().toString())
                .putString("afterSend", spinnerAfterSend.getText().toString())
                .putString("formato", FORMATO_GISCE.equals(spinnerFormato.getText().toString())
                        ? FORMATO_GISCE : FORMATO_EXCEL)
                .apply();
    }

    private void mostrarConfiguracion() {
        Config c = cargar(this);
        editCncName.setText(c.cncName);
        if (c.stgVersion != null) {
            spinnerStgVersion.setText(c.stgVersion, false);
        }
        if (c.afterGenerate != null) {
            spinnerAfterGenerate.setText(c.afterGenerate, false);
        }
        if (c.afterSend != null) {
            spinnerAfterSend.setText(c.afterSend, false);
        }
        spinnerFormato.setText(c.formato, false);
    }
}