package com.example.syncro.objects.loadProfiles;

import com.example.syncro.client.GXDLMSReader;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumSet;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class LoadProfileReader {

    public static void leerCurvaCarga(GXDLMSReader reader, String fechaInicio, String fechaFin) {
        try {
            GXDLMSProfileGeneric lp1 = new GXDLMSProfileGeneric("1.0.99.1.0.255");

            System.out.println("Leyendo estructura de LP1...");
            reader.read(lp1, 3); // Intentamos leer la estructura real

            // Solo si el medidor (como ZIV) no nos da la estructura, la forzamos
            if (lp1.getCaptureObjects().isEmpty()) {
                System.out.println("Estructura vacía (ZIV detectado): Aplicando mapeo manual...");
                addObject(lp1, new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSData("0.0.96.10.2.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.1.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.2.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.3.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.4.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.5.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.6.8.0.255"));
            } else {
                System.out.println("Estructura recibida del medidor. Columnas: " + lp1.getCaptureObjects().size());
            }

            // --- CONFIGURACIÓN DE FECHAS ULTRA-COMPATIBLE ---
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            // Creamos Calendar para asegurar que los segundos sean 0
            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(fechaInicio));
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(fechaFin));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 45); // Sagemcom a veces prefiere el inicio del último bloque
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end = new GXDateTime(calEnd.getTime());

            // IMPORTANTE: Para Sagemcom/ZIV, ignoramos todo menos fecha y hora
            // Esto evita el "Access Violation" por campos que el medidor no entiende
            EnumSet<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.MILLISECOND,
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            );
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando rango: " + fechaInicio + " a " + fechaFin);

            Object[] rows = reader.readRowsByRange(lp1, start, end);

            if (rows != null && rows.length > 0) {
                System.out.println("¡Éxito! Registros: " + rows.length);
                for (Object row : rows) {
                    Object[] columns = (Object[]) row;
                    // Impresión dinámica según el número de columnas que tenga el medidor
                    StringBuilder sb = new StringBuilder();
                    for(Object col : columns) {
                        sb.append(col).append(" | ");
                    }
                    System.out.println(sb.toString());
                }
            } else {
                System.out.println("Buffer vacío.");
            }

        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
        }
    }

    // Método auxiliar para limpiar el código
    private static void addObject(GXDLMSProfileGeneric pg, gurux.dlms.objects.GXDLMSObject obj) {
        pg.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(obj,
                new gurux.dlms.objects.GXDLMSCaptureObject(2, 0)));
    }

}
