package com.celnet.syncro;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ResultadosIcpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados_icp);

        ImageView ivEstadoIcp = findViewById(R.id.ivEstadoIcp);
        LinearLayout groupEstadoAnterior = findViewById(R.id.groupEstadoAnterior);
        TextView textEstadoInicial = findViewById(R.id.textEstadoInicial);
        TextView textEstadoFinal = findViewById(R.id.textEstadoFinal);
        TextView textModoControl = findViewById(R.id.textModoControl);
        LinearLayout layoutMensaje = findViewById(R.id.layoutMensaje);
        TextView textMensaje = findViewById(R.id.textMensaje);

        boolean success = getIntent().getBooleanExtra("success", false);
        String estadoInicial = getIntent().getStringExtra("estadoInicial");
        String estadoFinal = getIntent().getStringExtra("estadoFinal");
        String modoControl = getIntent().getStringExtra("modoControl");
        String mensaje = getIntent().getStringExtra("mensaje");

        textEstadoInicial.setText(estadoInicial != null ? estadoInicial : "-");
        textEstadoFinal.setText(estadoFinal != null ? estadoFinal : "-");
        textModoControl.setText(modoControl != null ? modoControl : "-");
        textMensaje.setText(mensaje != null ? mensaje : "-");

        // Si estadoInicial y estadoFinal coinciden, es una simple lectura de estado
        // (readControlDisconnectMode), no una acción de conectar/desconectar: no
        // tiene sentido mostrar "Estado anterior" porque no hubo transición real.
        boolean esSimpleLectura = estadoInicial != null && estadoInicial.equals(estadoFinal);
        groupEstadoAnterior.setVisibility(esSimpleLectura ? View.GONE : View.VISIBLE);

        // Icono según si el estado ACTUAL empieza por "Conectado" o "Cerrado"
        boolean conectado = estadoFinal != null &&
                (estadoFinal.startsWith("Conectado") || estadoFinal.startsWith("Cerrado"));
        ivEstadoIcp.setImageResource(
                conectado ? R.drawable.ic_meter_connected : R.drawable.ic_meter_disconnected);

        // Banner de mensaje: verde si la operación fue correcta, rojo si hubo error
        int colorTexto = ContextCompat.getColor(this, success ? R.color.success : R.color.error);
        textMensaje.setTextColor(colorTexto);
    }
}