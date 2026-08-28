package com.celnet.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.prime.PrimeSecurityInfo;

public class ResultadosPrimeSeguridadActivity extends BaseActivity {

    public static final String EXTRA_INFO = "extra_prime_security_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_prime_seguridad);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvDualStackVersion = findViewById(R.id.tvDualStackVersion);
        TextView tvSarSize = findViewById(R.id.tvSarSize);
        TextView tvArq = findViewById(R.id.tvArq);
        TextView tvConstellationCoding = findViewById(R.id.tvConstellationCoding);

        PrimeSecurityInfo datos = getIntent().getParcelableExtra(EXTRA_INFO);

        if (datos == null) {
            return;
        }

        tvDualStackVersion.setText(datos.getDualStackVersion());
        tvSarSize.setText(datos.getSarSize());
        tvArq.setText(datos.isArqEnabled() ? "Enabled" : "Disabled");
        tvConstellationCoding.setText(datos.getConstellationCoding().toString());
    }
}