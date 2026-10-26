package com.celnet.syncro.objects.iec870.params;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.iec870.params.Iec870TrafoRatio;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

/** Lectura y programación de la relación de transformación por IEC-870. */
public class Iec870TrafoReader {

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Iec870TrafoRatio leer(Context context, ConnectionConfig config)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                return client.readTransformerRatio();
            } finally {
                client.closeSession();
            }
        }
    }

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static void programar(Context context, ConnectionConfig config, Iec870TrafoRatio rt)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                client.writeTransformerRatio(rt);
            } finally {
                client.closeSession();
            }
        }
    }
}