package com.example.syncro.objects.pricing;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreFila;
import com.example.syncro.utils.AppLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Locale;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class DailyBillingS05 {

    /**
     * Lee los cierres diarios S05 de un contrato específico (1, 2 o 3).
     */
    public static ArrayList<CierreFila> leerS05(GXDLMSReader reader,
                                                String from,
                                                String to,
                                                int contract) {
        ArrayList<CierreFila> result = new ArrayList<>();

        try {
            AppLogger.i("DailyBilling", "Leyendo S05 del contrato " + contract + "...");

            String obisS05 = "0.0.98.2." + contract + ".255";
            GXDLMSProfileGeneric s05 = new GXDLMSProfileGeneric(obisS05);

            // Necesario: Gurux requiere capture objects antes de readRowsByRange
            reader.read(s05, 3);

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.US);

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end   = new GXDateTime(calEnd.getTime());

            EnumSet<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.MILLISECOND,
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            );
            start.setSkip(skips);
            end.setSkip(skips);

            AppLogger.i("DailyBilling", "Leyendo S05 desde " + from + " hasta " + to);

            Object[] rows = reader.readRowsByRange(s05, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;

                    String fechaFormateada = formatearFechaS05(fila[0].toString());

                    for (int p = 0; p <= 6; p++) {
                        result.add(new CierreFila(
                                fechaFormateada,
                                contract,
                                p,
                                String.valueOf(fila[1  + p]),
                                String.valueOf(fila[8  + p]),
                                String.valueOf(fila[15 + p]),
                                String.valueOf(fila[22 + p]),
                                String.valueOf(fila[29 + p]),
                                String.valueOf(fila[36 + p])
                        ));
                    }
                }
            } else {
                AppLogger.i("DailyBilling",
                        "No hay registros en el rango para contrato " + contract);
            }

        } catch (Exception e) {
            AppLogger.e("DailyBilling",
                    "Fallo en la petición (contrato " + contract + "): " + e.getMessage());
        }

        return result;
    }

    /**
     * Lee los cierres diarios S05 de los contratos 1, 2 y 3 en secuencia
     * y devuelve todos los resultados combinados.
     * Equivalente al "Todos los contratos" del spinner.
     */
    public static ArrayList<CierreFila> leerS05Todos(GXDLMSReader reader,
                                                     String from,
                                                     String to) {
        ArrayList<CierreFila> result = new ArrayList<>();
        for (int contrato = 1; contrato <= 3; contrato++) {
            AppLogger.i("DailyBilling",
                    "--- Leyendo contrato " + contrato + " de 3 ---");
            result.addAll(leerS05(reader, from, to, contrato));
        }
        return result;
    }

    // Formateo de fecha: "01/06/26 00:00:00" → "2026/06/01 00:00:00.000W"
    private static String formatearFechaS05(String fecha) {
        try {
            String[] partes = fecha.split(" ");
            String[] dmy    = partes[0].split("/");

            return String.format(Locale.US,
                    "20%s/%02d/%02d 00:00:00.000W",
                    dmy[2],
                    Integer.parseInt(dmy[1]),
                    Integer.parseInt(dmy[0]));

        } catch (Exception e) {
            return fecha + ".000W";
        }
    }
}