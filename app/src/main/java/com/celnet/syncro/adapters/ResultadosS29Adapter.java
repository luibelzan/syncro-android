package com.celnet.syncro.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.celnet.syncro.models.instantvalues.RegistroS29;

import java.util.List;

/**
 * Adapter de RecyclerView para las filas del buffer S29.
 * Cada ViewHolder crea sus TextView de columna una única vez
 * (en onCreateViewHolder) y onBindViewHolder solo actualiza el texto,
 * de forma que el reciclaje de vistas es real incluso con muchas columnas.
 */
public class ResultadosS29Adapter extends RecyclerView.Adapter<ResultadosS29Adapter.RowViewHolder> {

    private final List<RegistroS29> registros;
    private final int[] anchosColumnas;

    public ResultadosS29Adapter(List<RegistroS29> registros, int[] anchosColumnas) {
        this.registros = registros;
        this.anchosColumnas = anchosColumnas;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LinearLayout fila = new LinearLayout(context);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView[] celdas = new TextView[anchosColumnas.length];
        for (int i = 0; i < anchosColumnas.length; i++) {
            TextView tv = crearCeldaDato(context, anchosColumnas[i]);
            celdas[i] = tv;
            fila.addView(tv);
        }

        return new RowViewHolder(fila, celdas);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        String[] valores = registros.get(position).toRowValues();
        for (int i = 0; i < holder.celdas.length; i++) {
            String texto = (i < valores.length && valores[i] != null) ? valores[i] : "-";
            holder.celdas[i].setText(texto);
        }
    }

    @Override
    public int getItemCount() {
        return registros.size();
    }

    private static TextView crearCeldaDato(Context context, int anchoPx) {
        TextView tv = new TextView(context);
        tv.setLayoutParams(new LinearLayout.LayoutParams(anchoPx, LinearLayout.LayoutParams.WRAP_CONTENT));
        int padding = dpToPx(context, 10);
        tv.setPadding(padding, padding, padding, padding);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); // debe coincidir con TEXT_SIZE_DATA_SP
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setSingleLine(true);
        tv.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
        tv.setBackgroundResource(R.drawable.bg_celda_dato);
        return tv;
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        final TextView[] celdas;

        RowViewHolder(@NonNull LinearLayout itemView, TextView[] celdas) {
            super(itemView);
            this.celdas = celdas;
        }
    }
}