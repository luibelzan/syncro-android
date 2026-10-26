package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.params.Iec870DeviceInfo;
import com.celnet.syncro.objects.iec870.params.Iec870InfoReader;
import com.celnet.syncro.models.iec870.params.Iec870PortConfig;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Locale;

/**
 * Configuración de puertos por IEC-870 (serie 1, serie 2 y óptico): casilla "modificar" por puerto,
 * velocidad y bits datos/paridad/stop (con "No Modificar"), y modo del puerto serie 1.
 * - "Leer": rellena el formulario y muestra el resto de la información (solo lectura).
 * - "Programar": escribe (ASDU 143) los puertos marcados; verificado con log solo para el serie 1.
 */
public class IecInfoActivity extends BaseActivity {

    /** Controles de un puerto. */
    private static class PuertoUi {
        CheckBox modificar;
        AutoCompleteTextView velocidad, params;
    }

    private PuertoUi puerto1, puerto2, optico;
    private EditText editModo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        puerto1 = crearPuerto(R.id.checkPort1, R.id.spinnerPort1Speed, R.id.spinnerPort1Params);
        puerto2 = crearPuerto(R.id.checkPort2, R.id.spinnerPort2Speed, R.id.spinnerPort2Params);
        optico = crearPuerto(R.id.checkOptical, R.id.spinnerOpticalSpeed, R.id.spinnerOpticalParams);
        editModo = findViewById(R.id.editPort1Mode);

        ExtendedFloatingActionButton btnRead = findViewById(R.id.btnRead);
        ExtendedFloatingActionButton btnNext = findViewById(R.id.btnNext);
        LinearLayout progressBar = findViewById(R.id.progressContainer);
        MaterialCardView cardInfo = findViewById(R.id.cardInfo);
        TextView tvInfo = findViewById(R.id.tvInfoEquipo);

        ConnectionConfig config = SessionManager.getInstance().getConnectionConfig();

