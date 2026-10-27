package com.celnet.syncro.objects.iec870.events;

import android.content.Context;

import com.celnet.syncro.client.Iec870Client;
import com.celnet.syncro.client.Iec870Exception;
import com.celnet.syncro.models.events.EventFila;
import com.celnet.syncro.models.iec870.eventos.Iec870Event;
import com.celnet.syncro.models.iec870.eventos.Iec870Events;
import com.celnet.syncro.session.ConnectionConfig;
import com.celnet.syncro.session.Iec870Connection;
import com.celnet.syncro.utils.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de eventos por IEC-870, devolviendo las mismas {@link EventFila} que la
 * lectura DLMS para reutilizar ResultadosEventsActivity. Con {@link Iec870Events#REG_TODOS}
 * lee todos los registros de eventos.
 */
public class Iec870EventsReader {

    public static class Resultado {
        public final String serie;
        public final ArrayList<EventFila> filas;
        /** Registros que no se pudieron leer con "todos los eventos" (vacío si no hubo fallos). */
        public final List<String> avisos;

        Resultado(String serie, ArrayList<EventFila> filas, List<String> avisos) {
            this.serie = serie;
            this.filas = filas;
            this.avisos = avisos;
        }
    }

    /** Bloqueante: llamar siempre desde un hilo secundario. */
    public static Resultado leerEventos(Context context, ConnectionConfig config, int registro,
                                        String fechaInicio, String fechaFin)
            throws Iec870Exception {

        try (Iec870Connection conn = new Iec870Connection(config)) {
            Iec870Client client = conn.open(context);
            client.setTrace(linea -> AppLogger.i("IEC870", linea));

            client.openSession();
            try {
                String serie = client.readSerial();
                AppLogger.i("IEC870", "Serial del contador : " + serie);

                List<String> avisos = new ArrayList<>();
                List<Iec870Event> eventos = registro == Iec870Events.REG_TODOS
                        ? client.readAllEvents(fechaInicio, fechaFin, avisos)
                        : client.readEvents(registro, fechaInicio, fechaFin);
                AppLogger.i("IEC870", "Eventos recibidos: " + eventos.size());

                ArrayList<EventFila> filas = new ArrayList<>();
                for (Iec870Event e : eventos) {
                    // id = código del evento (SPQ), cod = grupo (SPA)
                    filas.add(new EventFila(e.fechaHora, e.spq, e.descripcion, e.spa));
                }
                return new Resultado(serie, filas, avisos);
            } finally {
                client.closeSession();
            }
        }
    }
}