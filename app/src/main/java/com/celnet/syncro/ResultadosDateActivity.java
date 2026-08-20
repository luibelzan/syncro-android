package com.celnet.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultadosDateActivity extends AppCompatActivity {

    // Formato fijo que genera DateReader.readDate — ya no hace falta adivinar ni
    // probar varios candidatos, porque ahora se genera de forma controlada en origen.
    private static final String FORMATO_ENTRADA = "yyyy-MM-dd HH:mm:ss";

    private static final SimpleDateFormat FORMATO_SALIDA =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    private static final long UMBRAL_DIFERENCIA_SEGUNDOS = 180;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados_date);

        TextView tvFechaContador = findViewById(R.id.tvFechaContador);
        TextView tvFechaSistema = findViewById(R.id.tvFechaSistema);
        TextView tvDiferencia = findViewById(R.id.tvDiferencia);

        // Fecha del sistema (tablet), tomada en el momento de mostrar la pantalla
        Date fechaSistema = new Date();
        tvFechaSistema.setText(FORMATO_SALIDA.format(fechaSistema));

        // Fecha recibida del contador
        String fechaContadorRaw = getIntent().getStringExtra("date");

        if (fechaContadorRaw == null || fechaContadorRaw.trim().isEmpty()) {
            tvFechaContador.setText("No se pudo obtener la fecha");
            tvDiferencia.setText("-");
            return;
        }

        Date fechaContador = parsearFecha(fechaContadorRaw.trim());

        if (fechaContador == null) {
            // Ningún formato conocido coincidió: mostramos el texto tal cual,
            // sin arriesgarnos a calcular una diferencia sobre una fecha mal interpretada.
            tvFechaContador.setText(fechaContadorRaw);
            tvDiferencia.setText("Formato de fecha no reconocido");
            return;
        }

        tvFechaContador.setText(FORMATO_SALIDA.format(fechaContador));

        long diferenciaSegundos = Math.abs(
                (fechaSistema.getTime() - fechaContador.getTime()) / 1000);

        tvDiferencia.setText(diferenciaSegundos + " Segundos");

        boolean dentroDeTolerancia = diferenciaSegundos <= UMBRAL_DIFERENCIA_SEGUNDOS;
        tvDiferencia.setTextColor(getResources().getColor(
                dentroDeTolerancia ? R.color.success : R.color.error, getTheme()));
    }

    /**
     * Parsea con el único formato confirmado, exigiendo que consuma la cadena
     * completa (evita coincidencias parciales que den fechas sin sentido).
     * Devuelve null si no encaja.
     */
    private Date parsearFecha(String texto) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATO_ENTRADA, Locale.getDefault());
        sdf.setLenient(false);

        ParsePosition pos = new ParsePosition(0);
        Date fecha = sdf.parse(texto, pos);

        if (fecha != null && pos.getIndex() == texto.length() && pos.getErrorIndex() == -1) {
            return fecha;
        }
        return null;
    }
}