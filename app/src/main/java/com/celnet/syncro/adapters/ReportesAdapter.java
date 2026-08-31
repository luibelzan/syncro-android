package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.ReportFile;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

    private ArrayList<ReportFile> lista;

    public ReportesAdapter(ArrayList<ReportFile> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ReportFile report = lista.get(position);

        holder.txtNombre.setText(report.getFile().getName());

        String info =
                (report.getFile().length() / 1024) + " KB · " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(new Date(report.getFile().lastModified()));

        holder.txtInfo.setText(info);

        // Evitar que el listener se dispare al reciclar la vista con setChecked
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(report.isSeleccionado());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                report.setSeleccionado(isChecked));

        // Filas alternas (zebra striping)
        boolean esPar = position % 2 == 0;
        holder.itemView.setBackgroundResource(
                esPar ? R.drawable.bg_row_fila : R.drawable.bg_row_fila_alt);

        pintarEstado(holder, report);
    }

    private void pintarEstado(ViewHolder holder, ReportFile report) {
        holder.progressEstado.setVisibility(View.GONE);
        holder.ivEstado.setVisibility(View.GONE);
        holder.txtEstado.setVisibility(View.GONE);

        switch (report.getEstado()) {
            case SUBIENDO:
                holder.progressEstado.setVisibility(View.VISIBLE);
                holder.txtEstado.setVisibility(View.VISIBLE);
                holder.txtEstado.setText("Enviando...");
                holder.txtEstado.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface_variant));
                break;

            case EXITO:
                holder.ivEstado.setVisibility(View.VISIBLE);
                holder.ivEstado.setImageResource(android.R.drawable.checkbox_on_background);
                holder.ivEstado.setColorFilter(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
                holder.txtEstado.setVisibility(View.VISIBLE);
                holder.txtEstado.setText("Enviado correctamente");
                holder.txtEstado.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
                break;

            case ERROR:
                holder.ivEstado.setVisibility(View.VISIBLE);
                holder.ivEstado.setImageResource(android.R.drawable.ic_delete);
                holder.ivEstado.setColorFilter(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
                holder.txtEstado.setVisibility(View.VISIBLE);
                String motivo = report.getMensajeError();
                holder.txtEstado.setText(motivo != null ? "Error: " + motivo : "Error al enviar");
                holder.txtEstado.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
                break;

            case PENDIENTE:
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtNombre;
        TextView txtInfo;
        TextView txtEstado;
        MaterialCheckBox checkBox;
        ProgressBar progressEstado;
        ImageView ivEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            txtEstado = itemView.findViewById(R.id.txtEstado);
            checkBox = itemView.findViewById(R.id.checkReporte);
            progressEstado = itemView.findViewById(R.id.progressEstado);
            ivEstado = itemView.findViewById(R.id.ivEstado);
        }
    }
}