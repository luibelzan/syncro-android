package com.example.syncro.objects.pricing;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreMensualFila;
import com.example.syncro.utils.AppLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDateTime;
import gurux.dlms.GXSimpleEntry;
import gurux.dlms.enums.DataType;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class MonthlyBillingS04 {

    /**
     * Lee los cierres mensuales S04 de un contrato específico (1, 2 o 3).
     */
    public static ArrayList<CierreMensualFila> leerS04(GXDLMSReader reader,
                                                       String from, String to,
                                                       int contract) {
        ArrayList<CierreMensualFila> result = new ArrayList<>();

        try {
            AppLogger.i("MonthlyBilling",
                    "Ejecutando lectura de cierres S04 por rango (contrato " + contract + ")...");

            String obisS04 = "0.0.98.1." + contract + ".255";
            GXDLMSProfileGeneric s04 = new GXDLMSProfileGeneric(obisS04);

            // Necesario: Gurux requiere capture objects antes de readRowsByRange
            reader.read(s04, 3);

            // Columna dummy para medidores con 99 columnas
            if (s04.getCaptureObjects().size() == 99) {
                GXDLMSClock dummyClock = new GXDLMSClock("0.0.1.0.0.255");
                s04.getCaptureObjects().add(
                        new GXSimpleEntry<>(dummyClock, new GXDLMSCaptureObject(2, 0))
                );
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.US);

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 59);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end   = new GXDateTime(calEnd.getTime());

            EnumSet<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.MILLISECOND,
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            );
            start.setSkip(skips);
            end.setSkip(skips);

            AppLogger.i("MonthlyBilling",
                    "Solicitando S04: " + from + " al " + to);

            Object[] rows = reader.readRowsByRange(s04, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    if (fila.length < 100) continue;

                    // Saltar filas con fecha wildcard (FF FF...)
                    if (fila[0] instanceof byte[]) {
                        byte[] b = (byte[]) fila[0];
                        if (b.length == 12
                                && (b[0] & 0xFF) == 0xFF
                                && (b[1] & 0xFF) == 0xFF) continue;
                    }
                    if (fila[0] instanceof GXDateTime) {
                        Date f = ((GXDateTime) fila[0]).getValue();
                        if (f == null) continue;
                        Calendar c = Calendar.getInstance();
                        c.setTime(f);
                        if (c.get(Calendar.YEAR) <= 1970) continue;
                    }

                    String fechaInicio = formatearFechaS04(fila[0]);
                    String fechaFin    = formatearFechaS04(fila[1]);

                    for (int p = 0; p < 7; p++) {
                        int idxValMax   = 86 + (p * 2);
                        int idxFechaMax = 87 + (p * 2);

                        result.add(new CierreMensualFila(
                                fechaInicio,
                                fechaFin,
                                contract,
                                p,
                                parsearMaximetroValor(fila[idxValMax]),
                                parsearFechaMaximetro(fila[idxFechaMax]),
                                String.valueOf(fila[2  + p]),
                                String.valueOf(fila[9  + p]),
                                String.valueOf(fila[16 + p]),
                                String.valueOf(fila[23 + p]),
                                String.valueOf(fila[30 + p]),
                                String.valueOf(fila[37 + p]),
                                String.valueOf(fila[44 + p]),
                                String.valueOf(fila[51 + p]),
                                String.valueOf(fila[58 + p]),
                                String.valueOf(fila[65 + p]),
                                String.valueOf(fila[72 + p]),
                                String.valueOf(fila[79 + p])
                        ));
                    }
                }
            } else {
                AppLogger.i("MonthlyBilling",
                        "No hay registros en el rango para contrato " + contract);
            }

        } catch (Exception e) {
            AppLogger.e("MonthlyBilling",
                    "Error en lectura por rango S04 (contrato " + contract + "): "
                            + e.getMessage());
        }

        return result;
    }

    /**
     * Lee los cierres mensuales S04 de los contratos 1, 2 y 3 en secuencia.
     * Equivalente al "Todos los contratos" del spinner.
     */
    public static ArrayList<CierreMensualFila> leerS04Todos(GXDLMSReader reader,
                                                            String from,
                                                            String to) {
        ArrayList<CierreMensualFila> result = new ArrayList<>();
        for (int contrato = 1; contrato <= 3; contrato++) {
            AppLogger.i("MonthlyBilling",
                    "--- Leyendo contrato " + contrato + " de 3 ---");
            result.addAll(leerS04(reader, from, to, contrato));
        }
        return result;
    }

    // -------------------------------------------------------
    // leerS042 sin cambios (método alternativo de lectura completa)
    // -------------------------------------------------------

    public static ArrayList<CierreMensualFila> leerS042(GXDLMSReader reader,
                                                        String from, String to,
                                                        int contract) {
        ArrayList<CierreMensualFila> result = new ArrayList<>();

        try {
            System.out.println("Ejecutando lectura de cierres S04 completo...");

            String obisS04 = "0.0.98.1." + contract + ".255";
            GXDLMSProfileGeneric s04 = new GXDLMSProfileGeneric(obisS04);

            reader.read(s04, 3);
            System.out.println("Columnas S04: " + s04.getCaptureObjects().size());

            if (s04.getCaptureObjects().size() == 99) {
                GXDLMSClock dummyClock = new GXDLMSClock("0.0.1.0.0.255");
                s04.getCaptureObjects().add(
                        new GXSimpleEntry<>(dummyClock, new GXDLMSCaptureObject(2, 0))
                );
            }

            reader.read(s04, 2);
            Object[] todasLasFilas = s04.getBuffer();

            if (todasLasFilas == null || todasLasFilas.length == 0) {
                System.out.println("S04 vacío.");
                return result;
            }

            System.out.println("Filas totales en S04: " + todasLasFilas.length);

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
            calEnd.set(Calendar.SECOND, 59);
            calEnd.set(Calendar.MILLISECOND, 999);

            Date fechaDesde = calStart.getTime();
            Date fechaHasta = calEnd.getTime();

            for (Object row : todasLasFilas) {
                Object[] fila = (Object[]) row;
                if (fila == null || fila.length < 100) continue;

                Date fechaFila  = extraerFecha(fila[0]);
                Date fechaFila2 = extraerFecha(fila[1]);
                if (fechaFila == null) continue;

                System.out.println("Fila S04 fecha: " + fechaFila
                        + " | en rango: "
                        + (!fechaFila.before(fechaDesde) && !fechaFila.after(fechaHasta)));

                if (fechaFila2 == null
                        || fechaFila2.before(fechaDesde)
                        || fechaFila2.after(fechaHasta)) continue;

                String fechaInicio = formatearFechaS04(fila[0]);
                String fechaFin    = formatearFechaS04(fila[99]);

                for (int p = 0; p < 7; p++) {
                    int idxValMax   = 86 + (p * 2);
                    int idxFechaMax = 87 + (p * 2);

                    result.add(new CierreMensualFila(
                            fechaInicio, fechaFin, contract, p,
                            parsearMaximetroValor(fila[idxValMax]),
                            parsearFechaMaximetro(fila[idxFechaMax]),
                            String.valueOf(fila[2  + p]),
                            String.valueOf(fila[9  + p]),
                            String.valueOf(fila[16 + p]),
                            String.valueOf(fila[23 + p]),
                            String.valueOf(fila[30 + p]),
                            String.valueOf(fila[37 + p]),
                            String.valueOf(fila[44 + p]),
                            String.valueOf(fila[51 + p]),
                            String.valueOf(fila[58 + p]),
                            String.valueOf(fila[65 + p]),
                            String.valueOf(fila[72 + p]),
                            String.valueOf(fila[79 + p])
                    ));
                }
            }

            System.out.println("Filas S04 en rango: " + result.size() / 7);

        } catch (Exception e) {
            System.err.println("Error en lectura S04: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private static Date extraerFecha(Object raw) {
        try {
            if (raw instanceof GXDateTime) {
                return ((GXDateTime) raw).getValue();
            }
            if (raw instanceof byte[] && ((byte[]) raw).length == 12) {
                Object converted = GXDLMSClient.changeType((byte[]) raw, DataType.DATETIME);
                if (converted instanceof GXDateTime) {
                    return ((GXDateTime) converted).getValue();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String parsearMaximetroValor(Object raw) {
        try {
            if (raw instanceof GXDateTime) {
                return String.valueOf(((GXDateTime) raw).getValue().getTime() / 1000L);
            }
            if (raw instanceof byte[]) {
                byte[] b = (byte[]) raw;
                if (b.length == 12) {
                    Object converted = GXDLMSClient.changeType(b, DataType.DATETIME);
                    if (converted instanceof GXDateTime) {
                        long val = ((b[6] & 0xFF) << 24) | ((b[7] & 0xFF) << 16)
                                | ((b[8] & 0xFF) << 8)  |  (b[9] & 0xFF);
                        return String.valueOf(val);
                    }
                }
                if (b.length <= 4) {
                    return String.valueOf(GXDLMSClient.changeType(b, DataType.UINT32));
                }
                if (b.length == 8) {
                    return String.valueOf(GXDLMSClient.changeType(b, DataType.UINT64));
                }
            }
            return String.valueOf(raw);
        } catch (Exception e) {
            return "0";
        }
    }

    private static String parsearFechaMaximetro(Object raw) {
        try {
            if (raw instanceof GXDateTime) {
                GXDateTime dt = (GXDateTime) raw;
                Date fecha = dt.getValue();
                if (fecha == null) return "N/A";
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);
                if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        .format(fecha);
            }
            if (raw instanceof byte[]) {
                byte[] b = (byte[]) raw;
                if (b.length == 12) {
                    try {
                        Object converted = GXDLMSClient.changeType(b, DataType.DATETIME);
                        if (converted instanceof GXDateTime) {
                            Date fecha = ((GXDateTime) converted).getValue();
                            if (fecha == null) return "N/A";
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(fecha);
                            if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                            return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                                    .format(fecha);
                        }
                    } catch (Exception ignored) {}
                }
                if (b.length == 4) {
                    long segundos = 0;
                    for (byte x : b) segundos = (segundos << 8) | (x & 0xFF);
                    if (segundos == 0) return "N/A";
                    Date fecha = new Date(segundos * 1000L);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(fecha);
                    if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                    return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                            .format(fecha);
                }
            }
            return "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String formatearFechaS04(Object raw) {
        if (raw == null) return "N/A";

        if (raw instanceof GXDateTime) {
            GXDateTime dt = (GXDateTime) raw;
            Date fecha = dt.getValue();
            if (fecha == null) return "N/A";
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);
                if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        .format(fecha);
            } catch (Exception e) {
                return "N/A";
            }
        }

        if (raw instanceof byte[]) {
            byte[] b = (byte[]) raw;
            if (b.length == 12) {
                if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFF) return "N/A";
                try {
                    Object converted = GXDLMSClient.changeType(b, DataType.DATETIME);
                    if (converted instanceof GXDateTime) {
                        Date fecha = ((GXDateTime) converted).getValue();
                        if (fecha == null) return "N/A";
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(fecha);
                        if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                                .format(fecha);
                    }
                } catch (Exception e) {
                    return "N/A";
                }
            }
            return "N/A";
        }

        if (raw instanceof String) {
            String s = (String) raw;
            if (s.matches("\\d{4}/\\d{2}/\\d{2}.*")) return s;
            try {
                return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        .format(new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
                                .parse(s));
            } catch (Exception ignored) {}
            return s;
        }

        return "N/A";
    }
}