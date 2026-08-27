package com.celnet.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.models.parameters.DifferentialCurrentDetectionInfo;

import java.util.Locale;

public class ResultadosDeteccionCorrienteDiferencialActivity extends BaseActivity {

    public static final String EXTRA_INFO = "extra_differential_current_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_deteccion_corriente_diferencial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvUmbralVariacion = findViewById(R.id.tvUmbralVariacion);
        TextView tvUmbralTiempo = findViewById(R.id.tvUmbralTiempo);
        TextView tvUmbralCorrienteMinima = findViewById(R.id.tvUmbralCorrienteMinima);
        TextView tvCorrienteDiferencial = findViewById(R.id.tvCorrienteDiferencial);

        DifferentialCurrentDetectionInfo datos = getIntent().getParcelableExtra(EXTRA_INFO);

        if (datos == null) {
            return;
        }

        tvUmbralVariacion.setText(String.format(Locale.getDefault(), "%.2f %%",
                datos.getUmbralVariacionPorcentaje()));
        tvUmbralTiempo.setText(String.format(Locale.getDefault(), "%d s",
                datos.getUmbralTiempoSegundos()));
        tvUmbralCorrienteMinima.setText(String.format(Locale.getDefault(), "%.1f A",
                datos.getUmbralCorrienteMinimaAmperios()));
        tvCorrienteDiferencial.setText(String.format(Locale.getDefault(), "%.1f A",
                datos.getCorrienteDiferencialActualAmperios()));
    }
}