package com.celnet.syncro.objects.iec870.params;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.iec870.params.Iec870DateTime;
import com.celnet.syncro.models.iec870.params.Iec870DaylightChange;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

/** Lectura y programación de la fecha y hora del contador por IEC-870. */
public class Iec870DateReader {

    /** Fecha y hora del contador y sus fechas oficiales de cambio de hora. */
    public static class Resultado {
        public final Iec870DateTime fecha;
        public final Iec870DaylightChange cambioHora;

        Resultado(Iec870DateTime fecha, Iec870DaylightChange cambioHora) {
            this.fecha = fecha;
            this.cambioHora = cambioHora;
        }
    }

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Resultado leerFecha(Context context, ConnectionConfig config)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                Iec870DateTime fecha = client.readDateTime();
                AppLogger.i("IEC870", "Current Meter date time : " + fecha.toText());

                Iec870DaylightChange cambio = client.readDaylightChange();
                return new Resultado(fecha, cambio);
            } finally {
                client.closeSession();
            }
        }
    }

    /**
     * Programa la fecha y hora y devuelve la que el contador tiene a continuación
     * (se vuelve a leer para comprobar el resultado). Bloqueante: hilo secundario.
     */
    public static Iec870DateTime programarFecha(Context context, ConnectionConfig config,
                                                Iec870DateTime nueva) throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                client.writeDateTime(nueva);
                Iec870DateTime leida = client.readDateTime();
                AppLogger.i("IEC870", "Nueva hora del contador : " + leida.toText());
                return leida;
            } finally {
                client.closeSession();
            }
        }
    }
}