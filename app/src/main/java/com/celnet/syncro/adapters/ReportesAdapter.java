package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtNombre;
        TextView txtInfo;
        MaterialCheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            checkBox = itemView.findViewById(R.id.checkReporte);
        }
    }
}