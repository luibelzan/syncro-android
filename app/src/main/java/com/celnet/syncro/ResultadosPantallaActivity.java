package com.celnet.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadosPantallaActivity extends AppCompatActivity {

    public static final String EXTRA_TIEMPO = "extra_tiempo";
    public static final String EXTRA_MODO   = "extra_modo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_pantalla);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvTiempo = findViewById(R.id.tvTiempoResult);
        TextView tvModo   = findViewById(R.id.tvModoResult);

        int tiempo  = getIntent().getIntExtra(EXTRA_TIEMPO, -1);
        String modo = getIntent().getStringExtra(EXTRA_MODO);

        tvTiempo.setText("Tiempo desplazamiento: " + (tiempo >= 0 ? tiempo + " s" : "N/A"));
        tvModo.setText("Modo desplazamiento: "     + (modo != null ? modo : "N/A"));
    }
}