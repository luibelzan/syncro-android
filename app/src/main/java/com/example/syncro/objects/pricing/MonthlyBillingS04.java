package com.example.syncro.objects.pricing;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreFila;
import com.example.syncro.models.CierreMensualFila;

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
import gurux.dlms.internal.GXCommon;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class MonthlyBillingS04 {

    public static ArrayList<CierreMensualFila> leerS04(GXDLMSReader reader,
                                                       String from, String to,
                                                       int contract) {
        ArrayList<CierreMensualFila> result = new ArrayList<>();

        try {
            System.out.println("Ejecutando lectura de cierres S04 por rango...");

            String obisS04 = "0.0.98.1." + contract + ".255";
            GXDLMSProfileGeneric s04 = new GXDLMSProfileGeneric(obisS04);

            // 1. Leer objetos de captura (indispensable para saber qué columnas pedir)
            reader.read(s04, 3);

            // Lógica de columna dummy para medidores con 99 columnas (evita desfases de índice)
            if (s04.getCaptureObjects().size() == 99) {
                GXDLMSClock dummyClock = new GXDLMSClock("0.0.1.0.0.255");
                s04.getCaptureObjects().add(
                        new GXSimpleEntry<>(dummyClock, new GXDLMSCaptureObject(2, 0))
                );
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            // 2. Configurar el rango de tiempo con precisión de inicio de día y fin de día
            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23); // Ampliado a fin de día
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 59);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end = new GXDateTime(calEnd.getTime());

            // 3. Definir Skips (Ignorar milisegundos y estatus para mayor compatibilidad)
            EnumSet<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.MILLISECOND,
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            );
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando S04: " + from + " al " + to);

            // 4. Lectura por rango
            Object[] rows = reader.readRowsByRange(s04, start, end);

            if (rows != null && rows.length > 0) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    if (fila.length < 100) continue;

                    // ── Saltar filas con fecha de inicio wildcard (FF FF...) ──────────
                    if (fila[0] instanceof byte[]) {
                        byte[] b = (byte[]) fila[0];
                        if (b.length == 12 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFF) {
                            continue; // fila sin fecha de inicio → ignorar
                        }
                    }
                    // Si es GXDateTime con año <= 1970 también saltar
                    if (fila[0] instanceof GXDateTime) {
                        java.util.Date f = ((GXDateTime) fila[0]).getValue();
                        if (f == null) continue;
                        Calendar c = Calendar.getInstance();
                        c.setTime(f);
                        if (c.get(Calendar.YEAR) <= 1970) continue;
                    }
                    // ─────────────────────────────────────────────────────────────────

                    String fechaInicio = formatearFechaS04(fila[0]);
                    String fechaFin    = formatearFechaS04(fila[1]);

                    for (int p = 0; p < 7; p++) {
                        int idxValMax   = 86 + (p * 2);  // ← era 87, ahora 86
                        int idxFechaMax = 87 + (p * 2);  // ← era 86, ahora 87

                        Object rawVal = fila[86];
                        if (rawVal instanceof byte[]) {
                            byte[] b = (byte[]) rawVal;
                            System.out.println("VALOR MAX bytes(" + b.length + "): " + GXCommon.toHex(b, true));
                        }

                        // Los índices se mantienen según tu estructura de 100 columnas
                        result.add(new CierreMensualFila(
                                fechaInicio,
                                fechaFin,
                                contract,
                                p,
                                parsearMaximetroValor(fila[idxValMax]),   // Valor numérico
                                parsearFechaMaximetro(fila[idxFechaMax]),  // Fecha captura
                                String.valueOf(fila[2  + p]), // Absolutos
                                String.valueOf(fila[9  + p]),
                                String.valueOf(fila[16 + p]), // R1-R4 Abs
                                String.valueOf(fila[23 + p]),
                                String.valueOf(fila[30 + p]),
                                String.valueOf(fila[37 + p]),
                                String.valueOf(fila[44 + p]), // Incrementales
                                String.valueOf(fila[51 + p]),
                                String.valueOf(fila[58 + p]), // R1-R4 Inc
                                String.valueOf(fila[65 + p]),
                                String.valueOf(fila[72 + p]),
                                String.valueOf(fila[79 + p])
                        ));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error en lectura por rango S04: " + e.getMessage());
        }

        return result;
    }

    public static ArrayList<CierreMensualFila> leerS042(GXDLMSReader reader,
                                                       String from, String to,
                                                       int contract) {
        ArrayList<CierreMensualFila> result = new ArrayList<>();

        try {
            System.out.println("Ejecutando lectura de cierres S04 completo...");

            String obisS04 = "0.0.98.1." + contract + ".255";
            GXDLMSProfileGeneric s04 = new GXDLMSProfileGeneric(obisS04);

            // 1. Leer estructura de columnas (atributo 3)
            reader.read(s04, 3);
            System.out.println("Columnas S04: " + s04.getCaptureObjects().size());

            if (s04.getCaptureObjects().size() == 99) {
                GXDLMSClock dummyClock = new GXDLMSClock("0.0.1.0.0.255");
                s04.getCaptureObjects().add(
                        new GXSimpleEntry<>(dummyClock, new GXDLMSCaptureObject(2, 0))
                );
            }

            // 2. Leer el buffer COMPLETO sin selector de rango (atributo 2)
            // Esto evita el "Read-Write denied" que da el medidor con readRowsByRange
            reader.read(s04, 2);
            Object[] todasLasFilas = s04.getBuffer();

            if (todasLasFilas == null || todasLasFilas.length == 0) {
                System.out.println("S04 vacío.");
                return result;
            }

            System.out.println("Filas totales en S04: " + todasLasFilas.length);

            // 3. Filtro por fechas en cliente
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

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

            // 4. Procesar filas filtradas
            for (Object row : todasLasFilas) {
                Object[] fila = (Object[]) row;
                if (fila == null || fila.length < 100) continue;

                // Extraer y verificar fecha de la primera columna
                Date fechaFila = extraerFecha(fila[0]);
                Date fechaFila2 = extraerFecha(fila[1]);
                if (fechaFila == null) continue;

                System.out.println("Fila S04 fecha: " + fechaFila
                        + " | en rango: " + (!fechaFila.before(fechaDesde) && !fechaFila.after(fechaHasta)));

                if (fechaFila2.before(fechaDesde) || fechaFila2.after(fechaHasta)) continue;

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

// -------------------------------------------------------
// Helpers para el maxímetro
// -------------------------------------------------------

    private static String parsearMaximetroValor(Object raw) {
        try {
            if (raw instanceof GXDateTime) {
                return String.valueOf(((GXDateTime) raw).getValue().getTime() / 1000L);
            }
            if (raw instanceof byte[]) {
                byte[] b = (byte[]) raw;

                if (b.length == 12) {
                    // El medidor envía el valor como DLMS DateTime — extraemos los segundos
                    // desde medianoche como valor de demanda, o usamos epoch como número
                    Object converted = GXDLMSClient.changeType(b, DataType.DATETIME);
                    if (converted instanceof GXDateTime) {
                        GXDateTime dt = (GXDateTime) converted;
                        java.util.Date fecha = dt.getValue();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(fecha);
                        // El valor real de la demanda máxima suele estar en los bytes 6-9 (UINT32)
                        // Intentamos leerlo directamente de los bytes del payload
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
                java.util.Date fecha = dt.getValue();
                if (fecha == null) return "N/A";
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);
                if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                return sdf.format(fecha);
            }
            if (raw instanceof byte[]) {
                byte[] b = (byte[]) raw;
                // Attr 5 (capture_time) del Extended Register son SIEMPRE 12 bytes DLMS DateTime
                if (b.length == 12) {
                    try {
                        Object converted = GXDLMSClient.changeType(b, DataType.DATETIME);
                        if (converted instanceof GXDateTime) {
                            GXDateTime dt = (GXDateTime) converted;
                            java.util.Date fecha = dt.getValue();
                            if (fecha == null) return "N/A";
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(fecha);
                            if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                            return sdf.format(fecha);
                        }
                    } catch (Exception ignored) {}
                }
                // Si no son 12 bytes, intentamos como Date (4 bytes = epoch DLMS relativo)
                if (b.length == 4) {
                    long segundos = 0;
                    for (byte x : b) segundos = (segundos << 8) | (x & 0xFF);
                    if (segundos == 0) return "N/A";
                    java.util.Date fecha = new java.util.Date(segundos * 1000L);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(fecha);
                    if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                    return sdf.format(fecha);
                }
                return "N/A";
            }
            return "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String formatearFechaS04(Object raw) {
        if (raw == null) return "N/A";

        // Caso 1: ya es GXDateTime
        if (raw instanceof GXDateTime) {
            GXDateTime dt = (GXDateTime) raw;
            java.util.Date fecha = dt.getValue();
            if (fecha == null) return "N/A";
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);
                if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(fecha);
            } catch (Exception e) {
                return "N/A";
            }
        }

        // Caso 2: byte[] de 12 bytes (DLMS DateTime)
        if (raw instanceof byte[]) {
            byte[] b = (byte[]) raw;
            if (b.length == 12) {
                // Detectar wildcard DLMS: si los bytes de año (0-1) son FF FF → sin fecha
                if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFF) {
                    return "N/A";
                }
                try {
                    Object converted = GXDLMSClient.changeType(b, DataType.DATETIME);
                    if (converted instanceof GXDateTime) {
                        java.util.Date fecha = ((GXDateTime) converted).getValue();
                        if (fecha == null) return "N/A";
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(fecha);
                        if (cal.get(Calendar.YEAR) <= 1970) return "N/A";
                        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(fecha);
                    }
                } catch (Exception e) {
                    return "N/A";
                }
            }
            return "N/A";
        }

        // Caso 3: String
        if (raw instanceof String) {
            String s = (String) raw;
            if (s.matches("\\d{4}/\\d{2}/\\d{2}.*")) return s;
            try {
                SimpleDateFormat sdfIn  = new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault());
                SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
                return sdfOut.format(sdfIn.parse(s));
            } catch (Exception ignored) {}
            return s;
        }

        return "N/A";
    }
}
