package com.example.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadosIcpActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados_icp);

        TextView textResultado = findViewById(R.id.textResultado);
        TextView textEstadoInicial = findViewById(R.id.textEstadoInicial);
        TextView textEstadoFinal = findViewById(R.id.textEstadoFinal);
        TextView textMensaje = findViewById(R.id.textMensaje);

        Intent intent = getIntent();

        boolean success = intent.getBooleanExtra("success", false);
        String estadoInicial = intent.getStringExtra("estadoInicial");
        String estadoFinal = intent.getStringExtra("estadoFinal");
        String mensaje = intent.getStringExtra("mensaje");

        textResultado.setText(success ? "Éxito" : "Error");
        textEstadoInicial.setText("Estado inicial: " + estadoInicial);
        textEstadoFinal.setText("Estado final: " + estadoFinal);
        textMensaje.setText(mensaje);
    }
}