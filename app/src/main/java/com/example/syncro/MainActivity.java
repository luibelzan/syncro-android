package com.example.syncro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
    private RadioGroup radioGroupConexion;
    private LinearLayout layoutBluetooth;
    private LinearLayout layoutTcp;
    private String dispositivo;
    private String ip;
    private int port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        // Referencias
        radioGroupConexion = findViewById(R.id.radioGroupConexion);
        layoutBluetooth = findViewById(R.id.layoutBluetooth);
        layoutTcp = findViewById(R.id.layoutTcp);

        // Mostrar layout inicial según selección por defecto
        layoutBluetooth.setVisibility(View.VISIBLE);
        layoutTcp.setVisibility(View.GONE);

        if (radioGroupConexion.getCheckedRadioButtonId() == R.id.rbTcp) {
            layoutBluetooth.setVisibility(View.GONE);
            layoutTcp.setVisibility(View.VISIBLE);
        }

        // Listener para cambio de selección
        radioGroupConexion.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.rbBluetooth) {

                    layoutBluetooth.setVisibility(View.VISIBLE);
                    layoutTcp.setVisibility(View.GONE);

                } else if (checkedId == R.id.rbTcp) {

                    layoutBluetooth.setVisibility(View.GONE);
                    layoutTcp.setVisibility(View.VISIBLE);
                }
            }
        });

        // 🔹 Inicializar Spinner AQUÍ
        Spinner spinnerSonda = findViewById(R.id.spinnerSonda);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sondas_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSonda.setAdapter(adapter);

        spinnerSonda.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String seleccion = parent.getItemAtPosition(position).toString();
                // Aquí puedes usar la selección
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });

        ImageButton btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);

            }
        });



    }
}