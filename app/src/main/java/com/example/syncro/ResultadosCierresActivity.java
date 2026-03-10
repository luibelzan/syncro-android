package com.example.syncro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.CierreAdapter;
import com.example.syncro.models.CierreFila;

import java.util.ArrayList;

public class ResultadosCierresActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_cierres);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultadosCierres);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<CierreFila> datos =
                getIntent().getParcelableArrayListExtra("datos_cierres_tabla");

        if(datos != null) {
            CierreAdapter adapter = new CierreAdapter(datos);
            rv.setAdapter(adapter);
        }
    }
}