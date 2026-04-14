package com.example.syncro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class IcpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_icp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout btnIcpStatus = findViewById(R.id.btnIcpStatus);
        LinearLayout btnIcpExecute = findViewById(R.id.btnIcpExecute);
        LinearLayout btnIcpMode = findViewById(R.id.btnIcpMode);

        btnIcpStatus.setOnClickListener(v -> {

        });

        btnIcpExecute.setOnClickListener(v -> {
            Intent intent = new Intent(IcpActivity.this, IcpExecuteActivity.class);
            startActivity(intent);
        });

        btnIcpMode.setOnClickListener(v -> {

        });
    }
}