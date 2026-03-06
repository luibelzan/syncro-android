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

public class FraudEventLog {

    public static List<Object[]> readFraudEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Fraud Event Log: 0.0.99.98.1.255
            String obisFraud = "0.0.99.98.1.255";
            GXDLMSProfileGeneric fraudLog = new GXDLMSProfileGeneric(obisFraud);

            System.out.println("Leyendo estructura de Fraud Event Log...");
            reader.read(fraudLog, 3); // Leer capture_objects

            // Rango de fechas
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS); // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos de fraude...");
            Object[] rows = reader.readRowsByRange(fraudLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("FRAUDE DETECTADO: Fecha=" + fila[0] + " | ID Evento=" + fila[1]);
                }
            } else {
                System.out.println("No se encontraron eventos de fraude en el periodo solicitado.");
            }
        } catch (Exception e) {
            System.err.println("Error en Fraud Log: " + e.getMessage());
        }
        return result;
    }
}
