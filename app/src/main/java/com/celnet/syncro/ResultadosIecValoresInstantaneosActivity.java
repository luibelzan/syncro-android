package com.celnet.syncro;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.iec870.instantValues.Iec870InstantValues;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

/**
 * Resultados de los valores instantáneos leídos por IEC-870. Mismas secciones que
 * el informe del software funcional: tensión/corriente de secundario, relaciones
 * de transformación, valores de primario (calculados), potencias y energías.
 */
public class ResultadosIecValoresInstantaneosActivity extends BaseActivity {

    public static final String EXTRA_VALORES = "extra_iec_valores_instantaneos";

    private static final Locale ES = new Locale("es", "ES");
    private static final String INVALIDO = " *";

    private LinearLayout contenedor;
    private boolean hayInvalidos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_iec_valores_instantaneos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contenedor = findViewById(R.id.contenedor);

        @SuppressWarnings("deprecation")
        Iec870InstantValues v = (Iec870InstantValues) getIntent().getSerializableExtra(EXTRA_VALORES);
        if (v == null) {
            addTexto("No hay valores para mostrar.", 15, false);
            return;
        }

        addTexto("Valores instantáneos", 26, true);

        // --- Contador y fecha ---
        addTarjeta("Contador", null, new String[][]{
                {"Contador", v.serie},
                {"Timestamp", v.fechaHora}
        });

        // --- Tensión / corriente de secundario ---
        String[][] sec = new String[3][];
        for (int f = 0; f < 3; f++) {
            sec[f] = new String[]{
                    "Fase " + (f + 1),
                    dec1(v.tensionSecundariaV(f)) + marca(v.ivInvalido[f]),
                    dec1(v.corrienteSecundariaA(f)) + marca(v.ivInvalido[f])
            };
        }
        addTarjeta("Tensión / corriente de secundario",
                new String[]{"", "Tensión [V]", "Corriente [A]"}, sec);

        // --- Relaciones de transformación ---
        addTarjeta("Relación de transformación",
                new String[]{"", "Primario", "Secundario", "Relación"}, new String[][]{
                        {"Tensión [dV]", String.valueOf(v.tensionPrimariaDv),
                                String.valueOf(v.tensionSecundariaDv), dec3(v.relacionTension())},
                        {"Corriente [dA]", String.valueOf(v.corrientePrimariaDa),
                                String.valueOf(v.corrienteSecundariaDa), dec3(v.relacionCorriente())}
                });

        // --- Primario (calculado) ---
        String[][] prim = new String[3][];
        for (int f = 0; f < 3; f++) {
            prim[f] = new String[]{
                    "Fase " + (f + 1),
                    dec1(v.tensionPrimariaV(f)) + marca(v.ivInvalido[f]),
                    dec1(v.corrientePrimariaA(f)) + marca(v.ivInvalido[f])
            };
        }
        addTarjeta("Primario (calculado)",
                new String[]{"", "Tensión [V]", "Corriente [A]"}, prim);

        // --- Potencias ---
        String[] nombres = {"Total", "Fase 1", "Fase 2", "Fase 3"};
        String[][] pot = new String[4][];
        for (int k = 0; k < 4; k++) {
            String m = marca(v.potenciaInvalida[k]);
            pot[k] = new String[]{
                    nombres[k],
                    v.potenciaActivaConSigno(k) + m,
                    v.potenciaReactivaConSigno(k) + m,
                    dec3(v.factorPotencia(k))
            };
        }
        addTarjeta("Potencias instantáneas",
                new String[]{"", "Activa [kW]", "Reactiva [kVAr]", "FP"}, pot);

        // --- Energías ---
        String[] etiquetas = {"Activa importada", "Activa exportada",
                "Reactiva Q1", "Reactiva Q2", "Reactiva Q3", "Reactiva Q4"};
        String[][] ener = new String[6][];
        for (int k = 0; k < 6; k++) {
            ener[k] = new String[]{
                    etiquetas[k],
                    v.energia[k] + marca(v.energiaInvalida[k]) + (k < 2 ? " kWh" : " kVArh")
            };
        }
        addTarjeta("Energías", null, ener);

        if (hayInvalidos) {
            addTexto("* Valor marcado como no válido por el contador.", 12, false);
        }
    }

    // ------------------------------------------------------------------
    // Construcción de la pantalla
    // ------------------------------------------------------------------

    /**
     * Tarjeta con título y una tabla. Primera columna alineada a la izquierda y el
     * resto a la derecha (monoespaciado). {@code cabeceras} puede ser null.
     */
    private void addTarjeta(String titulo, String[] cabeceras, String[][] filas) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        card.setLayoutParams(lp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.outline));
        card.setStrokeWidth(dp(1));

        LinearLayout cuerpo = new LinearLayout(this);
        cuerpo.setOrientation(LinearLayout.VERTICAL);
        cuerpo.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(cuerpo);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(titulo);
        tvTitulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitulo.setTextColor(ContextCompat.getColor(this, R.color.primary));
        cuerpo.addView(tvTitulo);

        if (cabeceras != null) {
            cuerpo.addView(crearFila(cabeceras, true));
        }
        for (String[] fila : filas) {
            cuerpo.addView(crearFila(fila, false));
        }
        contenedor.addView(card);
    }

    private LinearLayout crearFila(String[] celdas, boolean cabecera) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(0, dp(cabecera ? 10 : 6), 0, dp(2));

        for (int i = 0; i < celdas.length; i++) {
            TextView tv = new TextView(this);
            // La primera columna (etiqueta) es algo más ancha que las de datos
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, i == 0 ? 1.3f : 1f));
            tv.setText(celdas[i]);
            tv.setGravity(i == 0 ? Gravity.START : Gravity.END);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, cabecera ? 12 : 14);
            if (i == 0 && !cabecera) {
                tv.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            } else {
                tv.setTextColor(ContextCompat.getColor(this,
                        cabecera ? R.color.on_surface_variant : R.color.on_surface));
                tv.setTypeface(Typeface.MONOSPACE, cabecera ? Typeface.BOLD : Typeface.NORMAL);
            }
            fila.addView(tv);
        }
        return fila;
    }

    private void addTexto(String texto, int sp, boolean negrita) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(ContextCompat.getColor(this,
                negrita ? R.color.on_surface : R.color.on_surface_variant));
        if (negrita) tv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(negrita ? 8 : 12);
        tv.setLayoutParams(lp);
        contenedor.addView(tv);
    }

    // ------------------------------------------------------------------
    // Formato (coma decimal, como el software funcional)
    // ------------------------------------------------------------------

    private String marca(boolean invalido) {
        if (invalido) hayInvalidos = true;
        return invalido ? INVALIDO : "";
    }

    private static String dec1(double v) {
        return String.format(ES, "%.1f", v);
    }

    private static String dec3(double v) {
        return String.format(ES, "%.3f", v);
    }

    private int dp(int valor) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics()));
    }
}