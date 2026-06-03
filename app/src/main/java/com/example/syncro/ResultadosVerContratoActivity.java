package com.example.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadosVerContratoActivity extends AppCompatActivity {

    private TextView txtResultadoContrato;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_ver_contrato);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtResultadoContrato = findViewById(R.id.txtResultadoContrato);

        // Obtener datos enviados desde VerContratoActivity
        String datosContrato = getIntent().getStringExtra("datos_contrato");

        if (datosContrato != null) {
            txtResultadoContrato.setText(datosContrato);
        }
    }
}