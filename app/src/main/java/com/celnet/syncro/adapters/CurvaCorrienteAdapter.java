package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.curvas.CurvaCorrienteFila;
import com.celnet.syncro.utils.NumeroFormatUtils;

import java.util.List;

public class CurvaCorrienteAdapter extends RecyclerView.Adapter<CurvaCorrienteAdapter.ViewHolder> {

    private final List<CurvaCorrienteFila> datos;

    public CurvaCorrienteAdapter(List<CurvaCorrienteFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curva_corriente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CurvaCorrienteFila fila = datos.get(position);
        holder.tvFechaHora.setText(fila.fechaHora);
        holder.tvMaxL1.setText(NumeroFormatUtils.unDecimal(fila.maxL1));
        holder.tvMaxL2.setText(NumeroFormatUtils.unDecimal(fila.maxL2));
        holder.tvMaxL3.setText(NumeroFormatUtils.unDecimal(fila.maxL3));
        holder.tvMaxN.setText(NumeroFormatUtils.unDecimal(fila.maxN));
        holder.tvAvL1.setText(NumeroFormatUtils.unDecimal(fila.avL1));
        holder.tvAvL2.setText(NumeroFormatUtils.unDecimal(fila.avL2));
        holder.tvAvL3.setText(NumeroFormatUtils.unDecimal(fila.avL3));
        holder.tvAvN.setText(NumeroFormatUtils.unDecimal(fila.avN));
        holder.tvMinL1.setText(NumeroFormatUtils.unDecimal(fila.minL1));
        holder.tvMinL2.setText(NumeroFormatUtils.unDecimal(fila.minL2));
        holder.tvMinL3.setText(NumeroFormatUtils.unDecimal(fila.minL3));
        holder.tvMinN.setText(NumeroFormatUtils.unDecimal(fila.minN));
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
        TextView tvFechaHora, tvMaxL1, tvMaxL2, tvMaxL3, tvMaxN,
                tvAvL1, tvAvL2, tvAvL3, tvAvN, tvMinL1, tvMinL2, tvMinL3, tvMinN, tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvMaxL1 = itemView.findViewById(R.id.tvMaxL1);
            tvMaxL2 = itemView.findViewById(R.id.tvMaxL2);
            tvMaxL3 = itemView.findViewById(R.id.tvMaxL3);
            tvMaxN = itemView.findViewById(R.id.tvMaxN);
            tvAvL1 = itemView.findViewById(R.id.tvAvL1);
            tvAvL2 = itemView.findViewById(R.id.tvAvL2);
            tvAvL3 = itemView.findViewById(R.id.tvAvL3);
            tvAvN = itemView.findViewById(R.id.tvAvN);
            tvMinL1 = itemView.findViewById(R.id.tvMinL1);
            tvMinL2 = itemView.findViewById(R.id.tvMinL2);
            tvMinL3 = itemView.findViewById(R.id.tvMinL3);
            tvMinN = itemView.findViewById(R.id.tvMinN);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}