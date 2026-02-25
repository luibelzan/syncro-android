package com.example.syncro.objects.events;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class StandarEventLogReader {
    public static List<Object[]> readStandardEventLog(GXDLMSReader reader, LocalDate from, LocalDate to) {
        List<Object[]> result = new ArrayList<>();
        try {
            String obisLog = "0.0.99.98.0.255";
            GXDLMSProfileGeneric eventLog = new GXDLMSProfileGeneric(obisLog);

            System.out.println("Leyendo estructura de Standard Event Log...");
            reader.read(eventLog, 3); // Atributo 3: Capture Objects

            // --- SOLUCIÓN PARA ZIV: Check de estructura vacía ---
            if (eventLog.getCaptureObjects() == null || eventLog.getCaptureObjects().isEmpty()) {
                System.out.println("ZIV detectado o estructura vacía. Aplicando plantilla manual...");

                // 1. Definimos los objetos lógicos
                gurux.dlms.objects.GXDLMSClock clock = new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
                gurux.dlms.objects.GXDLMSData eventCode = new gurux.dlms.objects.GXDLMSData("0.0.96.11.7.255");

                // 2. Creamos las definiciones de captura (Attribute 2 es el valor del objeto)
                gurux.dlms.objects.GXDLMSCaptureObject capClock = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);
                gurux.dlms.objects.GXDLMSCaptureObject capEvent = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);

                // 3. Añadimos a la lista usando SimpleEntry para cumplir con Map.Entry
                eventLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(clock, capClock));
                eventLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(eventCode, capEvent));
            }

            // Configurar rango
            GXDateTime start = new GXDateTime(from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0, 0);
            GXDateTime end = new GXDateTime(to.getYear(), to.getMonthValue(), to.getDayOfMonth(), 23, 59, 59, 0);

            // ZIV suele ser muy estricto con los bytes de estado en el filtrado por rango
            Set<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS);

            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos desde " + from + " hasta " + to);

            // En ZIV por TCP/Gateway, a veces es mejor leer por entradas (AllRows)
            // si el filtro por rango da error, pero intentemos primero el rango:
            Object[] rows = reader.readRowsByRange(eventLog, start, end);

            if (rows != null) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("Evento detectado: Fecha=" + fila[0] + " | ID=" + fila[1]);
                }
            }
        } catch (Exception e) {
            // Tip Industrial: Si falla por "Access Error" al filtrar, el Gateway podría
            // estar bloqueando Selective Access
            System.err.println("Error leyendo logs: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
