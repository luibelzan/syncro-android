package com.example.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.R;
import com.example.syncro.models.EventFila;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder>{

    private final List<EventFila> datos;

    public EventAdapter(List<EventFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        EventFila fila = datos.get(position);

        holder.tvFh.setText(fila.fh);
        holder.tvId.setText(String.valueOf(fila.id));
        holder.tvDescription.setText(fila.description);
        holder.tvCod.setText(String.valueOf(fila.cod));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFh, tvId, tvDescription, tvCod;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFh = itemView.findViewById(R.id.tvFh);
            tvId = itemView.findViewById(R.id.tvId);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCod = itemView.findViewById(R.id.tvCod);
        }
    }
}
