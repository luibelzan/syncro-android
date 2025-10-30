package com.example.syncro;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.syncro.client.DLMSConnection;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.utils.Utils;

public class MainActivity extends AppCompatActivity {

    private TextView txtStatus;
    private Button btnConnect;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        txtStatus = findViewById(R.id.txtStatus);
        btnConnect = findViewById(R.id.btnConnect);

        // 🔹 Verificar permisos Bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }

        btnConnect.setOnClickListener(v -> {
            txtStatus.setText("Conectando...");
            new Thread(() -> {
                try {
                    Utils.controlDisconnectMode(this, false);
                    runOnUiThread(() -> {
                        txtStatus.setText("Conectado correctamente");
                        Toast.makeText(this, "Conexión DLMS activa", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        txtStatus.setText("Error de conexión: " + e.getMessage());
                    });
                }
            }).start();
        });
    }
}