package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.curvas.CurvaFila;

import java.util.List;

public class CurvaAdapter extends RecyclerView.Adapter<CurvaAdapter.ViewHolder> {

    private final List<CurvaFila> datos;

    public CurvaAdapter(List<CurvaFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curva, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CurvaFila fila = datos.get(position);
        holder.tvFechaHora.setText(fila.fechaHora);
        holder.tvBc.setText(fila.bc);
        holder.tvAi.setText(fila.ai);
        holder.tvAe.setText(fila.ae);
        holder.tvR1.setText(fila.r1);
        holder.tvR2.setText(fila.r2);
        holder.tvR3.setText(fila.r3);
        holder.tvR4.setText(fila.r4);

        // Filas alternas (zebra striping) para facilitar la lectura de la tabla
        boolean esPar = position % 2 == 0;
        holder.itemView.setBackgroundResource(
                esPar ? R.drawable.bg_row_fila : R.drawable.bg_row_fila_alt);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFechaHora, tvBc, tvAi, tvAe, tvR1, tvR2, tvR3, tvR4;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvBc = itemView.findViewById(R.id.tvBc);
            tvAi = itemView.findViewById(R.id.tvAi);
            tvAe = itemView.findViewById(R.id.tvAe);
            tvR1 = itemView.findViewById(R.id.tvR1);
            tvR2 = itemView.findViewById(R.id.tvR2);
            tvR3 = itemView.findViewById(R.id.tvR3);
            tvR4 = itemView.findViewById(R.id.tvR4);
        }
    }
}