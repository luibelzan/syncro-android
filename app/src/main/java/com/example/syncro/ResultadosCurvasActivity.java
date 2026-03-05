package com.example.syncro;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.CurvaAdapter;
import com.example.syncro.models.CurvaFila;

import java.util.ArrayList;

public class ResultadosCurvasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados_curvas);

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<CurvaFila> datos = getIntent().getParcelableArrayListExtra("datos_curva_tabla");

        if(datos != null) {
            CurvaAdapter adapter = new CurvaAdapter(datos);
            rv.setAdapter(adapter);
        }
    }
}