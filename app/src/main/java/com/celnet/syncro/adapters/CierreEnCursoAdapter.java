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

public class CierreEnCursoAdapter extends RecyclerView.Adapter<CierreEnCursoAdapter.VH> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_LINE   = 1;

    private static class Row {
        final String text;
        final boolean isHeader;
        Row(String text, boolean isHeader) {
            this.text     = text;
            this.isHeader = isHeader;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public CierreEnCursoAdapter(ArrayList<CierreEnCursoFila> datos) {
        for (CierreEnCursoFila f : datos) {

            // ── Cabecera de contrato ──────────────────────────────────────
            rows.add(new Row("Cierre contrato nº " + f.contrato, true));
            rows.add(new Row("Timestamp " + f.fecha, false));

            // ── Activa Importada ─────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Tarifa activa Importada " + (i + 1) + " = " + f.aPlus[i] + " [kWh]",
                        false));
            }
            rows.add(new Row(
                    "Tarifa activa Importada Total = " + f.aPlus[6] + " [kWh]", false));

            // ── Activa Exportada ─────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Tarifa activa Exportada " + (i + 1) + " = " + f.aMinus[i] + " [kWh]",
                        false));
            }
            rows.add(new Row(
                    "Tarifa activa Exportada Total = " + f.aMinus[6] + " [kWh]", false));

            // ── Reactiva QI ──────────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Tarifa reactiva QI " + (i + 1) + " = " + f.qi[i] + " [kvarh]",
                        false));
            }
            rows.add(new Row(
                    "Tarifa reactiva QI Total = " + f.qi[6] + " [kvarh]", false));

            // ── Reactiva QII ─────────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Tarifa reactiva QII " + (i + 1) + " = " + f.qii[i] + " [kvarh]",
                        false));
            }
            rows.add(new Row(
                    "Tarifa reactiva QII Total = " + f.qii[6] + " [kvarh]", false));

            // ── Reactiva QIII ────────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Tarifa reactiva QIII " + (i + 1) + " = " + f.qiii[i] + " [kvarh]",
                        false));
            }
            rows.add(new Row(
                    "Tarifa reactiva QIII Total = " + f.qiii[6] + " [kvarh]", false));

            // ── Reactiva QIV ─────────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Tarifa reactiva QIV " + (i + 1) + " = " + f.qiv[i] + " [kvarh]",
                        false));
            }
            rows.add(new Row(
                    "Tarifa reactiva QIV Total = " + f.qiv[6] + " [kvarh]", false));

            // ── Maxímetros ───────────────────────────────────────────────
            for (int i = 0; i < 6; i++) {
                rows.add(new Row(
                        "Max periodo " + (i + 1) + " = " + f.maxDemand[i] + " [W]",
                        false));
                rows.add(new Row(
                        "Fecha/hora max = " + f.maxDates[i],
                        false));
            }
            rows.add(new Row(
                    "Max Total = " + f.maxDemand[6] + " [W]", false));
            rows.add(new Row(
                    "Fecha/hora max = " + f.maxDates[6], false));

            // ── Separador visual entre bloques de contrato ────────────────
            rows.add(new Row("", false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_LINE;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_HEADER)
                ? R.layout.item_cierre_encurso_header
                : R.layout.item_cierre_encurso_line;
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        // El color, la tipografía y el tamaño ya están definidos en el XML
        // de cada tipo de item (header vs. line), no hace falta tocarlos aquí.
        h.tv.setText(rows.get(position).text);
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        VH(View v) {
            super(v);
            tv = v.findViewById(R.id.tvLine);
        }
    }
}