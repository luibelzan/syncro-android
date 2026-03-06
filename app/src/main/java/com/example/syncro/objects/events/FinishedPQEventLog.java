package com.example.syncro.objects.events;

import com.example.syncro.client.GXDLMSReader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class FinishedPQEventLog {

    public static List<Object[]> readFinishedPQEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Finished PQ Event Log: 0.0.99.98.9.255
            String obisFinishedPQ = "0.0.99.98.9.255";
            GXDLMSProfileGeneric finishedPQLog = new GXDLMSProfileGeneric(obisFinishedPQ);

            System.out.println("Leyendo estructura de Finished PQ Event Log...");
            reader.read(finishedPQLog, 3); // Carga capture_objects

            // Configurar rango de fechas
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS);    // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos PQ finalizados...");
            Object[] rows = reader.readRowsByRange(finishedPQLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("FINISHED PQ: Fecha Fin=" + fila[0] + " | ID Evento=" + fila[1]);
                }
            } else {
                System.out.println("No hay registros de eventos PQ finalizados.");
            }
        } catch (Exception e) {
            System.err.println("Error en Finished PQ Log: " + e.getMessage());
        }
        return result;
    }
}
