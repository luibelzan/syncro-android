package com.celnet.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadosDateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados_date);

        TextView txtFecha = findViewById(R.id.txtFecha);

        // Recibir el dato
        String fecha = getIntent().getStringExtra("date");

        if (fecha != null) {
            txtFecha.setText(fecha);
        } else {
            txtFecha.setText("No se pudo obtener la fecha");
        }
    }
}