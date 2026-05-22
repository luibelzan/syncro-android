package com.example.syncro.licenses;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.syncro.MainActivity;
import com.example.syncro.R;
import com.example.syncro.licenses.LicenseCheckActivity;
import com.example.syncro.licenses.LicenseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * BluetoothScanActivity
 *
 * Muestra los dispositivos Bluetooth vinculados cuyo nombre contiene "tespro".
 * Al seleccionar uno:
 *   - Si ya hay licencia guardada para esa MAC → va a MainActivity
 *   - Si no → va a LicenseCheckActivity pasando la MAC
 *
 * Esta actividad es el punto de entrada principal de la app.
 */
public class BluetoothScanActivity extends AppCompatActivity {

    private static final String DEVICE_FILTER = "tespro"; // filtro por nombre

    private ListView   lvDevices;
    private Button     btnScan;
    private LinearLayout progressContainer;
    private TextView   tvEmpty;

    private final List<BluetoothDevice> foundDevices  = new ArrayList<>();
    private final List<String>          deviceLabels  = new ArrayList<>();
    private ArrayAdapter<String>        adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scan);

        lvDevices        = findViewById(R.id.lvDevices);
        btnScan          = findViewById(R.id.btnScan);
        progressContainer = findViewById(R.id.progressContainer);
        tvEmpty          = findViewById(R.id.tvEmpty);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceLabels);
        lvDevices.setAdapter(adapter);

        btnScan.setOnClickListener(v -> scanDevices());

        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = foundDevices.get(position);
            String mac = device.getAddress();
            onDeviceSelected(mac);
        });

        // Escanear automáticamente al abrir
        scanDevices();
    }

    private void scanDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN}, 1);
            return;
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Toast.makeText(this, "Activa el Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        foundDevices.clear();
        deviceLabels.clear();
        adapter.notifyDataSetChanged();

        progressContainer.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Buscar entre dispositivos ya vinculados (paired)
        Set<BluetoothDevice> paired = btAdapter.getBondedDevices();
        for (BluetoothDevice device : paired) {
            String name = device.getName();
            if (name != null && name.toLowerCase().contains(DEVICE_FILTER)) {
                foundDevices.add(device);
                deviceLabels.add(name + "\n" + device.getAddress());
            }
        }

        progressContainer.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();

        if (foundDevices.isEmpty()) {
            tvEmpty.setText("No se encontraron sondas \"" + DEVICE_FILTER +
                    "\".\nAsegúrate de que está vinculada en Ajustes de Bluetooth.");
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void onDeviceSelected(String mac) {
        // Guardar MAC en SessionManager para la conexión posterior
        // SessionManager.getInstance().setSelectedMac(mac);  ← adapta a tu SessionManager

        if (LicenseManager.hasLicense(this)) {
            // Ya hay una licencia en caché → verificar que es para esta MAC
            String cachedMac = LicenseManager.getCachedMac(this);
            if (cachedMac != null && cachedMac.equalsIgnoreCase(mac)) {
                // Todo OK → ir a MainActivity
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                // La sonda no coincide con la licenciada → pedir activación
                goToLicenseCheck(mac);
            }
        } else {
            // Sin licencia → pedir activación
            goToLicenseCheck(mac);
        }
    }

    private void goToLicenseCheck(String mac) {
        Intent intent = new Intent(this, LicenseCheckActivity.class);
        intent.putExtra("device_mac", mac);
        startActivity(intent);
    }
}