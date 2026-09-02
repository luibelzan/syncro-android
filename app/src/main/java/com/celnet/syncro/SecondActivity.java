package com.celnet.syncro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

public class SecondActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout btnReads = findViewById(R.id.btnReads);
        LinearLayout btnConfig = findViewById(R.id.btnConfig);
        LinearLayout btnParams = findViewById(R.id.btnParams);
        LinearLayout btnContracts = findViewById(R.id.btnContracts);
        LinearLayout btnAbout = findViewById(R.id.btnAbout);
        LinearLayout btnReports = findViewById(R.id.btnReports);
        LinearLayout btnPrime = findViewById(R.id.btnPrime);
        LinearLayout btnSondaStatus = findViewById(R.id.btnSondaStatus);

        btnReads.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ReadsActivity.class);
            startActivity(intent);
        });

        btnConfig.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ConfigActivity.class);
            startActivity(intent);
        });

        btnParams.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ParametrosActivity.class);
            startActivity(intent);
        });

        btnContracts.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ContratosActivity.class);
            startActivity(intent);
        });

        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, InfoActivity.class);
            startActivity(intent);
        });

        btnReports.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ReportesActivity.class);
            startActivity(intent);
        });

        btnPrime.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, PrimeActivity.class);
            startActivity(intent);
        });

        btnSondaStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SecondActivity.this, EstadoSondaActivity.class);
                startActivity(intent);
            }
        });
    }
}