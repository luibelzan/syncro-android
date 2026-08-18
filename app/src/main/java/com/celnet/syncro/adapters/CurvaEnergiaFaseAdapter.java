package com.celnet.syncro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.curvas.CurvaEnergiaFaseFila;

import java.util.List;

public class CurvaEnergiaFaseAdapter extends RecyclerView.Adapter<CurvaEnergiaFaseAdapter.ViewHolder> {

    private final List<CurvaEnergiaFaseFila> datos;

    public CurvaEnergiaFaseAdapter(List<CurvaEnergiaFaseFila> datos) {
        this.datos = datos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curva_energia_fase, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CurvaEnergiaFaseFila f = datos.get(position);
        holder.tvFechaHora.setText(f.fechaHora);
        holder.tvEaPosR.setText(f.eaPosR); holder.tvEaNegR.setText(f.eaNegR);
        holder.tvQ1R.setText(f.q1R); holder.tvQ2R.setText(f.q2R); holder.tvQ3R.setText(f.q3R); holder.tvQ4R.setText(f.q4R);
        holder.tvEaPosS.setText(f.eaPosS); holder.tvEaNegS.setText(f.eaNegS);
        holder.tvQ1S.setText(f.q1S); holder.tvQ2S.setText(f.q2S); holder.tvQ3S.setText(f.q3S); holder.tvQ4S.setText(f.q4S);
        holder.tvEaPosT.setText(f.eaPosT); holder.tvEaNegT.setText(f.eaNegT);
        holder.tvQ1T.setText(f.q1T); holder.tvQ2T.setText(f.q2T); holder.tvQ3T.setText(f.q3T); holder.tvQ4T.setText(f.q4T);
        holder.tvStatus.setText(f.status);

        boolean esPar = position % 2 == 0;
        holder.itemView.setBackgroundResource(esPar ? R.drawable.bg_row_fila : R.drawable.bg_row_fila_alt);
    }

    @Override
    public int getItemCount() { return datos.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFechaHora, tvEaPosR, tvEaNegR, tvQ1R, tvQ2R, tvQ3R, tvQ4R,
                tvEaPosS, tvEaNegS, tvQ1S, tvQ2S, tvQ3S, tvQ4S,
                tvEaPosT, tvEaNegT, tvQ1T, tvQ2T, tvQ3T, tvQ4T, tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvEaPosR = itemView.findViewById(R.id.tvEaPosR); tvEaNegR = itemView.findViewById(R.id.tvEaNegR);
            tvQ1R = itemView.findViewById(R.id.tvQ1R); tvQ2R = itemView.findViewById(R.id.tvQ2R);
            tvQ3R = itemView.findViewById(R.id.tvQ3R); tvQ4R = itemView.findViewById(R.id.tvQ4R);
            tvEaPosS = itemView.findViewById(R.id.tvEaPosS); tvEaNegS = itemView.findViewById(R.id.tvEaNegS);
            tvQ1S = itemView.findViewById(R.id.tvQ1S); tvQ2S = itemView.findViewById(R.id.tvQ2S);
            tvQ3S = itemView.findViewById(R.id.tvQ3S); tvQ4S = itemView.findViewById(R.id.tvQ4S);
            tvEaPosT = itemView.findViewById(R.id.tvEaPosT); tvEaNegT = itemView.findViewById(R.id.tvEaNegT);
            tvQ1T = itemView.findViewById(R.id.tvQ1T); tvQ2T = itemView.findViewById(R.id.tvQ2T);
            tvQ3T = itemView.findViewById(R.id.tvQ3T); tvQ4T = itemView.findViewById(R.id.tvQ4T);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}