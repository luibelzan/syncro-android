package com.example.syncro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.adapters.EventAdapter;
import com.example.syncro.models.EventFila;

import java.util.ArrayList;

public class ResultadosEventsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_events);

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<EventFila> datos = getIntent().getParcelableArrayListExtra("datos_event_tabla");

        if(datos != null) {
            EventAdapter adapter = new EventAdapter(datos);
            rv.setAdapter(adapter);
        }
    }
}