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
import com.example.syncro.objects.LoadProfileReader;
import com.example.syncro.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


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
                    // === Ajustar rango solicitado ===
                    Calendar calFrom = Calendar.getInstance();
                    calFrom.set(2026, Calendar.JANUARY, 20, 0, 0, 0); // 1/11/2025 00:00
                    Date from = calFrom.getTime();

                    Calendar calTo = Calendar.getInstance();
                    calTo.set(2026, Calendar.JANUARY, 21, 0, 0, 0); // 2/11/2025 00:00
                    Date to = calTo.getTime();

                    List<List<Object>> registros = LoadProfileReader.readLoadProfileDayByDay(this, from, to);

                    System.out.println("Registros recibidos: " + registros.size());
                    for (List<Object> fila : registros) {
                        System.out.println(fila);
                    }
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