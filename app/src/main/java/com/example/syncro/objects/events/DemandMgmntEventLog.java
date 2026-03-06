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

public class DemandMgmntEventLog {

    public static List<Object[]> readDemandMgmntEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Demand Management Event Log: 0.0.99.98.6.255
            String obisDemand = "0.0.99.98.6.255";
            GXDLMSProfileGeneric demandLog = new GXDLMSProfileGeneric(obisDemand);

            System.out.println("Leyendo estructura de Demand Management Event Log...");
            reader.read(demandLog, 3); // Leer definición de columnas

            // Rango de fechas
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS);    // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos de gestión de demanda...");
            Object[] rows = reader.readRowsByRange(demandLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("DEMAND EVENT: Fecha=" + fila[0] + " | ID=" + fila[1]);
                }
            } else {
                System.out.println("No hay eventos de gestión de demanda registrados.");
            }
        } catch (Exception e) {
            System.err.println("Error en Demand Mgmnt Log: " + e.getMessage());
        }
        return result;
    }
}
