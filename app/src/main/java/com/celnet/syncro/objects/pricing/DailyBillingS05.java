package com.celnet.syncro.objects.pricing;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.cierres.CierreFila;
import com.celnet.syncro.utils.AppLogger;

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

            if (s05.getCaptureObjects().isEmpty()) {
                // Algunos contadores devuelven el atributo 3 como un array vacío
                // (mismo caso "ZIV" ya visto en las curvas de carga S44/S45/S46).
                // Mapeo manual según documentación oficial "Stored Billing Values
                // Profile" (0-0:98.2.c.255): Clock + 6 grupos de 7 registros
                // (y=1,2,5,6,7,8 -> A+,A-,QI,QII,QIII,QIV; n=0..6 -> total+6
                // periodos tarifarios), OBIS 1-0:y.8.(10*contrato+n).255.
                AppLogger.w("DailyBilling",
                        "Estructura vacía en S05 (contrato " + contract + "): aplicando mapeo manual según documentación.");

                addObject(s05, new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255"));

                int[] codigosEnergia = {1, 2, 5, 6, 7, 8}; // A+, A-, QI, QII, QIII, QIV
                for (int y : codigosEnergia) {
                    for (int n = 0; n <= 6; n++) {
                        int x = 10 * contract + n;
                        String obisRegistro = String.format(Locale.US, "1.0.%d.8.%d.255", y, x);
                        addObject(s05, new gurux.dlms.objects.GXDLMSRegister(obisRegistro));
                    }
                }
            } else {
                AppLogger.i("DailyBilling",
                        "Estructura S05 recibida del medidor (contrato " + contract + "). Columnas: "
                                + s05.getCaptureObjects().size());
            }

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
                                wattsAKilowatts(fila[1  + p]),
                                wattsAKilowatts(fila[8  + p]),
                                wattsAKilowatts(fila[15 + p]),
                                wattsAKilowatts(fila[22 + p]),
                                wattsAKilowatts(fila[29 + p]),
                                wattsAKilowatts(fila[36 + p])
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

    private static void addObject(GXDLMSProfileGeneric pg, gurux.dlms.objects.GXDLMSObject obj) {
        pg.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(obj,
                new gurux.dlms.objects.GXDLMSCaptureObject(2, 0)));
    }

    private static String wattsAKilowatts(Object valorWh) {
        try {
            double wh = Double.parseDouble(valorWh.toString());
            long kwh = Math.round(wh / 1000.0);
            return String.valueOf(kwh);
        } catch (Exception e) {
            return String.valueOf(valorWh);
        }
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