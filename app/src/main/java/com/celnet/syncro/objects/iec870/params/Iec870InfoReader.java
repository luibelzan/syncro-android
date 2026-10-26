package com.celnet.syncro.objects.iec870.params;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.iec870.params.Iec870DeviceInfo;
import com.celnet.syncro.models.iec870.params.Iec870PortConfig;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

/** Lectura (y, más adelante, programación) de la información de configuración del equipo. */
public class Iec870InfoReader {

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Iec870DeviceInfo leer(Context context, ConnectionConfig config)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                return client.readDeviceInfo();
            } finally {
                client.closeSession();
            }
        }
    }

    /**
     * Programa los puertos indicados (null = sin cambios). Bloqueante: llamar desde un hilo
     * secundario. Devuelve el número de serie.
     */
    public static String programar(Context context, ConnectionConfig config,
                                   Iec870PortConfig puerto1, Iec870PortConfig puerto2,
                                   Iec870PortConfig puertoOptico) throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                client.writePortConfig(puerto1, puerto2, puertoOptico);
                return serie;
            } finally {
                client.closeSession();
            }
        }
    }
}