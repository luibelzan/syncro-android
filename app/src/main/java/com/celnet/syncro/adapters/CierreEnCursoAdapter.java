package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.cierres.CierreEnCursoFila;

import java.util.ArrayList;
import java.util.List;

public class CierreEnCursoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER  = 0;
    private static final int TYPE_LINE    = 1;
    private static final int TYPE_SECCION = 2;

    private static abstract class Row {
        abstract int type();
    }

    private static class RowTexto extends Row {
        final String text;
        final boolean isHeader;
        RowTexto(String text, boolean isHeader) {
            this.text = text;
            this.isHeader = isHeader;
        }
        @Override int type() { return isHeader ? TYPE_HEADER : TYPE_LINE; }
    }

    private static class RowSeccion extends Row {
        final String titulo;
        final long[] valores;      // longitud 7 (P0..P6)
        final String[] fechas;     // longitud 7, o null si esta sección no lleva fechas
        RowSeccion(String titulo, long[] valores, String[] fechas) {
            this.titulo = titulo;
            this.valores = valores;
            this.fechas = fechas;
        }
        @Override int type() { return TYPE_SECCION; }
    }

    private final List<Row> rows = new ArrayList<>();

    public CierreEnCursoAdapter(ArrayList<CierreEnCursoFila> datos) {
        for (CierreEnCursoFila f : datos) {

            // ── Cabecera de contrato ──────────────────────────────────────
            rows.add(new RowTexto("Cierre contrato nº " + f.contrato, true));
            rows.add(new RowTexto("Timestamp " + f.fecha, false));

            // ── Tablas P0..P6 ───────────────────────────────────────────
            rows.add(new RowSeccion("Activa Importada", f.aPlus, null));
            rows.add(new RowSeccion("Activa Exportada", f.aMinus, null));
            rows.add(new RowSeccion("Reactiva QI", f.qi, null));
            rows.add(new RowSeccion("Reactiva QII", f.qii, null));
            rows.add(new RowSeccion("Reactiva QIII", f.qiii, null));
            rows.add(new RowSeccion("Reactiva QIV", f.qiv, null));
            rows.add(new RowSeccion("Maxímetros", f.maxDemand, f.maxDates));

            // ── Separador visual entre bloques de contrato ────────────────
            rows.add(new RowTexto("", false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_SECCION) {
            View v = inflater.inflate(R.layout.item_cierre_encurso_seccion, parent, false);
            return new VHSeccion(v);
        }

        int layout = (viewType == TYPE_HEADER)
                ? R.layout.item_cierre_encurso_header
                : R.layout.item_cierre_encurso_line;
        View v = inflater.inflate(layout, parent, false);
        return new VHTexto(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (row instanceof RowSeccion) {
            RowSeccion seccion = (RowSeccion) row;
            VHSeccion h = (VHSeccion) holder;

            h.tvTitulo.setText(seccion.titulo);

            for (int i = 0; i < 7; i++) {
                h.valores[i].setText(String.valueOf(seccion.valores[i]));
            }

            if (seccion.fechas != null) {
                h.rowFechas.setVisibility(View.VISIBLE);
                for (int i = 0; i < 7; i++) {
                    h.fechas[i].setText(formatearFechaCelda(seccion.fechas[i]));
                }
            } else {
                h.rowFechas.setVisibility(View.GONE);
            }

        } else {
            ((VHTexto) holder).tv.setText(((RowTexto) row).text);
        }
    }

    /** Divide "fecha hora" en dos líneas para que quepa en la celda. */
    private String formatearFechaCelda(String fechaHora) {
        if (fechaHora == null || fechaHora.trim().isEmpty()) {
            return "-";
        }
        int espacio = fechaHora.indexOf(' ');
        if (espacio == -1) {
            return fechaHora;
        }
        return fechaHora.substring(0, espacio) + "\n" + fechaHora.substring(espacio + 1);
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class VHTexto extends RecyclerView.ViewHolder {
        final TextView tv;
        VHTexto(View v) {
            super(v);
            tv = v.findViewById(R.id.tvLine);
        }
    }

    static class VHSeccion extends RecyclerView.ViewHolder {
        final TextView tvTitulo;
        final TextView[] valores = new TextView[7];
        final TextView[] fechas = new TextView[7];
        final View rowFechas;

        VHSeccion(View v) {
            super(v);
            tvTitulo = v.findViewById(R.id.tvTituloSeccion);
            valores[0] = v.findViewById(R.id.tvVal0);
            valores[1] = v.findViewById(R.id.tvVal1);
            valores[2] = v.findViewById(R.id.tvVal2);
            valores[3] = v.findViewById(R.id.tvVal3);
            valores[4] = v.findViewById(R.id.tvVal4);
            valores[5] = v.findViewById(R.id.tvVal5);
            valores[6] = v.findViewById(R.id.tvVal6);

            rowFechas = v.findViewById(R.id.rowFechas);
            fechas[0] = v.findViewById(R.id.tvFecha0);
            fechas[1] = v.findViewById(R.id.tvFecha1);
            fechas[2] = v.findViewById(R.id.tvFecha2);
            fechas[3] = v.findViewById(R.id.tvFecha3);
            fechas[4] = v.findViewById(R.id.tvFecha4);
            fechas[5] = v.findViewById(R.id.tvFecha5);
            fechas[6] = v.findViewById(R.id.tvFecha6);
        }
    }
}