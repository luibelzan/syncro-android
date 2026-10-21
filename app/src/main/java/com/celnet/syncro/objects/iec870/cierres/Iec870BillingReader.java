package com.celnet.syncro.objects.iec870.cierres;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.iec870.cierres.Iec870BillingRecord;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de tarificación (valores memorizados) por IEC-870. Una sola sesión para
 * todos los contratos pedidos.
 */
public class Iec870BillingReader {

    public static class Resultado {
        public final String serie;
        public final ArrayList<Iec870BillingRecord> registros;

        Resultado(String serie, ArrayList<Iec870BillingRecord> registros) {
            this.serie = serie;
            this.registros = registros;
        }
    }

    /**
     * Bloqueante: llamar siempre desde un hilo secundario.
     *
     * @param contratos contratos a leer (1..3), en el orden en que se leen
     */
    public static Resultado leerMemorizados(Context context, ConnectionConfig config,
                                            int[] contratos, String fechaInicio, String fechaFin)
            throws Iec870Exception {
        return leer(context, config, contratos, fechaInicio, fechaFin);
    }

    /** Valores en curso (sin fechas). Bloqueante: llamar desde un hilo secundario. */
    public static Resultado leerEnCurso(Context context, ConnectionConfig config, int[] contratos)
            throws Iec870Exception {
        return leer(context, config, contratos, null, null);
    }

    /** Con fechas nulas lee los valores en curso; con fechas, los memorizados. */
    private static Resultado leer(Context context, ConnectionConfig config, int[] contratos,
                                  String fechaInicio, String fechaFin)
            throws Iec870Exception {
        boolean enCurso = fechaInicio == null;

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                ArrayList<Iec870BillingRecord> todos = new ArrayList<>();
                for (int contrato : contratos) {
                    List<Iec870BillingRecord> regs = enCurso
                            ? client.readCurrentBilling(contrato)
                            : client.readStoredBilling(contrato, fechaInicio, fechaFin);
                    AppLogger.i("IEC870", "Contrato " + contrato + ": " + regs.size() + " registros");
                    todos.addAll(regs);
                }
                return new Resultado(serie, todos);
            } finally {
                client.closeSession();
            }
        }
    }
}