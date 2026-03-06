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

public class SyncEventLog {

    public static List<Object[]> readSyncEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Sync Event Log: 0.0.99.98.8.255
            String obisSync = "0.0.99.98.8.255";
            GXDLMSProfileGeneric syncLog = new GXDLMSProfileGeneric(obisSync);

            System.out.println("Leyendo estructura de Sync Event Log...");
            reader.read(syncLog, 3); // Carga capture_objects

            // Definición del rango temporal
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS);    // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos de sincronización horaria...");
            Object[] rows = reader.readRowsByRange(syncLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("SYNC EVENT: Fecha=" + fila[0] + " | ID=" + fila[1]);
                }
            } else {
                System.out.println("No se registraron cambios de hora en el medidor.");
            }
        } catch (Exception e) {
            System.err.println("Error en Sync Log: " + e.getMessage());
        }
        return result;
    }
}
