package com.celnet.syncro.export;

import com.celnet.syncro.models.iec870.curvas.Iec870LoadProfileRow;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Informe de curvas de carga en Excel (hoja "Curvas"), con las columnas del informe de
 * referencia: CUPS, Contador, Fecha, y para cada energía su valor y su bit de calidad (hex de
 * 2 cifras).
 *
 * Correspondencia con los totales integrados de la ASDU 11 (direcciones 1..6):
 * A+ = dirección 1, A- = 2, Ri+ = cuadrante 1 (3), Ri- = cuadrante 2 (4), Rc- = cuadrante 3 (5),
 * Rc+ = cuadrante 4 (6).
 */
public final class Iec870CurvasXlsx {

    public static final String HOJA = "Curvas";

    private Iec870CurvasXlsx() {
    }

    public static void generar(OutputStream out, String cups, String contador,
                               List<Iec870LoadProfileRow> filas) throws IOException {
        MiniXlsx x = new MiniXlsx(HOJA, new double[]{25, 10, 21, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30});
        x.addHeader("CUPS", "Contador", "Fecha",
                "Energia Activa Importada (A+)", "Bit de Calidad Energia Activa (A+)",
                "Energia Activa Exportada (A-)", "Bit de Calidad Energia Activa (A-)",
                "Energia Reactiva Inductiva Importada (Ri+)", "Bit de Calidad Energia Inductiva",
                "Energia Reactiva Capacitiva Importada (Rc+)", "Bit de Calidad Energia Capacitiva",
                "Energia Reactiva Inductiva Exportada (Ri-)", "Bit de Calidad Energia Inductiva",
                "Energia Reactiva Capacitiva Exportada (Rc-)", "Bit de Calidad Energia Capacitiva");
        for (Iec870LoadProfileRow f : filas) {
            MiniXlsx.ExcelDate fecha = MiniXlsx.ExcelDate.fromText(f.fechaHora);
            x.addRow(cups, contador, fecha,
                    f.ai, q(f, 1),
                    f.ae, q(f, 2),
                    f.r1, q(f, 3),
                    f.r4, q(f, 6),
                    f.r2, q(f, 4),
                    f.r3, q(f, 5));
        }
        x.writeTo(out);
    }

    private static String q(Iec870LoadProfileRow f, int direccion) {
        return String.format("%02X", f.calidades[direccion]);
    }
}