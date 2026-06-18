package com.celnet.syncro;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.CierreEnCursoAdapter;
import com.celnet.syncro.models.CierreEnCursoFila;
import com.celnet.syncro.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ResultadosCierresEnCursoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_cierres_encurso);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        ArrayList<CierreEnCursoFila> datos =
                getIntent().getParcelableArrayListExtra("datos_cierres_tabla");
        //String cntId = getIntent().getStringExtra("cntId");

        RecyclerView rv = findViewById(R.id.rvResultadosCierres);
        rv.setLayoutManager(new LinearLayoutManager(this));

        if (datos != null) {
            rv.setAdapter(new CierreEnCursoAdapter(datos));
        }

        LinearLayout btnExport = findViewById(R.id.btnExport);

    }

}