package com.example.syncro;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.CierreMensualAdapter;
import com.example.syncro.models.CierreMensualFila;

import java.util.ArrayList;

public class ResultadosCierresMensualesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_cierres_mensuales);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultadosCierresMensuales);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<CierreMensualFila> datos = getIntent().getParcelableArrayListExtra("datos_cierres_tabla");
        String cntId = getIntent().getStringExtra("cntId");
        LinearLayout btnExport = findViewById(R.id.btnExport);

        if(datos != null) {
            CierreMensualAdapter adapter = new CierreMensualAdapter(datos);
            rv.setAdapter(adapter);
        }
    }
}