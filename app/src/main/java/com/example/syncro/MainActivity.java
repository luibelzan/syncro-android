package com.example.syncro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;

public class MainActivity extends AppCompatActivity {

    private TextView txtStatus;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        txtStatus = findViewById(R.id.txtStatus);
        btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(v -> {
            txtStatus.setText("Conectando...");
            new Thread(() -> {
                try {
                    String portName = "COM5";

                    DLMSConnection connection = DLMSConnection.initializeConnection(portName);

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