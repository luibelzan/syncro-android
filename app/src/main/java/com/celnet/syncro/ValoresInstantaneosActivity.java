package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.client.DLMSConnection;
import com.celnet.syncro.models.instantvalues.RegistroS29;
import com.celnet.syncro.models.instantvalues.TipoLecturaInstantanea;
import com.celnet.syncro.objects.instantValues.InstantaneousValuesReader;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ValoresInstantaneosActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_valores_instantaneos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout progressBar = findViewById(R.id.progressContainer);
        MaterialCardView cardResultados = findViewById(R.id.cardResultados);
        LinearLayout layoutResultados = findViewById(R.id.layoutResultados);
        TextView tvResultado = findViewById(R.id.tvResultado);
        MaterialCardView cardSelector = findViewById(R.id.cardSelector);

        AutoCompleteTextView spinnerTipoLectura = findViewById(R.id.spinnerTipoLectura);
        TipoLecturaInstantanea[] tipos = TipoLecturaInstantanea.values();
        ArrayAdapter<TipoLecturaInstantanea> adapterTipos = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, tipos);
        spinnerTipoLectura.setAdapter(adapterTipos);
        spinnerTipoLectura.setText(tipos[0].toString(), false);

        // La lectura ya no se dispara automáticamente al abrir la pantalla:
        // el usuario elige el tipo y pulsa el botón de leer.
        ExtendedFloatingActionButton btnLeer = findViewById(R.id.btnLeer);

        btnLeer.setOnClickListener(v -> {
            TipoLecturaInstantanea tipoSeleccionado = tipos[0];
            for (TipoLecturaInstantanea t : tipos) {
                if (t.toString().equals(spinnerTipoLectura.getText().toString())) {
                    tipoSeleccionado = t;
                    break;
                }
            }
            TipoLecturaInstantanea tipoFinal = tipoSeleccionado;

            ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

            progressBar.setVisibility(View.VISIBLE);
            cardResultados.setVisibility(View.GONE);
            layoutResultados.setVisibility(View.GONE);
            btnLeer.setEnabled(false);

            new Thread(() -> {
                DLMSConnection conn = (config.getType() == ConnectionConfig.ConnectionType.BLUETOOTH)
                        ? new DLMSConnection(config.getBluetoothDeviceName())
                        : new DLMSConnection(config.getIp(), config.getPort());

                try {
                    DLMSConnection.ConnectionResult res = conn.connectWithAutoDetect(ValoresInstantaneosActivity.this);

                    if (tipoFinal == TipoLecturaInstantanea.VALORES_S28) {
                        // S28: sin cambios, se sigue mostrando en esta misma pantalla.
                        String datos = InstantaneousValuesReader.leerValores(res.reader);

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            cardSelector.setVisibility(View.GONE);
                            cardResultados.setVisibility(View.VISIBLE);
                            layoutResultados.setVisibility(View.VISIBLE);
                            btnLeer.setEnabled(true);
                            tvResultado.setText(datos);
                        });

                    } else {
                        // S29: histórico completo, se muestra en su propia pantalla
                        // como tabla (RecyclerView + adapter, igual que CurvaFila).
                        List<RegistroS29> registros = InstantaneousValuesReader.leerValoresS29(res.reader);
                        //Intent intent = new Intent(ValoresInstantaneosActivity.this, ResultadosValoresInstantaneosS29Activity.class);

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnLeer.setEnabled(true);

                            Intent intent = new Intent(ValoresInstantaneosActivity.this,
                                    ResultadosValoresInstantaneosS29Activity.class);
                            intent.putParcelableArrayListExtra(
                                    ResultadosValoresInstantaneosS29Activity.EXTRA_REGISTROS,
                                    new ArrayList<>(registros));
                            intent.putExtra("cntId", res.serialNumber);
                            startActivity(intent);
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnLeer.setEnabled(true);
                        new androidx.appcompat.app.AlertDialog.Builder(ValoresInstantaneosActivity.this)
                                .setTitle("Error de lectura")
                                .setMessage("No se pudieron leer los valores instantáneos del contador.\n\n"
                                        + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .setCancelable(true)
                                .show();
                    });
                } finally {
                    conn.close();
                }
            }).start();
        });
    }
}