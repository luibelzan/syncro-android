package com.celnet.syncro;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.export.IecExcelExport;
import com.celnet.syncro.models.iec870.cierres.Iec870BillingRecord;
import com.celnet.syncro.export.Iec870CierresXlsx;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultados de tarificación (valores memorizados) leídos por IEC-870. Una tarjeta
 * por contrato y periodo de cierre, con una columna por tarifa (P0 = total).
 */
public class ResultadosIecCierresActivity extends BaseActivity {

    public static final String EXTRA_REGISTROS = "extra_iec_cierres";
    /** Título de la pantalla: "Cierres memorizados" / "Cierres en curso". */
    public static final String EXTRA_TITULO = "extra_iec_cierres_titulo";

    /** true = cierres en curso: sin Reservado 7/8. */
    public static final String EXTRA_EN_CURSO = "extra_iec_cierres_en_curso";

    private static final int ANCHO_ETIQUETA_DP = 150;
    private static final int ANCHO_CELDA_DP = 96;

    private LinearLayout contenedor;
    private boolean enCurso;

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
        enCurso = getIntent().getBooleanExtra(EXTRA_EN_CURSO, false);

        @SuppressWarnings({"unchecked", "deprecation"})
        ArrayList<Iec870BillingRecord> registros =
                (ArrayList<Iec870BillingRecord>) getIntent().getSerializableExtra(EXTRA_REGISTROS);
        String cntId = getIntent().getStringExtra("cntId");

        String tituloPantalla = getIntent().getStringExtra(EXTRA_TITULO);
        addTexto(tituloPantalla != null ? tituloPantalla : "Cierres memorizados", 26, true);
        if (cntId != null) {
            addTexto("Contador " + cntId, 14, false);
        }

        if (registros == null || registros.isEmpty()) {
            addTexto("No hay registros para mostrar.", 15, false);
            return;
        }

        // Exportar a Excel
        ExtendedFloatingActionButton btnExport = findViewById(R.id.btnExport);
        btnExport.setVisibility(android.view.View.VISIBLE);
        btnExport.setOnClickListener(v -> IecExcelExport.exportar(this, "Exportar cierres a Excel",
                cups -> nombreExcel(cups, registros),
                (out, cups) -> Iec870CierresXlsx.generar(out, cups, cntId != null ? cntId : "", registros)));

