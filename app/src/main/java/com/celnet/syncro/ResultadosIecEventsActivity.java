package com.celnet.syncro;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.adapters.iec870.IecEventAdapter;
import com.celnet.syncro.export.Iec870EventsXlsx;
import com.celnet.syncro.export.IecExcelExport;
import com.celnet.syncro.models.iec870.eventos.Iec870EventFila;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;

/** Resultados de eventos IEC-870: fecha, SPA, SPQ, SPI y descripción; se exportan a Excel. */
public class ResultadosIecEventsActivity extends BaseActivity {

    public static final String EXTRA_EVENTOS = "datos_event_iec";
    public static final String EXTRA_CNT_ID = "cntId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultados_iec_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ExtendedFloatingActionButton btnExport = findViewById(R.id.btnExport);

        String cntId = getIntent().getStringExtra(EXTRA_CNT_ID);
        @SuppressWarnings({"unchecked", "deprecation"})
        ArrayList<Iec870EventFila> datos =
                (ArrayList<Iec870EventFila>) getIntent().getSerializableExtra(EXTRA_EVENTOS);

        if (datos != null) rv.setAdapter(new IecEventAdapter(datos));

        btnExport.setOnClickListener(v -> {
            if (datos == null || datos.isEmpty()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
                return;
            }
            IecExcelExport.exportar(this, cntId,
                    id -> nombreExcel(id, datos),
                    out -> Iec870EventsXlsx.generar(out, cntId != null ? cntId : "", datos));
        });
    }

    /** "IdCnt_Eventos_yyyyMMddHHmmss.xlsx" (la fecha y hora de la exportación, como en DLMS). */
    private static String nombreExcel(String id, ArrayList<Iec870EventFila> datos) {
        return IecExcelExport.limpiar(id) + "_Eventos_" + IecExcelExport.fechaActual() + ".xlsx";
    }
}