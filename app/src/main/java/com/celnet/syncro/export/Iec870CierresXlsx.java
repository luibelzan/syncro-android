package com.celnet.syncro.export;

import com.celnet.syncro.models.iec870.cierres.Iec870BillingRecord;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Informe de cierres (tarificación) en Excel, hoja "Cierre_Mensual": una fila por contrato y
 * periodo tarifario (0 = total, 1..6) de cada cierre, con las columnas del informe de
 * referencia. Los bits de calidad se escriben como el número que envía el contador (en decimal).
 */
public final class Iec870CierresXlsx {

    public static final String HOJA = "Cierre_Mensual";

    private Iec870CierresXlsx() {
    }

    public static void generar(OutputStream out, String cups, String contador,
                               List<Iec870BillingRecord> registros) throws IOException {
        MiniXlsx x = new MiniXlsx(HOJA, new double[]{25, 10, 10, 9, 21, 21, 25, 28,
                30, 30, 30, 30, 30, 30, 30, 11, 21, 26, 20, 23});
        x.addHeader("CUPS", "Contador", "Contrato", "Periodo", "Fecha Inicio", "Fecha Fin",
                "Energia Activa Absoluta", "Energia Activa Incremental",
                "Bit de Calidad Energia Activa",
                "Energia Reactiva Inductiva Absoluta", "Energia Reactiva Inductiva Incremental",
                "Bit de Calidad Energia Reactiva Inductiva",
                "Energia Reactiva Capacitiva Absoluta", "Energia Reactiva Capacitiva Incremental",
                "Bit de Calidad Energia Reactiva Capacitiva",
                "Maximetro", "Fecha Maximetro", "Bit de Calidad Maximetro",
                "Exceso de Potencia", "Bit de Calidad Exceso");
        for (Iec870BillingRecord r : registros) {
            x.addRow(cups, contador, r.contrato, r.tarifa,
                    fecha(r.inicio), fecha(r.fin),
                    r.activaAbs, r.activaInc, r.activaCal,
                    r.reactivaIndAbs, r.reactivaIndInc, r.reactivaIndCal,
                    r.reactivaCapAbs, r.reactivaCapInc, r.reactivaCapCal,
                    r.maximetro, fecha(r.maximetroFecha), r.maximetroCal,
                    r.exceso, r.excesoCal);
        }
        x.writeTo(out);
    }

    /** Celda de fecha, o vacía si el contador no la envió. */
    private static MiniXlsx.ExcelDate fecha(String t) {
        return t == null || t.length() < 16 ? null : MiniXlsx.ExcelDate.fromText(t);
    }
}