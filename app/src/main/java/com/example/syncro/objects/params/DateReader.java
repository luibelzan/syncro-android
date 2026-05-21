package com.example.syncro.objects.params;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import gurux.dlms.GXDateTime;
import gurux.dlms.GXReplyData;
import gurux.dlms.objects.GXDLMSClock;

public class DateReader {

    public static String readDate(GXDLMSReader reader) throws Exception {

        try {
            System.out.println("Leyendo fecha y hora del equipo");
            GXDLMSClock clock = new GXDLMSClock("0.0.1.0.0.255");
            Object value = reader.read(clock, 2);

            if (value instanceof GXDateTime) {
                GXDateTime dateTime = (GXDateTime) value;
                return dateTime.toString(); // 👈 devolvemos la fecha
            } else {
                return "Formato inesperado: " + value;
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Fecha y hora del equipo", e);
            throw e;
        }
    }

    public static void syncClock(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                 int utcOffsetMinutes, Date dateTime) throws Exception {
        try {
            System.out.println("\n=== Sincronizacion de fecha y hora ===");
            GXDLMSClock clock = new GXDLMSClock();
            clock.setLogicalName("0.0.1.0.0.255");

            // Construir TimeZone a partir del offset en minutos
            int hours   = Math.abs(utcOffsetMinutes) / 60;
            int minutes = Math.abs(utcOffsetMinutes) % 60;
            String sign = utcOffsetMinutes >= 0 ? "+" : "-";
            String tzId = String.format("GMT%s%02d:%02d", sign, hours, minutes);
            TimeZone timeZone = TimeZone.getTimeZone(tzId);
            System.out.println("TimeZone: " + tzId);
            System.out.println("Fecha y hora a escribir: " + dateTime);

            // Leer TimeZone actual del contador
            Object tzCounterObj = reader.read(clock, 3);
            int tzCounter = (tzCounterObj instanceof Number) ? ((Number) tzCounterObj).intValue() : 0;
            System.out.println("TimeZone actual del contador (min respecto a UTC): " + tzCounter);

            // Escribir TimeZone al contador si difiere
            if (tzCounter != utcOffsetMinutes) {
                clock.setTimeZone(-utcOffsetMinutes); // Gurux invierte el signo
                byte[][] tzWrite = client.write(clock, 3);
                GXReplyData tzReply = new GXReplyData();
                for (byte[] frame : tzWrite) {
                    tzReply.clear();
                    reader.readDLMSPacket(frame, tzReply);
                    if (tzReply.getError() != 0) {
                        throw new RuntimeException("Error escribiendo TimeZone: " + tzReply.getError());
                    }
                }
                System.out.println("TimeZone del contador actualizado a: " + (-utcOffsetMinutes));
            } else {
                System.out.println("TimeZone del contador ya es correcto, no se modifica.");
            }

            // Construir Calendar con la fecha/hora recibida y la zona horaria indicada
            Calendar cal = Calendar.getInstance(timeZone);
            cal.setTime(dateTime); // <-- usa la fecha pasada como parámetro, no System.currentTimeMillis()

            GXDateTime gxTime = new GXDateTime(cal);
            clock.setTime(gxTime.getValue());

            byte[][] timeWrite = client.write(clock, 2);
            GXReplyData timeReply = new GXReplyData();
            for (byte[] frame : timeWrite) {
                timeReply.clear();
                reader.readDLMSPacket(frame, timeReply);
                if (timeReply.getError() != 0) {
                    throw new RuntimeException("Error escribiendo hora: " + timeReply.getError());
                }
            }

            // Verificación
            Object newTime = reader.read(clock, 2);
            Object finalTz = reader.read(clock, 3);
            System.out.println("Nueva hora del contador: " + newTime);
            System.out.println("Nuevo TimeZone del contador: " + finalTz);
            System.out.println("✅ Hora del contador sincronizada correctamente.");

        } catch (Exception e) {
            Log.e("DLMS", "Error al sincronizar la fecha y hora del equipo", e);
            throw e;
        }
    }
}
