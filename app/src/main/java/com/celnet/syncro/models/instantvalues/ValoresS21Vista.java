package com.celnet.syncro.models.instantvalues;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.celnet.syncro.R;
import com.celnet.syncro.models.instantvalues.ValoresS21;
import com.google.android.material.card.MaterialCardView;

/**
 * Pinta los valores instantáneos S21 con el mismo aspecto que el resto de resultados de la
 * aplicación: una tarjeta por sección (título en color primario, tablas monoespaciadas).
 */
public final class ValoresS21Vista {

    private static final int ANCHO_ETIQUETA_DP = 84;
    private static final int ANCHO_CELDA_DP = 92;

    private final Activity act;
    private final LinearLayout cont;

    private ValoresS21Vista(Activity act, LinearLayout cont) {
        this.act = act;
        this.cont = cont;
    }

    /** Muestra los valores; si no se pudieron separar (v == null) muestra el texto tal cual. */
    public static void mostrar(Activity act, LinearLayout cont, ValoresS21 v, String textoOriginal,
                               String serie) {
        ValoresS21Vista p = new ValoresS21Vista(act, cont);
        cont.removeAllViews();

        if (v == null) {
            p.tarjetaTexto("Valores instantáneos", textoOriginal);
            return;
        }

        p.tarjeta("Contador", null, new String[][]{
                {"Contador", serie != null ? serie : "-"},
                {"Timestamp", v.timestamp}
        }, false);

        String[][] iv = new String[4][];
        for (int f = 0; f < 3; f++) {
            iv[f] = new String[]{"Fase " + (f + 1), v.fases[f][0], v.fases[f][1], v.fases[f][2]};
        }
        iv[3] = new String[]{"FP total", "", "", v.fpTotal};
        p.tarjeta("Tensión / corriente / FP",
                new String[]{"", "Tensión [V]", "Corriente [A]", "FP"}, iv, false);

        p.tarjeta("Relación de transformación",
                new String[]{"", "Primario", "Secundario", "Relación"}, new String[][]{
                        {"Tensión [dV]", v.relacionTension[0], v.relacionTension[1], v.relacionTension[2]},
                        {"Corriente [dA]", v.relacionCorriente[0], v.relacionCorriente[1], v.relacionCorriente[2]}
                }, false);

        String[] nombres = {"Fase 1", "Fase 2", "Fase 3", "Total"};
        String[][] pot = new String[4][];
        for (int k = 0; k < 4; k++) {
            pot[k] = new String[]{nombres[k], v.potencias[k][0], v.potencias[k][1],
                    v.potencias[k][2], v.potencias[k][3]};
        }
        p.tarjeta("Potencias instantáneas",
                new String[]{"", "P+ [kW]", "P- [kW]", "Q+ [kvar]", "Q- [kvar]"}, pot, true);

        String[] etiquetas = {"Activa importada", "Activa exportada",
                "Reactiva Q1", "Reactiva Q2", "Reactiva Q3", "Reactiva Q4"};
        String[][] ener = new String[6][];
        for (int k = 0; k < 6; k++) {
            ener[k] = new String[]{etiquetas[k], v.energias[k] + (k < 2 ? " kWh" : " kvarh")};
        }
        p.tarjeta("Energías", null, ener, false);
    }

    // ------------------------------------------------------------------

    private MaterialCardView nuevaTarjeta(String titulo, LinearLayout[] cuerpoOut) {
        MaterialCardView card = new MaterialCardView(act);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        card.setLayoutParams(lp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(ContextCompat.getColor(act, R.color.surface));
        card.setStrokeColor(ContextCompat.getColor(act, R.color.outline));
        card.setStrokeWidth(dp(1));

        LinearLayout cuerpo = new LinearLayout(act);
        cuerpo.setOrientation(LinearLayout.VERTICAL);
        cuerpo.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(cuerpo);

        TextView tv = new TextView(act);
        tv.setText(titulo);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(act, R.color.primary));
        cuerpo.addView(tv);

        cuerpoOut[0] = cuerpo;
        return card;
    }

    /** Tarjeta con tabla. {@code desplazable}: celdas de ancho fijo con scroll horizontal. */
    private void tarjeta(String titulo, String[] cabeceras, String[][] filas, boolean desplazable) {
        LinearLayout[] c = new LinearLayout[1];
        MaterialCardView card = nuevaTarjeta(titulo, c);

        LinearLayout tabla = c[0];
        if (desplazable) {
            tabla = new LinearLayout(act);
            tabla.setOrientation(LinearLayout.VERTICAL);
        }
        if (cabeceras != null) tabla.addView(crearFila(cabeceras, true, desplazable));
        for (String[] f : filas) tabla.addView(crearFila(f, false, desplazable));

        if (desplazable) {
            HorizontalScrollView sv = new HorizontalScrollView(act);
            sv.setHorizontalScrollBarEnabled(true);
            sv.addView(tabla);
            c[0].addView(sv);
        }
        cont.addView(card);
    }

    private void tarjetaTexto(String titulo, String texto) {
        LinearLayout[] c = new LinearLayout[1];
        MaterialCardView card = nuevaTarjeta(titulo, c);
        HorizontalScrollView sv = new HorizontalScrollView(act);
        TextView tv = new TextView(act);
        tv.setText(texto);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(ContextCompat.getColor(act, R.color.on_surface));
        tv.setPadding(0, dp(8), 0, 0);
        sv.addView(tv);
        c[0].addView(sv);
        cont.addView(card);
    }

    private LinearLayout crearFila(String[] celdas, boolean cabecera, boolean anchoFijo) {
        LinearLayout fila = new LinearLayout(act);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(0, dp(cabecera ? 10 : 6), 0, dp(2));

        for (int i = 0; i < celdas.length; i++) {
            TextView tv = new TextView(act);
            LinearLayout.LayoutParams lp = anchoFijo
                    ? new LinearLayout.LayoutParams(dp(i == 0 ? ANCHO_ETIQUETA_DP : ANCHO_CELDA_DP),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                    i == 0 ? 1.3f : 1f);
            tv.setLayoutParams(lp);
            tv.setText(celdas[i]);
            tv.setGravity(i == 0 ? Gravity.START : Gravity.END);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, cabecera ? 12 : 14);
            if (anchoFijo) tv.setPadding(0, 0, dp(i == 0 ? 8 : 4), 0);
            if (i == 0 && !cabecera) {
                tv.setTextColor(ContextCompat.getColor(act, R.color.on_surface_variant));
            } else {
                tv.setTextColor(ContextCompat.getColor(act,
                        cabecera ? R.color.on_surface_variant : R.color.on_surface));
                tv.setTypeface(Typeface.MONOSPACE, cabecera ? Typeface.BOLD : Typeface.NORMAL);
            }
            fila.addView(tv);
        }
        return fila;
    }

    private int dp(int valor) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                act.getResources().getDisplayMetrics()));
    }
}