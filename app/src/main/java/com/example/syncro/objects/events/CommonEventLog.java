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

public class CommonEventLog {

    public static List<Object[]> leerCommonEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Common Event Log: 0.0.99.98.7.255
            String obisCommon = "0.0.99.98.7.255";
            GXDLMSProfileGeneric commonLog = new GXDLMSProfileGeneric(obisCommon);

            System.out.println("Leyendo estructura de Common Event Log...");
            reader.read(commonLog, 3); // Carga capture_objects

            // Rango de fechas
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS);    // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos comunes...");
            Object[] rows = reader.readRowsByRange(commonLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    // fila[0] suele ser la fecha, fila[1] el ID del evento
                    System.out.println("COMMON EVENT: Fecha=" + fila[0] + " | ID=" + fila[1]);
                }
            } else {
                System.out.println("No se encontraron eventos comunes.");
            }
        } catch (Exception e) {
            System.err.println("Error en Common Log: " + e.getMessage());
        }
        return result;
    }
}
