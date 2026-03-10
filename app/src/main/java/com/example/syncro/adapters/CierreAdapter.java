package com.example.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.R;
import com.example.syncro.models.CierreFila;

import java.util.List;

public class CierreAdapter extends RecyclerView.Adapter<CierreAdapter.ViewHolder>{

    private final List<CierreFila> datos;

    public CierreAdapter(List<CierreFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cierre, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        CierreFila fila = datos.get(position);

        holder.tvFecha.setText(fila.fecha);
        holder.tvPeriodo.setText(String.valueOf(fila.periodo));
        holder.tvActivaImport.setText(fila.activaImport);
        holder.tvActivaExport.setText(fila.activaExport);
        holder.tvR1.setText(fila.r1);
        holder.tvR2.setText(fila.r2);
        holder.tvR3.setText(fila.r3);
        holder.tvR4.setText(fila.r4);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvFecha, tvPeriodo, tvActivaImport, tvActivaExport, tvR1, tvR2, tvR3, tvR4;

        public ViewHolder(View itemView) {
            super(itemView);

            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvPeriodo = itemView.findViewById(R.id.tvPeriodo);
            tvActivaImport = itemView.findViewById(R.id.tvActivaImport);
            tvActivaExport = itemView.findViewById(R.id.tvActivaExport);
            tvR1 = itemView.findViewById(R.id.tvR1);
            tvR2 = itemView.findViewById(R.id.tvR2);
            tvR3 = itemView.findViewById(R.id.tvR3);
            tvR4 = itemView.findViewById(R.id.tvR4);
        }
    }

}
