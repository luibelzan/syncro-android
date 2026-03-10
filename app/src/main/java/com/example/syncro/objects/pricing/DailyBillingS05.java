package com.example.syncro.objects.pricing;

import com.example.syncro.client.GXDLMSReader;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class DailyBillingS05 {

    public static List<Object[]> leerS05(GXDLMSReader reader, String from, String to, int contract) {
        List<Object[]> result = new ArrayList<>();
        try {
            System.out.println("Leyendo S05 del contrato " + contract + "...");

            String obisS05 = "0.0.98.2." + contract + ".255";

            // 1️⃣ Crear objeto Profile Generic con OBIS de S05
            GXDLMSProfileGeneric s05 = new GXDLMSProfileGeneric(obisS05);

            // 2️⃣ Leer Capture Objects (atributo 3)
            System.out.println("Leyendo capture objects de S05...");
            reader.read(s05, 3);

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

            // IMPORTANTE: Para Sagemcom/ZIV, ignoramos todo menos fecha y hora
            // Esto evita el "Access Violation" por campos que el medidor no entiende
            EnumSet<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.MILLISECOND,
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            );
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Leyendo S05 desde " + from + " hasta " + to);

            // 4️⃣ Leer filas por rango
            Object[] rows = reader.readRowsByRange(s05, start, end);

            if (rows != null && rows.length > 0) {
                List<Object[]> listaParaProcesar = new ArrayList<>();
                for (Object row : rows) {
                    listaParaProcesar.add((Object[]) row);
                }

                // Llamada al nuevo método
                imprimirFormatoFuncional(listaParaProcesar);
            } else {
                System.out.println("No hay registros en este rango.");
            }

        } catch (Exception e) {
            System.err.println("Error durante la lectura de S05: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public static void imprimirFormatoFuncional(List<Object[]> filas) {
        // Cabecera idéntica
        System.out.println("Cnt Id;Fecha;Contrato nº;Periodo Tarifario;Import Active abs;Export Active abs;Reactive quadrant I abs;Reactive quadrant II abs;Reactive quadrant III abs;Reactive quadrant IV abs");

        //String idContador = "SAG0221005043";

        for (Object[] fila : filas) {
            String fechaOriginal = fila[0].toString();
            // Formatear fecha para que tenga el .000W
            String fechaFormateada = formatearFechaS05(fechaOriginal);

            for (int p = 0; p <= 6; p++) {
                Object activa, export, r1, r2, r3, r4;

                // MAPEO ESPECIAL PARA COINCIDIR CON SOFTWARE FUNCIONAL
                if (p == 0) {
                    // El Periodo 0 del funcional muestra lo que hay en el índice 3 (P1)
                    activa = fila[3];
                    export = fila[10];
                    r1 = fila[17];
                    r2 = fila[24];
                    r3 = fila[31];
                    r4 = fila[38];
                } else if (p == 1) {
                    // El Periodo 1 del funcional muestra lo que hay en el índice 2 (P0/Total)
                    activa = fila[2];
                    export = fila[9];
                    r1 = fila[16];
                    r2 = fila[23];
                    r3 = fila[30];
                    r4 = fila[37];
                } else {
                    // Para los periodos 2 al 6, el mapeo es directo (índice + p)
                    activa = fila[2 + p];
                    export = fila[9 + p];
                    r1 = fila[16 + p];
                    r2 = fila[23 + p];
                    r3 = fila[30 + p];
                    // Evitar out of bounds en R4 si el array es de 43
                    r4 = (37 + p < fila.length) ? fila[37 + p] : 0;
                }

                System.out.printf("%s;1;%d;%s;%s;%s;%s;%s;%s%n",
                        fechaFormateada, p, activa, export, r1, r2, r3, r4);
            }
        }
    }

    private static String formatearFechaS05(String fecha) {
        // Convierte "22/1/26 0:00:00" a "2026/01/22 00:00:00.000W"
        // Esto es un ejemplo simple, ajusta según tu objeto Date/LocalDateTime
        try {
            String[] partes = fecha.split(" ");
            String[] dmy = partes[0].split("/");
            return String.format("20%s/%02d/%02d 00:00:00.000W", dmy[2], Integer.parseInt(dmy[1]), Integer.parseInt(dmy[0]));
        } catch (Exception e) {
            return fecha + ".000W";
        }
    }
}
