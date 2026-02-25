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

public class PowContractEventLog {

    public static List<Object[]> readImpPowContractEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Import Power Contract Event Log: 0.0.99.98.3.255
            String obisImpPow = "0.0.99.98.3.255";
            GXDLMSProfileGeneric impPowLog = new GXDLMSProfileGeneric(obisImpPow);

            System.out.println("Leyendo estructura de Import Power Contract Event Log...");
            reader.read(impPowLog, 3); // Carga la definición de las columnas

            // Configuración del rango de tiempo
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS); // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos de contrato de potencia...");
            Object[] rows = reader.readRowsByRange(impPowLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("EVENTO CONTRATO: Fecha=" + fila[0] + " | ID=" + fila[1]);
                }
            } else {
                System.out.println("No hay eventos de contrato de potencia en este rango.");
            }
        } catch (Exception e) {
            System.err.println("Error en Imp Pow Contract Log: " + e.getMessage());
        }
        return result;
    }
}
