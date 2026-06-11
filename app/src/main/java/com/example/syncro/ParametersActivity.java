package com.example.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.models.ParametrosS06;
import com.example.syncro.objects.instantValues.ParametersReader;
import com.example.syncro.session.ConnectionConfig;
import com.example.syncro.session.SessionManager;
import com.example.syncro.utils.AppLogger;

public class ParametersActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parameters);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout progressBar      = findViewById(R.id.progressContainer);
        LinearLayout layoutResultados = findViewById(R.id.layoutResultados);
        TextView     tvResultado      = findViewById(R.id.tvResultado);
        ConnectionConfig config       = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        layoutResultados.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(ParametersActivity.this);

                ParametrosS06 p = ParametersReader.read(res.reader);
                conn.close();

                String texto = formatResult(p);

                AppLogger.i("ParametrosS06", texto);


                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutResultados.setVisibility(View.VISIBLE);
                    tvResultado.setText(texto);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ParametersActivity.this,
                            "Error de conexión: " + e.getClass().getSimpleName() +
                                    " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String formatResult(ParametrosS06 p) {
        return "Fecha                                : " + p.fecha                                      + "\n" +
                "Serial number                        : " + p.serialNumber                              + "\n" +
                "UNESA Manufacturer                   : " + p.unesaManufacturer                        + "\n" +
                "UNESA Model Type                     : " + p.unesaModelType                           + "\n" +
                "Manufacturing year                   : " + p.manufacturingYear                          + "\n" +
                "Type of equipment                    : " + p.typeOfEquipment                          + "\n" +
                "Firmware version                     : " + p.firmwareVersion                          + "\n" +
                "Prime Firmware version               : " + p.primeFirmwareVersion                     + "\n" +
                "Protocol                             : " + p.protocol                                  + "\n" +
                "Id. Comunic. Multicast               : " + p.idComunicMulticast                       + "\n" +
                "Prime MAC address                    : " + p.primeMacAddress                          + "\n" +
                "Primary voltage [V]                  : " + String.format("%.1f", p.primaryVoltage)    + "\n" +
                "Secondary voltage [V]                : " + String.format("%.1f", p.secondaryVoltage)  + "\n" +
                "Primary current [A]                  : " + String.format("%.1f", p.primaryCurrent)    + "\n" +
                "Secondary current [A]                : " + String.format("%.1f", p.secondaryCurrent)  + "\n" +
                "Threshold Voltage sags [s]           : " + p.thresholdVoltageSags                     + "\n" +
                "Threshold Voltage swells [s]         : " + p.thresholdVoltageSwells                   + "\n" +
                "Load profile Period 1 [s]            : " + p.loadProfilePeriod1                       + "\n" +
                "Demand close contracted power [%]    : " + String.format("%.2f", p.demandCloseContractedPower) + "\n" +
                "Reference voltage [V]                : " + p.referenceVoltage                         + "\n" +
                "Long Power Failure threshold [s]     : " + p.longPowerFailureThreshold                + "\n" +
                "Voltage sag threshold [%]            : " + String.format("%.2f", p.voltageSagThreshold)      + "\n" +
                "Voltage swell threshold [%]          : " + String.format("%.2f", p.voltageSwellThreshold)    + "\n" +
                "Voltage cut-off threshold [%]        : " + String.format("%.2f", p.voltageCutOffThreshold)   + "\n" +
                "Automatic monthly billing            : " + p.automaticMonthlyBilling                  + "\n" +
                "Scroll Display Mode                  : " + p.scrollDisplayMode                        + "\n" +
                "Time for Scroll Display              : " + p.timeForScrollDisplay;
    }
}