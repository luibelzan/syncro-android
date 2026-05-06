package com.example.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.R;
import com.example.syncro.models.CierreMensualFila;

import java.util.List;

public class CierreMensualAdapter extends RecyclerView.Adapter<CierreMensualAdapter.ViewHolder> {

    private final List<CierreMensualFila> datos;

    public CierreMensualAdapter(List<CierreMensualFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cierre_mensual, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        CierreMensualFila fila = datos.get(position);

        holder.tvFecha.setText(fila.fechaInicio);
        holder.tvContrato.setText(String.valueOf(fila.contrato));
        holder.tvPeriodo.setText(String.valueOf(fila.periodo));

        holder.tvMaxAIi.setText(fila.maxAIi);
        holder.tvFechaMaxAIi.setText(fila.fechaMaxAIi);

        holder.tvActImpAbs.setText(fila.activeImportAbs);
        holder.tvActExpAbs.setText(fila.activeExportAbs);

        holder.tvR1Abs.setText(fila.r1);
        holder.tvR2Abs.setText(fila.r2);
        holder.tvR3Abs.setText(fila.r3);
        holder.tvR4Abs.setText(fila.r4);

        holder.tvActImpInc.setText(fila.activeImportInc);
        holder.tvActExpInc.setText(fila.activeExportInc);

        holder.tvR1Inc.setText(fila.r1Inc);
        holder.tvR2Inc.setText(fila.r2Inc);
        holder.tvR3Inc.setText(fila.r3Inc);
        holder.tvR4Inc.setText(fila.r4Inc);
    }

    @Override
    public int getItemCount() {
        return datos != null ? datos.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvFecha, tvContrato, tvPeriodo,
                tvMaxAIi, tvFechaMaxAIi,
                tvActImpAbs, tvActExpAbs,
                tvR1Abs, tvR2Abs, tvR3Abs, tvR4Abs,
                tvActImpInc, tvActExpInc,
                tvR1Inc, tvR2Inc, tvR3Inc, tvR4Inc;

        public ViewHolder(View itemView) {
            super(itemView);

            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvContrato = itemView.findViewById(R.id.tvContrato);
            tvPeriodo = itemView.findViewById(R.id.tvPeriodo);

            tvMaxAIi = itemView.findViewById(R.id.tvMaxAIi);
            tvFechaMaxAIi = itemView.findViewById(R.id.tvFechaMaxAIi);

            tvActImpAbs = itemView.findViewById(R.id.tvActImpAbs);
            tvActExpAbs = itemView.findViewById(R.id.tvActExpAbs);

            tvR1Abs = itemView.findViewById(R.id.tvR1Abs);
            tvR2Abs = itemView.findViewById(R.id.tvR2Abs);
            tvR3Abs = itemView.findViewById(R.id.tvR3Abs);
            tvR4Abs = itemView.findViewById(R.id.tvR4Abs);

            tvActImpInc = itemView.findViewById(R.id.tvActImpInc);
            tvActExpInc = itemView.findViewById(R.id.tvActExpInc);

            tvR1Inc = itemView.findViewById(R.id.tvR1Inc);
            tvR2Inc = itemView.findViewById(R.id.tvR2Inc);
            tvR3Inc = itemView.findViewById(R.id.tvR3Inc);
            tvR4Inc = itemView.findViewById(R.id.tvR4Inc);
        }
    }
}