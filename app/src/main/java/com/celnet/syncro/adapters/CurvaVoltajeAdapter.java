package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.curvas.CurvaVoltajeFila;

import java.util.List;

public class CurvaVoltajeAdapter extends RecyclerView.Adapter<CurvaVoltajeAdapter.ViewHolder> {

    private final List<CurvaVoltajeFila> datos;

    public CurvaVoltajeAdapter(List<CurvaVoltajeFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curva_voltaje, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CurvaVoltajeFila fila = datos.get(position);
        holder.tvFechaHora.setText(fila.fechaHora);
        holder.tvMaxL1.setText(fila.maxL1);
        holder.tvMaxL2.setText(fila.maxL2);
        holder.tvMaxL3.setText(fila.maxL3);
        holder.tvAvL1.setText(fila.avL1);
        holder.tvAvL2.setText(fila.avL2);
        holder.tvAvL3.setText(fila.avL3);
        holder.tvMinL1.setText(fila.minL1);
        holder.tvMinL2.setText(fila.minL2);
        holder.tvMinL3.setText(fila.minL3);
        holder.tvStatus.setText(fila.status);

        boolean esPar = position % 2 == 0;
        holder.itemView.setBackgroundResource(
                esPar ? R.drawable.bg_row_fila : R.drawable.bg_row_fila_alt);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFechaHora, tvMaxL1, tvMaxL2, tvMaxL3, tvAvL1, tvAvL2, tvAvL3, tvMinL1, tvMinL2, tvMinL3, tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvMaxL1 = itemView.findViewById(R.id.tvMaxL1);
            tvMaxL2 = itemView.findViewById(R.id.tvMaxL2);
            tvMaxL3 = itemView.findViewById(R.id.tvMaxL3);
            tvAvL1 = itemView.findViewById(R.id.tvAvL1);
            tvAvL2 = itemView.findViewById(R.id.tvAvL2);
            tvAvL3 = itemView.findViewById(R.id.tvAvL3);
            tvMinL1 = itemView.findViewById(R.id.tvMinL1);
            tvMinL2 = itemView.findViewById(R.id.tvMinL2);
            tvMinL3 = itemView.findViewById(R.id.tvMinL3);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}