        // Agrupar por contrato + periodo (un cierre = un contrato y sus tarifas)
        Map<String, List<Iec870BillingRecord>> grupos = new LinkedHashMap<>();
        for (Iec870BillingRecord r : registros) {
            String clave = r.contrato + "|" + r.inicio + "|" + r.fin;
            List<Iec870BillingRecord> lista = grupos.get(clave);
            if (lista == null) {
                lista = new ArrayList<>();
                grupos.put(clave, lista);
            }
            lista.add(r);
        }
        for (List<Iec870BillingRecord> grupo : grupos.values()) {
            addCierre(grupo);
        }
    }

    /**
     * "CUPS_Cierres_Mes_Año.xlsx" si todos los cierres terminan en el mismo mes (el cierre de las
     * 00:00 del día 1 pertenece al mes anterior); "CUPS_Cierres_EnCurso_dd-MM-yyyy.xlsx" en curso.
     */
    private String nombreExcel(String cups, ArrayList<Iec870BillingRecord> registros) {
        String base = IecExcelExport.limpiar(cups) + "_Cierres_";
        if (enCurso) {
            String f = registros.get(registros.size() - 1).fin;
            return base + "EnCurso_" + f.substring(8, 10) + "-" + f.substring(5, 7) + "-"
                    + f.substring(0, 4) + ".xlsx";
        }
        int[] ini = IecExcelExport.fechaMenosUnMinuto(registros.get(0).fin);
        int[] fin = IecExcelExport.fechaMenosUnMinuto(registros.get(registros.size() - 1).fin);
        String d = String.format(java.util.Locale.US, "%04d/%02d/%02d 00:00", ini[0], ini[1], ini[2]);
        String h = String.format(java.util.Locale.US, "%04d/%02d/%02d 00:00", fin[0], fin[1], fin[2]);
        return base + IecExcelExport.periodo(d, h) + ".xlsx";
    }

    private void addCierre(List<Iec870BillingRecord> g) {
        Iec870BillingRecord primero = g.get(0);

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

        TextView titulo = new TextView(this);
        titulo.setText("Cierre contrato nº " + primero.contrato);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(ContextCompat.getColor(this, R.color.primary));
        cuerpo.addView(titulo);

        TextView periodo = new TextView(this);
        periodo.setText("De " + fecha(primero.inicio) + "\nhasta " + fecha(primero.fin));
        periodo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        periodo.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        periodo.setPadding(0, dp(4), 0, dp(8));
        cuerpo.addView(periodo);

        // Tabla: filas = magnitudes, columnas = tarifas (scroll horizontal)
        LinearLayout tabla = new LinearLayout(this);
        tabla.setOrientation(LinearLayout.VERTICAL);

        int n = g.size();
        String[] cab = new String[n + 1];
        cab[0] = "";
        for (int i = 0; i < n; i++) cab[i + 1] = "P" + g.get(i).tarifa;
        tabla.addView(crearFila(cab, true));

        tabla.addView(crearFila(fila("Activa abs. [kWh]", g, 0), false));
        tabla.addView(crearFila(fila("Activa inc. [kWh]", g, 1), false));
        tabla.addView(crearFila(fila("R. inductiva abs. [kVArh]", g, 2), false));
        tabla.addView(crearFila(fila("R. inductiva inc.", g, 3), false));
        tabla.addView(crearFila(fila("R. capacitiva abs. [kVArh]", g, 4), false));
        tabla.addView(crearFila(fila("R. capacitiva inc.", g, 5), false));
        if (!enCurso) {
            tabla.addView(crearFila(fila("Reservado 7", g, 6), false));
            tabla.addView(crearFila(fila("Reservado 8", g, 7), false));
        }
        tabla.addView(crearFila(fila("Maxímetro", g, 8), false));
        tabla.addView(crearFila(fila("Fecha maxímetro", g, 9), false));
        tabla.addView(crearFila(fila("Exceso de potencia", g, 10), false));
        tabla.addView(crearFila(fila("Calidad A/Ri/Rc", g, 11), false));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(true);
        scroll.addView(tabla);
        cuerpo.addView(scroll);

        contenedor.addView(card);
    }

    /** Valores de una magnitud para todas las tarifas del cierre. */
    private String[] fila(String etiqueta, List<Iec870BillingRecord> g, int campo) {
        String[] f = new String[g.size() + 1];
        f[0] = etiqueta;
        for (int i = 0; i < g.size(); i++) {
            Iec870BillingRecord r = g.get(i);
            String v;
            switch (campo) {
                case 0: v = String.valueOf(r.activaAbs); break;
                case 1: v = String.valueOf(r.activaInc); break;
                case 2: v = String.valueOf(r.reactivaIndAbs); break;
                case 3: v = String.valueOf(r.reactivaIndInc); break;
                case 4: v = String.valueOf(r.reactivaCapAbs); break;
                case 5: v = String.valueOf(r.reactivaCapInc); break;
                case 6: v = String.valueOf(r.reservado7); break;
                case 7: v = String.valueOf(r.reservado8); break;
                case 8: v = String.valueOf(r.maximetro); break;
                case 9: v = r.maximetro == 0 ? "-" : dosLineas(fecha(r.maximetroFecha)); break;
                case 10: v = String.valueOf(r.exceso); break;
                default:
                    v = String.format("%02X/%02X/%02X", r.activaCal, r.reactivaIndCal,
                            r.reactivaCapCal);
            }
            f[i + 1] = v;
        }
        return f;
    }

    /** "yyyy/MM/dd HH:mm..." -> "dd/MM/yyyy HH:mm...". */
    private String fecha(String texto) {
        if (texto == null) return texto;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(\\d{4})/(\\d{2})/(\\d{2})(.*)$").matcher(texto);
        return m.matches() ? m.group(3) + "/" + m.group(2) + "/" + m.group(1) + m.group(4) : texto;
    }

    private static String dosLineas(String fechaHora) {
        int e = fechaHora.indexOf(' ');
        return e < 0 ? fechaHora : fechaHora.substring(0, e) + "\n" + fechaHora.substring(e + 1);
    }

    private LinearLayout crearFila(String[] celdas, boolean cabecera) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(0, dp(cabecera ? 6 : 5), 0, dp(2));

        for (int i = 0; i < celdas.length; i++) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    dp(i == 0 ? ANCHO_ETIQUETA_DP : ANCHO_CELDA_DP),
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setText(celdas[i]);
            tv.setGravity(i == 0 ? Gravity.START : Gravity.END);
            tv.setPadding(0, 0, dp(i == 0 ? 8 : 4), 0);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, cabecera ? 12 : 13);
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
        lp.topMargin = dp(negrita ? 8 : 4);
        tv.setLayoutParams(lp);
        contenedor.addView(tv);
    }

    private int dp(int valor) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics()));
    }
}