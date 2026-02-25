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

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;
import com.example.syncro.objects.events.StandarEventLogReader;
import com.example.syncro.objects.instantValues.InstantaneousValuesReader;
import com.example.syncro.objects.params.DateReader;
import com.example.syncro.utils.MeterData;


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
                    DLMSConnection conn = new DLMSConnection("prueba");
                    GXDLMSReader reader = conn.bluetoothConnnect(this);
                    GXDLMSSecureClient2 client = conn.getClient();

                    //TARIFICACION
                    //BillingDataReader.readBillingDataContract1(reader);
                    //CurrentBillingReader.readCurrentBilling(reader);

                    //CURVAS
                    //LoadProfileReader.leerCurvaCarga(reader, "2026/02/09", "2026/02/10");
                    
                    //PARAMETROS
                    //DateReader.readDate(reader);
                    //DateReader.syncClock(reader, client);
                    //SerialNumberReader.readSerialNumer(reader);

                    //EVENTOS
                    //StandarEventLogReader.readStandarEventLog(reader);

                    //VALORES INSTANTANEOS
                    InstantaneousValuesReader.readMeterData(reader);
                    conn.close();


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