package com.celnet.syncro.objects.iec870.instantValues;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.iec870.instantValues.Iec870InstantValues;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

/**
 * Lectura de valores instantáneos por IEC-870: abre sesión, lee el serial y los
 * valores (relación de transformación, energías, potencias, tensiones e
 * intensidades) y cierra la sesión.
 */
public class Iec870InstantValuesReader {

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Iec870InstantValues leerValoresInstantaneos(Context context,
                                                              ConnectionConfig config)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                Iec870InstantValues valores = client.readInstantValues();
                valores.serie = serie;
                return valores;
            } finally {
                client.closeSession();
            }
        }
    }
}