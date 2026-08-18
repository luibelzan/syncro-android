package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.EventFila;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private final List<EventFila> datos;

    // Formato real confirmado en el log de depuración: "1/7/26 13:24:21"
    // (día y mes sin cero a la izquierda, año en 2 dígitos, hora sin cero a la izquierda)
    private static final String FORMATO_ENTRADA = "d/M/yy H:mm:ss";

    private static final SimpleDateFormat FORMATO_SALIDA =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    public EventAdapter(List<EventFila> datos) {
        this.datos = datos;
    }

    private static String formatearFecha(String fh) {
        if (fh == null || fh.trim().isEmpty()) {
            return "-";
        }
        String texto = fh.trim();

        SimpleDateFormat sdf = new SimpleDateFormat(FORMATO_ENTRADA, Locale.getDefault());
        sdf.setLenient(false);

        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        Date fecha = sdf.parse(texto, pos);

        if (fecha != null && pos.getIndex() == texto.length() && pos.getErrorIndex() == -1) {
            return FORMATO_SALIDA.format(fecha);
        }

        // No coincide con el formato esperado: se muestra tal cual llegó
        // en vez de arriesgarse a mostrar una fecha incorrecta.
        return texto;
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

        holder.tvFh.setText(formatearFecha(fila.fh));
        holder.tvCod.setText(fila.cod != null ? String.valueOf(fila.cod) : "-");
        holder.tvId.setText(fila.id != null ? String.valueOf(fila.id) : "-");
        holder.tvDescription.setText(fila.description);

        // Filas alternas (zebra striping)
        boolean esPar = position % 2 == 0;
        holder.itemView.setBackgroundResource(
                esPar ? R.drawable.bg_row_fila : R.drawable.bg_row_fila_alt);
    }

    @Override
    public int getItemCount() {
        return datos != null ? datos.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFh, tvCod, tvId, tvDescription;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFh = itemView.findViewById(R.id.tvFh);
            tvCod = itemView.findViewById(R.id.tvCod);
            tvId = itemView.findViewById(R.id.tvId);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}