package com.example.syncro.objects.events;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.EventFila;

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

public class FirmwareEventLog {

    public static List<EventFila> leerFirmwareEventLog(GXDLMSReader reader, String from, String to) {
        List<EventFila> result = new ArrayList<>();
        try {
            // OBIS para Firmware Event Log: 0.0.99.98.4.255
            String obisFirmware = "0.0.99.98.4.255";
            GXDLMSProfileGeneric firmwareLog = new GXDLMSProfileGeneric(obisFirmware);

            System.out.println("Leyendo estructura de Firmware Event Log...");
            reader.read(firmwareLog, 3); // Carga capture_objects

            // --- SOLUCIÓN PARA ZIV: Check de estructura vacía ---
            if (firmwareLog.getCaptureObjects() == null || firmwareLog.getCaptureObjects().isEmpty()) {
                System.out.println("ZIV detectado o estructura vacía. Aplicando plantilla manual...");

                // 1. Definimos los objetos lógicos
                gurux.dlms.objects.GXDLMSClock clock = new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
                gurux.dlms.objects.GXDLMSData eventCode = new gurux.dlms.objects.GXDLMSData("0.0.96.11.7.255");

                // 2. Creamos las definiciones de captura (Attribute 2 es el valor del objeto)
                gurux.dlms.objects.GXDLMSCaptureObject capClock = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);
                gurux.dlms.objects.GXDLMSCaptureObject capEvent = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);

                // 3. Añadimos a la lista usando SimpleEntry para cumplir con Map.Entry
                firmwareLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(clock, capClock));
                firmwareLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(eventCode, capEvent));
            }

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

            System.out.println("Solicitando eventos de firmware...");
            Object[] rows = reader.readRowsByRange(firmwareLog, start, end);

            if (rows != null && rows.length > 0) {

                int contador = 1;

                for (Object row : rows) {

                    Object[] fila = (Object[]) row;
                    String fecha = fila[0].toString();
                    int id = Integer.parseInt(fila[1].toString());
                    String descripcion = getEventDescription(id);

                    EventFila evento = new EventFila(
                            fecha,
                            id,
                            descripcion,
                            contador
                    );

                    result.add(evento);

                    contador++;
                }

            } else {
                System.out.println("No hay eventos de firmware registrados.");
            }
        } catch (Exception e) {
            System.err.println("Error en Firmware Log: " + e.getMessage());
        }
        return result;
    }

    private static String getEventDescription(int code) {

        switch (code) {

            case 2:
                return "Power Restore";

            case 3:
                return "Power Fail";

            case 9:
                return "Clock Adjusted";

            case 10:
                return "Tariff Change";

            case 21:
                return "Local Programming";

            case 22:
                return "Remote Programming";

            case 23:
                return "Firmware Upgrade";

            default:
                return "Unknown Event";
        }
    }
}
