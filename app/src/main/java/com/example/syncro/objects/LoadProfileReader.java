package com.example.syncro.objects;

import android.content.Context;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import gurux.dlms.GXArray;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDateTime;
import gurux.dlms.GXStructure;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class LoadProfileReader {

    public static List<List<Object>> readLoadProfileDayByDay(Context context, Date fromDate, Date toDate)
            throws Exception {

        DLMSConnection con = DLMSConnection.initializeConnection2(context);
        GXDLMSReader reader = con.reader;
        GXDLMSClient client = con.client;
        try {
            List<List<Object>> allRecords = new ArrayList<>();

            GXDLMSProfileGeneric loadProfile = new GXDLMSProfileGeneric("1.0.99.1.0.255");

            // 1. Leer capture objects
            reader.read(loadProfile, 3);
            System.out.println("Capture objects: " + loadProfile.getCaptureObjects().size());

            // 2. Leer número de entradas y capture period
            reader.read(loadProfile, 7);
            reader.read(loadProfile, 4);
            long totalEntries = loadProfile.getEntriesInUse();
            long capturePeriod = loadProfile.getCapturePeriod(); // en segundos
            System.out.println("Entradas totales: " + totalEntries + ", Capture period: " + capturePeriod + "s");

            if (capturePeriod != 3600) {
                throw new Exception("Este ejemplo asume 1 hora por entrada (3600s). Ajusta si es diferente.");
            }

            int entriesPerDay = 24;
            int blockSize = 8; // como en el log: 6-8 por bloque

            // 3. Iterar día por día
            Calendar cal = Calendar.getInstance();
            cal.setTime(fromDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(toDate);

            while (!cal.after(endCal)) {
                Date dayStart = cal.getTime();
                Date dayEnd = new Date(cal.getTimeInMillis() + 24 * 60 * 60 * 1000L);

                System.out.println("Leyendo datos del día: " +
                        String.format("%1$td/%1$tm/%1$tY", dayStart));

                // Calcular índice aproximado del día (desde el final)
                long now = System.currentTimeMillis();
                long dayMs = dayStart.getTime();
                long daysAgo = (now - dayMs) / (24 * 60 * 60 * 1000L);
                int approxIndex = (int) (totalEntries - (daysAgo * entriesPerDay));

                // Ajustar límites
                int startIndex = Math.max(1, approxIndex - entriesPerDay); // margen
                int safeEnd = (int) Math.min(totalEntries, startIndex + entriesPerDay * 2);

                List<List<Object>> dayRecords = new ArrayList<>();

                // Leer en bloques de `blockSize`
                for (int idx = startIndex; idx < safeEnd; idx += blockSize) {
                    int count = Math.min(blockSize, safeEnd - idx);
                    if (count <= 0)
                        break;

                    System.out.println("  Leyendo bloque: índice " + idx + ", count: " + count);
                    try {
                        Object result = reader.readRowsByEntry(loadProfile, idx, count);
                        List<List<Object>> block = parseResult(result);

                        // Filtrar por fecha del día
                        for (List<Object> row : block) {
                            if (row.isEmpty())
                                continue;
                            Object timeObj = row.get(0);
                            if (!(timeObj instanceof GXDateTime))
                                continue;

                            GXDateTime rowTime = (GXDateTime) timeObj;
                            Date rowDate = rowTime.getMeterCalendar().getTime();

                            if (rowDate.compareTo(dayStart) >= 0 && rowDate.compareTo(dayEnd) < 0) {
                                dayRecords.add(row);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error leyendo bloque " + idx + ": " + e.getMessage());
                        // Continuar con el siguiente bloque
                    }

                    // Pequeña pausa para no saturar
                    Thread.sleep(50);
                }

                System.out.println("  -> " + dayRecords.size() + " registros recuperados para este día");
                allRecords.addAll(dayRecords);

                // Siguiente día
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            System.out.println("Total registros recuperados: " + allRecords.size());
            return allRecords;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<List<Object>> parseResult(Object result) {
        List<List<Object>> records = new ArrayList<>();
        if (result instanceof GXArray) {
            for (Object rowObj : (GXArray) result) {
                records.add(parseRow(rowObj));
            }
        } else if (result instanceof Object[]) {
            for (Object rowObj : (Object[]) result) {
                records.add(parseRow(rowObj));
            }
        }
        return records;
    }

    private static List<Object> parseRow(Object rowObj) {
        List<Object> row = new ArrayList<>();
        if (rowObj instanceof GXStructure) {
            GXStructure struct = (GXStructure) rowObj;
            for (Object value : struct) {
                if (value instanceof GXDateTime) {
                    row.add(value);
                } else {
                    row.add(value);
                }
            }
        } else if (rowObj instanceof Object[]) {
            row.addAll(Arrays.asList((Object[]) rowObj));
        } else {
            row.add(rowObj);
        }
        return row;
    }

}
