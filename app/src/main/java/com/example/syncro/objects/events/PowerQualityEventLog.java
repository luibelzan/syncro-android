package com.example.syncro.objects.events;

import com.example.syncro.client.GXDLMSReader;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class PowerQualityEventLog {

    public static List<Object[]> readPowerQualityEventLog(GXDLMSReader reader, String from, String to) {
        List<Object[]> result = new ArrayList<>();
        try {
            // OBIS para Power Quality Event Log: 0.0.99.98.5.255
            String obisPQ = "0.0.99.98.5.255";
            GXDLMSProfileGeneric pqLog = new GXDLMSProfileGeneric(obisPQ);

            System.out.println("Leyendo estructura de Power Quality Event Log...");
            reader.read(pqLog, 3); // Lee capture_objects (definición de columnas)

            // --- CONFIGURACIÓN DE FECHAS ULTRA-COMPATIBLE ---
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            // Creamos Calendar para asegurar que los segundos sean 0
            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 45); // Sagemcom a veces prefiere el inicio del último bloque
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end = new GXDateTime(calEnd.getTime());

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS); // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos de calidad de red...");
            Object[] rows = reader.readRowsByRange(pqLog, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    result.add(fila);
                    System.out.println("PQ EVENT: Fecha=" + fila[0] + " | ID=" + fila[1]);
                }
            } else {
                System.out.println("No hay eventos de calidad de red registrados.");
            }
        } catch (Exception e) {
            System.err.println("Error en Power Quality Log: " + e.getMessage());
        }
        return result;
    }
}
