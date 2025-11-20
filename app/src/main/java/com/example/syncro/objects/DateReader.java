package com.example.syncro.objects;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;

import java.util.Date;

import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDateTime;
import gurux.dlms.GXReplyData;
import gurux.dlms.objects.GXDLMSClock;

public class DateReader {

    public static void readDate(Context context) throws Exception {
        DLMSConnection con = DLMSConnection.initializeConnection2(context);
        GXDLMSReader reader = con.reader;

        try {
            System.out.println("Leyendo fecha y hora del equipo");
            GXDLMSClock clock = new GXDLMSClock("0.0.1.0.0.255");
            Object value = reader.read(clock, 2); // Attribute 2 is the time value

            // Handle the value
            if (value instanceof GXDateTime) {
                GXDateTime dateTime = (GXDateTime) value;
                System.out.println("Current meter date/time: " + dateTime.toString());
            } else {
                System.out.println("Unexpected value format: " + value);
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Fecha y hora del equipo", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            DLMSConnection.closeConnection(con.reader, con.serial);
        }
    }

    public static void syncClock(Context context) throws Exception {
        DLMSConnection con = DLMSConnection.initializeConnection2(context);
        GXDLMSReader reader = con.reader;
        GXDLMSClient client = con.client;

        try {
            System.out.println("\n=== Sincronizacion de fecha y hora ===");
            GXDLMSClock clock = new GXDLMSClock();
            clock.setLogicalName("0.0.1.0.0.255");

            Object tzCounterObj = reader.read(clock, 3); // Attribute 3 = TimeZone
            int tzCounter = (tzCounterObj instanceof Number) ? ((Number) tzCounterObj).intValue() : 0;
            //readDate(this);
            System.out.println("TimeZone del contador (minutos respecto a UTC): " + tzCounter);

        /*
        // 3️⃣ Leer TimeZone del PC
        TimeZone tzLocal = TimeZone.getDefault();
        int tzLocalOffset = tzLocal.getOffset(System.currentTimeMillis()) / 60000; // minutos
        System.out.println("Zona horaria PC (minutos respecto a UTC): " + tzLocalOffset);

        // 4️⃣ Intentar ajustar TimeZone del contador al del PC
        boolean tzUpdated = false;
        clock.setTimeZone(120); // Gurux invierte el signo
        byte[][] tzWrite = client.write(clock, 3);
        GXReplyData tzReply = new GXReplyData();
        for (byte[] frame : tzWrite) {
            tzReply.clear();
            reader.readDLMSPacket(frame, tzReply);
            if (tzReply.getError() != 0) {
                throw new RuntimeException("Error escribiendo TimeZone: " + tzReply.getError());
            }
        }

        // Verificar si el TimeZone se actualizó
        Object newTzObj = reader.read(clock, 3);
        int newTzValue = (newTzObj instanceof Number) ? ((Number) newTzObj).intValue() : 0;
        if (newTzValue == tzLocalOffset) { // Gurux invierte el signo, así que comparamos con -tzLocalOffset
            System.out.println("TimeZone del contador actualizado a: " + newTzObj);
            tzUpdated = true;
        } else {
            System.out.println(
                    "⚠️ No se pudo actualizar el TimeZone del contador. Usando TimeZone actual: " + newTzValue);
        }

        // 5️⃣ Calcular hora ajustada
        long nowMillis = System.currentTimeMillis();
        long adjustedMillis;
        if (tzUpdated) {
            // Si el TimeZone se actualizó, escribir la hora en UTC
            adjustedMillis = nowMillis - (tzLocalOffset * 60L * 1000L); // Convertir a UTC
        } else {
            // Si el TimeZone no se actualizó, ajustar la hora para que el contador muestre
            // la hora correcta con su TimeZone actual
            adjustedMillis = nowMillis + ((tzCounter - tzLocalOffset) * 60L * 1000L);
        }
        Date adjustedTime = new Date(adjustedMillis);
        */
            Date now = new Date();

            // 6️⃣ Escribir hora ajustada
            clock.setTime(now);
            byte[][] timeWrite = client.write(clock, 2);
            GXReplyData timeReply = new GXReplyData();
            for (byte[] frame : timeWrite) {
                timeReply.clear();
                reader.readDLMSPacket(frame, timeReply);
                if (timeReply.getError() != 0) {
                    throw new RuntimeException("Error escribiendo hora: " + timeReply.getError());
                }
            }

            // 7️⃣ Verificación
            Object newTime = reader.read(clock, 2);
            Object finalTz = reader.read(clock, 3);
            //int finalTzValue = (finalTz instanceof Number) ? ((Number) finalTz).intValue() : 0;
            System.out.println("Nueva hora del contador: " + newTime);
            System.out.println("Nuevo TimeZone del contador: " + finalTz);
            //System.out.println("Hora del PC: " + new Date());
            System.out.println("✅ Hora del contador sincronizada correctamente.");
        } catch (Exception e) {
            Log.e("DLMS", "Error al sincronizar la fecha y hora del equipo", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            DLMSConnection.closeConnection(con.reader, con.serial);
        }
    }
}
