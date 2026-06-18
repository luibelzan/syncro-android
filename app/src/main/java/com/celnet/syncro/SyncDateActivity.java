package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.objects.params.DateReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SyncDateActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sync_date);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText editDateTime = findViewById(R.id.editDateTime);
        EditText editTimezone = findViewById(R.id.editTimezone);
        CheckBox checkBoxManual = findViewById(R.id.checkBoxManual);
        ImageButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);

        // Fecha y hora actual
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

        String fechaActual = sdf.format(new Date());

        // Establecer valores por defecto
        editDateTime.setText(fechaActual);
        editTimezone.setText("120");

        // Estado inicial (bloqueados)
        setEditable(false, editDateTime, editTimezone);

        // Listener del checkbox
        checkBoxManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setEditable(isChecked, editDateTime, editTimezone);
        });

        btnNext.setOnClickListener(v -> {
            String dateTimeStr = editDateTime.getText().toString().trim();
            String timezoneStr = editTimezone.getText().toString().trim();
            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            Date dateTime;
            int utcOffsetMinutes;

            try {
                dateTime = sdf.parse(dateTimeStr); // <-- reutiliza el sdf de arriba, sin redeclarar
            } catch (Exception e) {
                Toast.makeText(this, "Formato de fecha incorrecto (yyyy/MM/dd HH:mm)", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                utcOffsetMinutes = Integer.parseInt(timezoneStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "La zona horaria debe ser un número entero (ej: 120)", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Deshabilitar botón mientras se ejecuta
            btnNext.setEnabled(false);

            final Date finalDateTime = dateTime;
            final int finalOffset = utcOffsetMinutes;

            progressBar.setVisibility(View.VISIBLE);

            // 3. Ejecutar en hilo secundario (DLMS bloquea la UI)
            new Thread(() -> {
                try {
                    DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                            ? new DLMSConnection(config.getBluetoothDeviceName())
                            : new DLMSConnection(config.getIp(), config.getPort());

                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(SyncDateActivity.this);


                    GXDLMSSecureClient2 client = conn.getClient();

                    DateReader.syncClock(res.reader, client, finalOffset, finalDateTime);
                    conn.close();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnNext.setEnabled(true);

                        Toast.makeText(SyncDateActivity.this,
                                "Fecha y hora sincronizada correctamente",
                                Toast.LENGTH_LONG).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    // Toda actualización de UI dentro de runOnUiThread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SyncDateActivity.this,
                                "Error de conexión: " + e.getClass().getSimpleName() +
                                        " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }

    private void setEditable(boolean enabled, EditText... fields) {
        for (EditText et : fields) {
            et.setEnabled(enabled);
            et.setFocusable(enabled);
            et.setFocusableInTouchMode(enabled);
            et.setCursorVisible(enabled);
        }
    }
}