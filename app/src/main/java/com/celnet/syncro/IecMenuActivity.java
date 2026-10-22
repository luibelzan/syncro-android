package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Menú principal del modo IEC-870 (v3.0). Igual que SecondActivity pero solo con
 * las opciones disponibles por IEC-870: Configuración, Lecturas, Parámetros,
 * Reportes, Estado sonda y Acerca de.
 */
public class IecMenuActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout btnConfig = findViewById(R.id.btnConfig);
        LinearLayout btnReads = findViewById(R.id.btnReads);
        LinearLayout btnParams = findViewById(R.id.btnParams);
        LinearLayout btnReports = findViewById(R.id.btnReports);
        LinearLayout btnSondaStatus = findViewById(R.id.btnSondaStatus);
        LinearLayout btnAbout = findViewById(R.id.btnAbout);

        /*
        btnConfig.setOnClickListener(v ->
                startActivity(new Intent(this, IecConfigActivity.class)));
        */
        btnReads.setOnClickListener(v ->
                startActivity(new Intent(this, IecReadsActivity.class)));

        btnParams.setOnClickListener(v ->
                startActivity(new Intent(this, IecParametrosActivity.class)));

        btnReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportesActivity.class)));

        btnSondaStatus.setOnClickListener(v ->
                startActivity(new Intent(this, EstadoSondaActivity.class)));

        btnAbout.setOnClickListener(v ->
                startActivity(new Intent(this, InfoActivity.class)));
    }
}