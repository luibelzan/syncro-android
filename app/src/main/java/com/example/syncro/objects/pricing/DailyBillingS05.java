package com.example.syncro.objects.pricing;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreFila;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class DailyBillingS05 {

    public static ArrayList<CierreFila> leerS05(GXDLMSReader reader, String from, String to, int contract) {
        ArrayList<CierreFila> result = new ArrayList<>();

        try {
            System.out.println("Leyendo S05 del contrato " + contract + "...");

            String obisS05 = "0.0.98.2." + contract + ".255";

            // 1️⃣ Crear Profile Generic
            GXDLMSProfileGeneric s05 = new GXDLMSProfileGeneric(obisS05);

            // 2️⃣ Leer capture objects
            System.out.println("Leyendo capture objects de S05...");
            reader.read(s05, 3);

            // 3️⃣ Configuración de fechas
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end = new GXDateTime(calEnd.getTime());

            EnumSet<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.MILLISECOND,
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            );

            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Leyendo S05 desde " + from + " hasta " + to);

            // 4️⃣ Leer filas
            Object[] rows = reader.readRowsByRange(s05, start, end);

            if (rows != null && rows.length > 0) {

                for (Object row : rows) {

                    Object[] fila = (Object[]) row;

                    String fechaOriginal = fila[0].toString();
                    String fechaFormateada = formatearFechaS05(fechaOriginal);

                    // 🔥 MAPPING CORRECTO
                    for (int p = 0; p <= 6; p++) {

                        Object activa = fila[1 + p];
                        Object export = fila[8 + p];
                        Object r1 = fila[15 + p];
                        Object r2 = fila[22 + p];
                        Object r3 = fila[29 + p];
                        Object r4 = fila[36 + p];

                        result.add(new CierreFila(
                                fechaFormateada,
                                contract,   // ✅ correcto
                                p,          // ✅ correcto
                                String.valueOf(activa),
                                String.valueOf(export),
                                String.valueOf(r1),
                                String.valueOf(r2),
                                String.valueOf(r3),
                                String.valueOf(r4)
                        ));
                    }
                }

            } else {
                System.out.println("No hay registros en este rango.");
            }

        } catch (Exception e) {
            System.err.println("Error durante la lectura de S05: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // 🗓️ Formateo de fecha
    private static String formatearFechaS05(String fecha) {
        try {
            String[] partes = fecha.split(" ");
            String[] dmy = partes[0].split("/");

            return String.format("20%s/%02d/%02d 00:00:00.000W",
                    dmy[2],
                    Integer.parseInt(dmy[1]),
                    Integer.parseInt(dmy[0]));

        } catch (Exception e) {
            return fecha + ".000W";
        }
    }
}