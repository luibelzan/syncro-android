package com.celnet.syncro.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
                report.getOrigen() + " · " +
                        (report.getFile().length() / 1024) + " KB · " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(new Date(report.getFile().lastModified()));

        holder.txtInfo.setText(info);

        // Evitar que el listener se dispare al reciclar la vista con setChecked
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(report.isSeleccionado());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                report.setSeleccionado(isChecked));

        // Estado del envío: progreso, éxito o error con su motivo
        holder.progreso.setVisibility(View.GONE);
        holder.txtEstado.setVisibility(View.GONE);
        switch (report.getEstado()) {
            case SUBIENDO:
                holder.progreso.setVisibility(View.VISIBLE);
                holder.txtEstado.setVisibility(View.VISIBLE);
                holder.txtEstado.setTextColor(Color.parseColor("#1565C0"));
                holder.txtEstado.setText("Enviando…");
                break;
            case EXITO:
                holder.txtEstado.setVisibility(View.VISIBLE);
                holder.txtEstado.setTextColor(Color.parseColor("#2E7D32"));
                holder.txtEstado.setText("✓ Enviado correctamente");
                break;
            case ERROR:
                holder.txtEstado.setVisibility(View.VISIBLE);
                holder.txtEstado.setTextColor(Color.parseColor("#C62828"));
                String motivo = report.getMensajeError();
                holder.txtEstado.setText(motivo != null && !motivo.isEmpty()
                        ? "✗ Error: " + motivo : "✗ Error al enviar");
                break;
            default:
                break;
        }

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
        TextView txtEstado;
        ProgressBar progreso;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            checkBox = itemView.findViewById(R.id.checkReporte);
            txtEstado = itemView.findViewById(R.id.txtEstado);
            progreso = itemView.findViewById(R.id.progresoEnvio);
        }
    }
}