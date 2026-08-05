package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.ParametrosS06;
import com.celnet.syncro.objects.instantValues.ParametersReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.celnet.syncro.utils.AppLogger;
import com.celnet.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class ParametersActivity extends BaseActivity {

    private ParametrosS06 lastParametros;
    private String        lastCntId;

    // ── XML generation ───────────────────────────────────────────────────────

    private String generateXml(ParametrosS06 p, String cncId, String cntId) {
        String fh = "";
        if (p.fecha != null && !p.fecha.equals("N/A")) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(p.fecha,
                        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                fh = ldt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "000S";
            } catch (Exception e) {
                fh = "";
            }
        }

        String fab = p.unesaManufacturer != null && !p.unesaManufacturer.equals("N/A")
                ? " " + p.unesaManufacturer
                : "";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Report IdRpt=\"S06\" IdPet=\"0\" Version=\"3.1.c\">\n" +
                "\t<Cnc Id=\"" + cncId + "\">\n" +
                "\t\t<Cnt Id=\"" + cntId + "\">\n" +
                "\t\t\t<S06" +
                " Fh=\""             + fh                                                   + "\"" +
                " NS=\""             + nvl(p.serialNumber)                                  + "\"" +
                " Fab=\""            + fab                                                  + "\"" +
                " Mod=\""            + nvl(p.unesaModelType)                               + "\"" +
                " Af=\""             + nvl(p.manufacturingYear)                            + "\"" +
                " Te=\""             + nvl(p.typeOfEquipment)                              + "\"" +
                " Vf=\""             + nvl(p.firmwareVersion)                              + "\"" +
                " VPrime=\""         + nvl(p.primeFirmwareVersion)                         + "\"" +
                " Pro=\""            + nvl(p.protocol)                                     + "\"" +
                " Idm=\""            + nvl(p.idComunicMulticast)                           + "\"" +
                " Mac=\""            + nvl(p.primeMacAddress)                              + "\"" +
                " Tp=\"\""  +
                " Ts=\"\""  +
                " Ip=\"\""  +
                " Is=\"\""  +
                " Usag=\""           + p.thresholdVoltageSags                              + "\"" +
                " Uswell=\""         + p.thresholdVoltageSwells                            + "\"" +
                " Per=\""            + p.loadProfilePeriod1                                + "\"" +
                " Dctcp=\""          + String.format("%.2f", p.demandCloseContractedPower) + "\"" +
                " Vr=\""             + p.referenceVoltage                                  + "\"" +
                " Ut=\""             + p.longPowerFailureThreshold                         + "\"" +
                " UsubT=\""          + String.format("%.2f", p.voltageSagThreshold)        + "\"" +
                " UsobT=\""          + String.format("%.2f", p.voltageSwellThreshold)      + "\"" +
                " UcorteT=\""        + String.format("%.2f", p.voltageCutOffThreshold)     + "\"" +
                " AutMothBill=\""    + nvl(p.automaticMonthlyBilling)                      + "\"" +
                " ScrollDispMode=\"" + nvl(p.scrollDisplayMode)                            + "\"" +
                " ScrollDispTime=\"" + p.timeForScrollDisplay                              + "\"" +
                "/>\n" +
                "\t\t</Cnt>\n" +
                "\t</Cnc>\n" +
                "</Report>";
    }

    private String nvl(String s) {
        return (s == null || s.equals("N/A")) ? "" : s;
    }

    // ── Export ───────────────────────────────────────────────────────────────

    private void exportar(ParametrosS06 p, String cntId) {

        SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
        String cncName = prefs.getString("cncName", "Syncro");

        String xml = generateXml(p, cncName, cntId);

        String cncNameClean = cncName.replaceAll("\\s+", "_");
        String fechaActual  = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                .format(new Date());
        String nombreFichero = cncNameClean + "_0_S06_0_" + fechaActual + ".xml";

        File syncroFolder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Syncro/Reports"
        );
        if (!syncroFolder.exists()) syncroFolder.mkdirs();

        File file = new File(syncroFolder, nombreFichero);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(xml.getBytes());
            fos.flush();

            String afterGenerate = prefs.getString(
                    "afterGenerate",
                    "Guardar e intentar enviar al FTP inmediatamente"
            );
            String afterSend = prefs.getString(
                    "afterSend",
                    "Mover a la carpeta de backup del dispositivo"
            );

            if (afterGenerate.equals("Solo guardar para enviar al FTP mas tarde")) {
                Toast.makeText(this, "Archivo guardado para envío posterior", Toast.LENGTH_LONG).show();
                return;
            }

            new Thread(() -> {
                boolean subidaCorrecta = false;
                try {
                    String protocolo = prefs.getString("protocolo", "FTP");

                    if ("SFTP".equalsIgnoreCase(protocolo)) {
                        subidaCorrecta = Utils.subirArchivoSFTP(ParametersActivity.this, file);
                    } else if ("FTPS".equalsIgnoreCase(protocolo)) {
                        subidaCorrecta = Utils.subirArchivoFTPS(ParametersActivity.this, file);
                    } else {
                        subidaCorrecta = Utils.subirArchivoFTP(ParametersActivity.this, file);
                    }

                    if (subidaCorrecta) {
                        Utils.gestionarArchivoTrasEnvio(file, afterSend, ParametersActivity.this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                boolean resultado = subidaCorrecta;
                runOnUiThread(() -> Toast.makeText(
                        ParametersActivity.this,
                        resultado ? "Archivo enviado correctamente" : "Error al enviar archivo",
                        Toast.LENGTH_LONG
                ).show());
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_LONG).show();
        }
    }

    // ── Activity lifecycle ───────────────────────────────────────────────────

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
        LinearLayout btnExport        = findViewById(R.id.btnExport);
        TextView     tvResultado      = findViewById(R.id.tvResultado);
        ConnectionConfig config       = SessionManager.getInstance().getConnectionConfig();

        progressBar.setVisibility(View.VISIBLE);
        layoutResultados.setVisibility(View.GONE);

        // El botón solo actúa si ya hay datos leídos
        btnExport.setOnClickListener(v -> {
            if (lastParametros != null) {
                exportar(lastParametros, lastCntId);
            } else {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            }
        });

        new Thread(() -> {
            DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                    ? new DLMSConnection(config.getBluetoothDeviceName())
                    : new DLMSConnection(config.getIp(), config.getPort());

            try {
                DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(ParametersActivity.this);
                String cntId = res.serialNumber;

                ParametrosS06 p = ParametersReader.read(res.reader);
                conn.close();

                // Guardar para el botón de exportar
                lastParametros = p;
                lastCntId      = cntId;

                String texto = formatResult(p);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutResultados.setVisibility(View.VISIBLE);
                    tvResultado.setText(texto);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    new androidx.appcompat.app.AlertDialog.Builder(ParametersActivity.this)
                            .setTitle("Error de lectura")
                            .setMessage("No se pudieron leer los parametros del contador.\n\n"
                                    + e.getMessage())
                            .setPositiveButton("Aceptar", null)
                            .setCancelable(true)
                            .show();
                });
            } finally {
                conn.close();
            }
        }).start();
    }

    // ── Format for screen display ────────────────────────────────────────────

    private String formatResult(ParametrosS06 p) {
        return "Fecha                                : " + p.fecha                                             + "\n" +
                "Serial number                        : " + p.serialNumber                                     + "\n" +
                "UNESA Manufacturer                   : " + p.unesaManufacturer                               + "\n" +
                "UNESA Model Type                     : " + p.unesaModelType                                  + "\n" +
                "Manufacturing year                   : " + p.manufacturingYear                               + "\n" +
                "Type of equipment                    : " + p.typeOfEquipment                                 + "\n" +
                "Firmware version                     : " + p.firmwareVersion                                 + "\n" +
                "Prime Firmware version               : " + p.primeFirmwareVersion                            + "\n" +
                "Protocol                             : " + p.protocol                                        + "\n" +
                "Id. Comunic. Multicast               : " + p.idComunicMulticast                              + "\n" +
                "Prime MAC address                    : " + p.primeMacAddress                                 + "\n" +
                "Primary voltage [V]                  : " + String.format("%.1f", p.primaryVoltage)           + "\n" +
                "Secondary voltage [V]                : " + String.format("%.1f", p.secondaryVoltage)         + "\n" +
                "Primary current [A]                  : " + String.format("%.1f", p.primaryCurrent)           + "\n" +
                "Secondary current [A]                : " + String.format("%.1f", p.secondaryCurrent)         + "\n" +
                "Threshold Voltage sags [s]           : " + p.thresholdVoltageSags                            + "\n" +
                "Threshold Voltage swells [s]         : " + p.thresholdVoltageSwells                          + "\n" +
                "Load profile Period 1 [s]            : " + p.loadProfilePeriod1                              + "\n" +
                "Demand close contracted power [%]    : " + String.format("%.2f", p.demandCloseContractedPower) + "\n" +
                "Reference voltage [V]                : " + p.referenceVoltage                                + "\n" +
                "Long Power Failure threshold [s]     : " + p.longPowerFailureThreshold                       + "\n" +
                "Voltage sag threshold [%]            : " + String.format("%.2f", p.voltageSagThreshold)      + "\n" +
                "Voltage swell threshold [%]          : " + String.format("%.2f", p.voltageSwellThreshold)    + "\n" +
                "Voltage cut-off threshold [%]        : " + String.format("%.2f", p.voltageCutOffThreshold)   + "\n" +
                "Automatic monthly billing            : " + p.automaticMonthlyBilling                         + "\n" +
                "Scroll Display Mode                  : " + p.scrollDisplayMode                               + "\n" +
                "Time for Scroll Display              : " + p.timeForScrollDisplay;
    }
}