package com.celnet.syncro.objects.iec870.curvas;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.curvas.CurvaFila;
import com.celnet.syncro.models.iec870.curvas.Iec870LoadProfileRow;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de la curva de carga (S02) por IEC-870, devolviendo las mismas
 * {@link CurvaFila} que la lectura DLMS para reutilizar las pantallas.
 */
public class Iec870LoadProfileReader {

    public static class Resultado {
        public final String serie;
        public final ArrayList<CurvaFila> filas;

        Resultado(String serie, ArrayList<CurvaFila> filas) {
            this.serie = serie;
            this.filas = filas;
        }
    }

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Resultado leerCurvaCarga(Context context, ConnectionConfig config,
                                           String fechaInicio, String fechaFin)
            throws Iec870Exception {
        return leerCurvaCarga(context, config, fechaInicio, fechaFin,
                Iec870Client.Periodo.HORARIA);
    }

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Resultado leerCurvaCarga(Context context, ConnectionConfig config,
                                           String fechaInicio, String fechaFin,
                                           Iec870Client.Periodo periodo)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                List<Iec870LoadProfileRow> filas = client.readLoadProfile(
                        fechaInicio, fechaFin, periodo);
                AppLogger.i("IEC870", "Filas recibidas: " + filas.size());

                ArrayList<CurvaFila> resultado = new ArrayList<>();
                for (Iec870LoadProfileRow f : filas) {
                    resultado.add(new CurvaFila(
                            f.fechaHora,
                            String.format("(%02X)", f.calidad),
                            String.valueOf(f.ai),
                            String.valueOf(f.ae),
                            String.valueOf(f.r1),
                            String.valueOf(f.r2),
                            String.valueOf(f.r3),
                            String.valueOf(f.r4)));
                }
                return new Resultado(serie, resultado);
            } finally {
                client.closeSession();
            }
        }
    }
}