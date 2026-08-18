package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Arrays;

import gurux.dlms.enums.Authentication;

public class ConfigContadorActivity extends BaseActivity {

    // Claves para SharedPreferences
    public static final String PREFS_NAME          = "dlms_config";
    public static final String KEY_PASSWORD        = "password";
    public static final String KEY_AUTHENTICATION  = "authentication";
    public static final String KEY_CLIENT_ADDRESS  = "client_address";
    public static final String KEY_LOGICAL_DEVICE  = "logical_device";
    public static final String KEY_PHYSICAL_DEVICE = "physical_device";
    public static final String KEY_ADDRESS_SIZE    = "address_size";


    // Valores por defecto (los que tenías en DLMSConnection)
    private static final String  DEFAULT_PASSWORD        = "00000002";
    private static final String  DEFAULT_AUTHENTICATION  = "LOW";
    private static final int     DEFAULT_CLIENT_ADDRESS  = 1;
    private static final int     DEFAULT_LOGICAL_DEVICE  = 1;
    private static final int     DEFAULT_PHYSICAL_DEVICE = 16;
    private static final int     DEFAULT_ADDRESS_SIZE    = 1;
    private static final int     DEFAULT_MAX_PDU         = 236;

    private MaterialAutoCompleteTextView spinnerAuth;
    private MaterialAutoCompleteTextView spinnerAddressSize;
    private EditText etPassword;
    private EditText etClientAddress;
    private EditText etLogicalDevice;
    private EditText etPhysicalDevice;
    private SharedPreferences prefs;

    private String[] authOptions;
    private String[] addressSizeOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_contador);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bindViews();
        setupAuthDropdown();
        setupAddressSizeDropdown();
        loadSavedValues();
    }

    private void bindViews() {
        spinnerAuth        = findViewById(R.id.spinnerAuthentication);
        spinnerAddressSize = findViewById(R.id.spinnerAddressSize);
        etPassword         = findViewById(R.id.etPassword);
        etClientAddress    = findViewById(R.id.etClientAddress);
        etLogicalDevice    = findViewById(R.id.etLogicalDevice);
        etPhysicalDevice   = findViewById(R.id.etPhysicalDevice);
        ExtendedFloatingActionButton btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            if (saveConfig()) {
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAuthDropdown() {
        authOptions = new String[]{
                Authentication.NONE.toString(),
                Authentication.LOW.toString(),
                Authentication.HIGH.toString()
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, authOptions);
        spinnerAuth.setAdapter(adapter);
    }

    private void setupAddressSizeDropdown() {
        addressSizeOptions = new String[]{"Auto (0)", "1", "2", "4"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, addressSizeOptions);
        spinnerAddressSize.setAdapter(adapter);
    }

    private void loadSavedValues() {
        // Contraseña
        etPassword.setText(prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD));

        // Tipo de autenticación
        String savedAuth = prefs.getString(KEY_AUTHENTICATION, DEFAULT_AUTHENTICATION);
        for (String opcion : authOptions) {
            if (opcion.equalsIgnoreCase(savedAuth)) {
                spinnerAuth.setText(opcion, false);
                break;
            }
        }

        // Direcciones
        etClientAddress.setText(String.valueOf(
                prefs.getInt(KEY_CLIENT_ADDRESS, DEFAULT_CLIENT_ADDRESS)));
        etLogicalDevice.setText(String.valueOf(
                prefs.getInt(KEY_LOGICAL_DEVICE, DEFAULT_LOGICAL_DEVICE)));
        etPhysicalDevice.setText(String.valueOf(
                prefs.getInt(KEY_PHYSICAL_DEVICE, DEFAULT_PHYSICAL_DEVICE)));

        // Tamaño de dirección de servidor
        int savedSize = prefs.getInt(KEY_ADDRESS_SIZE, DEFAULT_ADDRESS_SIZE);
        int sizeIdx = addressSizeToIndex(savedSize);
        spinnerAddressSize.setText(addressSizeOptions[sizeIdx], false);
    }

    /**
     * Valida y guarda la configuración en SharedPreferences.
     * @return true si todo es válido, false si hay errores.
     */
    private boolean saveConfig() {
        // -- Validación básica --
        String password = etPassword.getText().toString().trim();
        if (password.isEmpty()) {
            etPassword.setError("La contraseña no puede estar vacía");
            etPassword.requestFocus();
            return false;
        }

        int clientAddress, logicalDevice, physicalDevice;
        try {
            clientAddress  = Integer.parseInt(etClientAddress.getText().toString().trim());
            logicalDevice  = Integer.parseInt(etLogicalDevice.getText().toString().trim());
            physicalDevice = Integer.parseInt(etPhysicalDevice.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Los valores numéricos no son válidos", Toast.LENGTH_SHORT).show();
            return false;
        }

        // -- Persistencia --
        String selectedAuth = spinnerAuth.getText().toString();

        int posicionAddrSize = Arrays.asList(addressSizeOptions)
                .indexOf(spinnerAddressSize.getText().toString());
        int selectedAddrSize = indexToAddressSize(posicionAddrSize);

        prefs.edit()
                .putString(KEY_PASSWORD,        password)
                .putString(KEY_AUTHENTICATION,  selectedAuth)
                .putInt(KEY_CLIENT_ADDRESS,      clientAddress)
                .putInt(KEY_LOGICAL_DEVICE,      logicalDevice)
                .putInt(KEY_PHYSICAL_DEVICE,     physicalDevice)
                .putInt(KEY_ADDRESS_SIZE,        selectedAddrSize)
                .apply();

        return true;
    }

    // --- Helpers dropdown Address Size ---
    private int addressSizeToIndex(int size) {
        switch (size) {
            case 1:  return 1;
            case 2:  return 2;
            case 4:  return 3;
            default: return 0; // Auto
        }
    }

    private int indexToAddressSize(int index) {
        switch (index) {
            case 1:  return 1;
            case 2:  return 2;
            case 3:  return 4;
            default: return 0; // Auto
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------------------------------------
    // Método estático de utilidad: aplica la config guardada sobre DLMSConnection
    // Úsalo en DLMSConnection.configurarClienteDlms() o antes de conectar.
    // -----------------------------------------------------------------------
    public static DLMSConfigValues loadConfig(android.content.Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        DLMSConfigValues v  = new DLMSConfigValues();
        v.password        = p.getString(KEY_PASSWORD, DEFAULT_PASSWORD);
        v.authentication = parseAuthentication(
                p.getString(KEY_AUTHENTICATION, "Low"));
        v.clientAddress   = p.getInt(KEY_CLIENT_ADDRESS,  DEFAULT_CLIENT_ADDRESS);
        v.logicalDevice   = p.getInt(KEY_LOGICAL_DEVICE,  DEFAULT_LOGICAL_DEVICE);
        v.physicalDevice  = p.getInt(KEY_PHYSICAL_DEVICE, DEFAULT_PHYSICAL_DEVICE);
        v.addressSize     = p.getInt(KEY_ADDRESS_SIZE,    DEFAULT_ADDRESS_SIZE);
        return v;
    }

    /** DTO simple para pasar la config a DLMSConnection */
    public static class DLMSConfigValues {
        public String         password;
        public Authentication authentication;
        public int            clientAddress;
        public int            logicalDevice;
        public int            physicalDevice;
        public int            addressSize;
        public int            maxPdu;
    }

    public static Authentication parseAuthentication(String value) {
        for (Authentication a : Authentication.values()) {
            if (a.name().equalsIgnoreCase(value)) {
                return a;
            }
        }
        return Authentication.LOW; // fallback
    }
}