package com.celnet.syncro.adapters.iec870;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.iec870.eventos.Iec870EventFila;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lista de eventos IEC-870: fecha, SPA, SPQ, SPI y descripción. */
public class IecEventAdapter extends RecyclerView.Adapter<IecEventAdapter.ViewHolder> {

    private static final String FORMATO_ENTRADA = "d/M/yy H:mm:ss";
    private static final SimpleDateFormat FORMATO_SALIDA =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    private final List<Iec870EventFila> datos;

    public IecEventAdapter(List<Iec870EventFila> datos) {
        this.datos = datos;
    }

    private static String formatearFecha(String fh) {
        if (fh == null || fh.trim().isEmpty()) return "-";
        String texto = fh.trim();
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATO_ENTRADA, Locale.getDefault());
        sdf.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        Date fecha = sdf.parse(texto, pos);
        if (fecha != null && pos.getIndex() == texto.length() && pos.getErrorIndex() == -1) {
            return FORMATO_SALIDA.format(fecha);
        }
        return texto;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_iec_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        Iec870EventFila f = datos.get(position);
        h.tvFh.setText(formatearFecha(f.fh));
        h.tvSpa.setText(String.valueOf(f.spa));
        h.tvSpq.setText(String.valueOf(f.spq));
        h.tvSpi.setText(String.valueOf(f.spi));
        h.tvDescription.setText(f.descripcion);
        h.itemView.setBackgroundResource(position % 2 == 0
                ? R.drawable.bg_row_fila : R.drawable.bg_row_fila_alt);
    }

    @Override
    public int getItemCount() {
        return datos != null ? datos.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFh, tvSpa, tvSpq, tvSpi, tvDescription;

        ViewHolder(View v) {
            super(v);
            tvFh = v.findViewById(R.id.tvFh);
            tvSpa = v.findViewById(R.id.tvSpa);
            tvSpq = v.findViewById(R.id.tvSpq);
            tvSpi = v.findViewById(R.id.tvSpi);
            tvDescription = v.findViewById(R.id.tvDescription);
        }
    }
}