        // ---------------- Leer ----------------
        btnRead.setOnClickListener(v -> {
            bloquear(true, btnRead, btnNext, progressBar);

            new Thread(() -> {
                try {
                    Iec870DeviceInfo info = Iec870InfoReader.leer(IecInfoActivity.this, config);

                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, progressBar);
                        mostrarPuerto(puerto1, info.puerto1);
                        mostrarPuerto(puerto2, info.puerto2);
                        mostrarPuerto(optico, info.puertoOptico);
                        editModo.setText(String.valueOf(info.puerto1.modo));
                        tvInfo.setText(textoInfo(info));
                        cardInfo.setVisibility(View.VISIBLE);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        bloquear(false, btnRead, btnNext, progressBar);
                        mostrarError("No se pudo leer la información del equipo.", e);
                    });
                }
            }).start();
        });

        // ---------------- Programar ----------------
        btnNext.setOnClickListener(v -> {
            Iec870PortConfig p1 = leerPuerto(puerto1);
            Iec870PortConfig p2 = leerPuerto(puerto2);
            Iec870PortConfig po = leerPuerto(optico);
            if (p1 == null && p2 == null && po == null) {
                Toast.makeText(this, "Marque al menos un puerto a programar",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Integer modo = null;
            if (p1 != null && !editModo.getText().toString().trim().isEmpty()) {
                try {
                    modo = Integer.parseInt(editModo.getText().toString().trim());
                    if (modo < 0 || modo > 255) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    editModo.setError("Debe estar entre 0 y 255");
                    editModo.requestFocus();
                    return;
                }
            }

            if (p1 != null) {
                p1.modo = modo != null ? modo : 0;
            }
            final Iec870PortConfig fp1 = p1, fp2 = p2, fpo = po;

            new AlertDialog.Builder(this)
                    .setTitle("Programar puertos")
                    .setMessage("Se programará:\n"
                            + "Serie 1: " + resumen(p1) + (p1 != null ? ", modo " + p1.modo : "") + "\n"
                            + "Serie 2: " + resumen(p2) + "\n"
                            + "Óptico: " + resumen(po) + "\n\n"
                            + "Recuerde tener activado el modo de escritura en el CN.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Programar", (d, w) -> {
                        bloquear(true, btnRead, btnNext, progressBar);
                        new Thread(() -> {
                            try {
                                Iec870InfoReader.programar(IecInfoActivity.this, config,
                                        fp1, fp2, fpo);
                                runOnUiThread(() -> {
                                    bloquear(false, btnRead, btnNext, progressBar);
                                    Toast.makeText(IecInfoActivity.this,
                                            "Configuración de puertos programada",
                                            Toast.LENGTH_LONG).show();
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    bloquear(false, btnRead, btnNext, progressBar);
                                    mostrarError("No se pudo programar la configuración de puertos.", e);
                                });
                            }
                        }).start();
                    })
                    .show();
        });
    }

    private static final String NO_MODIFICAR = "No Modificar";

    private PuertoUi crearPuerto(int idCheck, int idVel, int idParams) {
        PuertoUi p = new PuertoUi();
        p.modificar = findViewById(idCheck);
        p.velocidad = findViewById(idVel);
        p.params = findViewById(idParams);

        // Velocidad: "No Modificar" + 300..115200
        String[] bauds = new String[Iec870PortConfig.BAUDS.length + 1];
        bauds[0] = NO_MODIFICAR;
        for (int i = 0; i < Iec870PortConfig.BAUDS.length; i++) {
            bauds[i + 1] = String.valueOf(Iec870PortConfig.BAUDS[i]);
        }
        p.velocidad.setAdapter(adaptador(bauds));

        // Bits datos, paridad, stop: "No Modificar" + 7 No 1 ... 8 Impar 2 (índice = código 1..12)
        String[] params = new String[13];
        params[0] = NO_MODIFICAR;
        for (int c = 1; c <= 12; c++) {
            params[c] = new Iec870PortConfig(0, c).textoParams();
        }
        p.params.setAdapter(adaptador(params));

        p.velocidad.setText(NO_MODIFICAR, false);
        p.params.setText(NO_MODIFICAR, false);
        return p;
    }

    private ArrayAdapter<String> adaptador(String[] valores) {
        return new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, valores);
    }

    /** Vuelca en el formulario lo leído del contador. */
    private void mostrarPuerto(PuertoUi ui, Iec870PortConfig cfg) {
        if (cfg.baudios() > 0) ui.velocidad.setText(String.valueOf(cfg.baudios()), false);
        if (cfg.bitsDatos() > 0) ui.params.setText(cfg.textoParams(), false);
    }

    /**
     * Configuración a programar: null si el puerto no está marcado para modificar; con códigos 0 los
     * campos en "No Modificar".
     */
    private Iec870PortConfig leerPuerto(PuertoUi ui) {
        if (!ui.modificar.isChecked()) return null;
        int codVel = 0;
        try {
            codVel = Iec870PortConfig.codigoVelocidad(
                    Integer.parseInt(ui.velocidad.getText().toString().trim()));
        } catch (NumberFormatException ignored) {
            // "No Modificar"
        }
        int codParams = 0;
        String t = ui.params.getText().toString().trim();
        for (int c = 1; c <= 12; c++) {
            if (new Iec870PortConfig(0, c).textoParams().equals(t)) codParams = c;
        }
        return new Iec870PortConfig(codVel, codParams);
    }

    private String resumen(Iec870PortConfig c) {
        if (c == null) return "sin cambios";
        return (c.baudios() > 0 ? c.baudios() + " baudios" : "velocidad sin cambios") + ", "
                + (c.bitsDatos() > 0 ? c.textoParams() : "bits/paridad/stop sin cambios");
    }

    private String textoInfo(Iec870DeviceInfo i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nº de serie: ").append(i.serie).append('\n');
        sb.append("Código de fabricante: ").append(i.codigoFabricante).append('\n');
        sb.append("Modelo de registrador: ").append(i.modelo).append('\n');
        sb.append("Versión de firmware: ").append(i.firmware).append('\n');
        sb.append("Fecha de la versión del protocolo: ").append(i.fechaProtocolo).append('\n');
        sb.append("Fecha de versión del equipo: ").append(i.fechaVersion).append('\n');
        sb.append("Estado de carga de la batería: ").append(i.bateria).append('\n');
        if (!i.puerto1.cadena.isEmpty()) {
            sb.append("Cadena de inicialización del módem: ").append(i.puerto1.cadena).append('\n');
        }
        sb.append(String.format(Locale.getDefault(), "Relación de tensión: %d / %d dV\n",
                i.trafo.tensionPrimariaDv, i.trafo.tensionSecundariaDv));
        sb.append(String.format(Locale.getDefault(), "Relación de corriente: %d / %d dA\n",
                i.trafo.corrientePrimariaDa, i.trafo.corrienteSecundariaDa));
        for (int k = 0; k < 3; k++) {
            sb.append("Periodo de integración curva ").append(k + 1).append(": ")
                    .append(i.periodoIntegracion[k]).append(" min\n");
        }
        for (int k = 0; k < 3; k++) {
            sb.append("Contrato ").append(k + 1).append(": ")
                    .append(i.contratoActivo[k] ? "activo" : "inactivo");
            if (k < 2) sb.append('\n');
        }
        return sb.toString();
    }

    private void bloquear(boolean leyendo, ExtendedFloatingActionButton btnRead,
                          ExtendedFloatingActionButton btnNext, LinearLayout progress) {
        btnRead.setEnabled(!leyendo);
        btnNext.setEnabled(!leyendo);
        progress.setVisibility(leyendo ? View.VISIBLE : View.GONE);
    }

    private void mostrarError(String mensaje, Exception e) {
        new AlertDialog.Builder(this)
                .setTitle("Error (IEC-870)")
                .setMessage(mensaje + "\n\n" + e.getMessage())
                .setPositiveButton("Aceptar", null)
                .show();
    }
}