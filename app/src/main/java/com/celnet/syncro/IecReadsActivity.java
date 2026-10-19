package com.celnet.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class IecReadsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iec_reads);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout btnCurvas = findViewById(R.id.btnCurvas);
        LinearLayout btnCierres = findViewById(R.id.btnCierres);
        LinearLayout btnEvents = findViewById(R.id.btnEvents);
        LinearLayout btnInstantValues = findViewById(R.id.btnInstantValues);

        btnCurvas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IecReadsActivity.this, IecCurvasActivity.class);
                startActivity(intent);
            }
        });

        btnEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IecReadsActivity.this, IecEventsActivity.class);
                startActivity(intent);
            }
        });

        /*
        btnCierres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReadsActivity.this, CierresActivity.class);
                startActivity(intent);
            }
        });


        btnInstantValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IecReadsActivity.this, ValoresInstantaneosActivity.class);
                startActivity(intent);
            }
        });
         */
    }
}