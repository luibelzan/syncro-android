package com.example.syncro;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
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

        btnReads.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ReadsActivity.class);
            startActivity(intent);
        });

        btnConfig.setOnClickListener(v -> {
            Intent intent = new Intent(SecondActivity.this, ConfigActivity.class);
            startActivity(intent);
        });
    }
}