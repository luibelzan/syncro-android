package com.example.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.R;
import com.example.syncro.models.CurvaFila;

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
        holder.tvEnergiaActiva.setText(fila.energiaActiva);
        holder.tvEnergiaReactiva.setText(fila.energiaReactiva);
        holder.tvPotenciaActiva.setText(fila.potenciaActiva);
        holder.tvPotenciaReactiva.setText(fila.potenciaReactiva);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFechaHora, tvEnergiaActiva, tvEnergiaReactiva, tvPotenciaActiva, tvPotenciaReactiva;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvEnergiaActiva = itemView.findViewById(R.id.tvEnergiaActiva);
            tvEnergiaReactiva = itemView.findViewById(R.id.tvEnergiaReactiva);
            tvPotenciaActiva = itemView.findViewById(R.id.tvPotenciaActiva);
            tvPotenciaReactiva = itemView.findViewById(R.id.tvPotenciaReactiva);
        }
    }
}
