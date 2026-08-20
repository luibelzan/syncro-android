package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.utils.AppLogger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import gurux.dlms.GXDateTime;
import gurux.dlms.GXReplyData;
import gurux.dlms.objects.GXDLMSClock;

public class DateReader {

    // Formato fijo y sin ambigüedad: nada de depender de GXDateTime.toString()
    // (su formato de salida puede variar según el locale del dispositivo).
    // ResultadosDateActivity debe parsear exactamente este mismo patrón.
    private static final String FORMATO_SALIDA = "yyyy-MM-dd HH:mm:ss";

    public static String readDate(GXDLMSReader reader) throws Exception {

        try {
            AppLogger.i("Syncro", "Leyendo fecha y hora del equipo");
            GXDLMSClock clock = new GXDLMSClock("0.0.1.0.0.255");
            Object value = reader.read(clock, 2);

            if (value instanceof GXDateTime) {
                GXDateTime dateTime = (GXDateTime) value;
                Date fecha = dateTime.getMeterCalendar().getTime();
                SimpleDateFormat sdf = new SimpleDateFormat(FORMATO_SALIDA, Locale.US);
                String resultado = sdf.format(fecha);
                AppLogger.i("Syncro", "Fecha y hora del equipo (formateada): " + resultado);
                return resultado;
            } else {
                return "Formato inesperado: " + value;
            }
        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al leer Fecha y hora del equipo: " + e.getMessage());
            throw e;
        }
    }

    public static void syncClock(GXDLMSReader reader, GXDLMSSecureClient2 client,
                                 int utcOffsetMinutes, Date dateTime) throws Exception {
        try {
            AppLogger.i("Syncro", "Sincronizacion de fecha y hora");
            GXDLMSClock clock = new GXDLMSClock();
            clock.setLogicalName("0.0.1.0.0.255");

            int hours   = Math.abs(utcOffsetMinutes) / 60;
            int minutes = Math.abs(utcOffsetMinutes) % 60;
            String sign = utcOffsetMinutes >= 0 ? "+" : "-";
            String tzId = String.format("GMT%s%02d:%02d", sign, hours, minutes);
            TimeZone timeZone = TimeZone.getTimeZone(tzId);
            AppLogger.i("Syncro", "TimeZone: " + tzId);
            AppLogger.i("Syncro", "Fecha y hora a escribir: " + dateTime);

            Object tzCounterObj = reader.read(clock, 3);
            int tzCounter = (tzCounterObj instanceof Number) ? ((Number) tzCounterObj).intValue() : 0;
            AppLogger.i("Syncro", "TimeZone actual del contador (min respecto a UTC): " + tzCounter);

            if (tzCounter != utcOffsetMinutes) {
                clock.setTimeZone(-utcOffsetMinutes);
                byte[][] tzWrite = client.write(clock, 3);
                GXReplyData tzReply = new GXReplyData();
                for (byte[] frame : tzWrite) {
                    tzReply.clear();
                    reader.readDLMSPacket(frame, tzReply);
                    if (tzReply.getError() != 0) {
                        throw new RuntimeException("Error escribiendo TimeZone: " + tzReply.getError());
                    }
                }
                AppLogger.i("Syncro", "TimeZone del contador actualizado a: " + (-utcOffsetMinutes));
            } else {
                AppLogger.i("Syncro", "TimeZone del contador ya es correcto, no se modifica.");
            }

            Calendar cal = Calendar.getInstance(timeZone);
            cal.setTime(dateTime);

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

            Object newTime = reader.read(clock, 2);
            Object finalTz = reader.read(clock, 3);
            AppLogger.i("Syncro", "Nueva hora del contador: " + newTime);
            AppLogger.i("Syncro", "Nuevo TimeZone del contador: " + finalTz);
            AppLogger.i("Syncro", "Hora del contador sincronizada correctamente.");

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al sincronizar la fecha y hora del equipo: " + e.getMessage());
            throw e;
        }
    }
}