package com.example.syncro.objects.pricing;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreFila;
import com.example.syncro.models.CierreMensualFila;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;

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
                    DateTimeSkips.STATUS,
                    DateTimeSkips.SECOND // A veces ayuda omitir segundos en cierres mensuales
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

                    // Extraer fechas de la fila
                    String fechaInicio = formatearFechaS04(fila[0].toString());
                    String fechaFin    = formatearFechaS04(fila[99].toString());

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

    private static String formatearFechaS04(String fecha) {
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

    public static ArrayList<CierreMensualFila> leerS042(GXDLMSReader reader,
                                                       String from, String to,
                                                       int contract) {
        ArrayList<CierreMensualFila> result = new ArrayList<>();

        try {
            String obisS04 = "0.0.98.1." + contract + ".255";
            GXDLMSProfileGeneric s04 = new GXDLMSProfileGeneric(obisS04);

            // 1. SINCRONIZACIÓN DE COLUMNAS (Crucial para evitar el error de columnas)
            // Leemos la definición de captura (Atributo 3)
            reader.read(s04, 3);

            // 2. CONFIGURACIÓN DEL FILTRO DE TIEMPO
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end = new GXDateTime(calEnd.getTime());

            // 3. LECTURA POR RANGO (Usando el método de alto nivel que sí existe)
            // Gurux gestiona internamente la selección de columnas si ya leyó el atributo 3
            Object[] rows = reader.readRowsByRange(s04, start, end);

            if (rows != null) {
                for (Object row : rows) {
                    Object[] fila = (Object[]) row;

                    // AJUSTE DINÁMICO DE ÍNDICES (Basado en tu log de 99 columnas)
                    // Si la fila tiene menos de 100, aplicamos el desplazamiento de -1
                    int offset = (fila.length < 100) ? -1 : 0;

                    String fechaInicio = extraerDato(fila[0]);
                    String fechaFin    = extraerDato(fila[fila.length - 1]);

                    for (int p = 0; p < 7; p++) {
                        // Índices según el log funcional
                        int idxFechaMax = 86 + (p * 2) + offset;
                        int idxValMax   = 87 + (p * 2) + offset;

                        if (idxValMax >= fila.length) break;

                        result.add(new CierreMensualFila(
                                fechaInicio,
                                fechaFin,
                                contract,
                                p,
                                extraerDato(fila[idxValMax]),
                                extraerDato(fila[idxFechaMax]), // Aquí obtendrás la fecha del maxímetro
                                extraerDato(fila[2  + p]),
                                extraerDato(fila[9  + p]),
                                extraerDato(fila[16 + p]),
                                extraerDato(fila[23 + p]),
                                extraerDato(fila[30 + p]),
                                extraerDato(fila[37 + p]),
                                extraerDato(fila[44 + p]),
                                extraerDato(fila[51 + p]),
                                extraerDato(fila[58 + p]),
                                extraerDato(fila[65 + p]),
                                extraerDato(fila[72 + p]),
                                extraerDato(fila[79 + p])
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error en lectura S04: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private static String extraerDato(Object obj) {
        if (obj == null) return "0";

        // Si la librería ya lo convirtió a GXDateTime
        if (obj instanceof GXDateTime) {
            return obj.toString();
        }

        // Si viene como array de bytes (Octet-String)
        if (obj instanceof byte[]) {
            byte[] b = (byte[]) obj;
            try {
                // Si tiene 12 bytes, es el formato de fecha DLMS que viste en el log funcional
                if (b.length == 12) {
                    return GXDLMSClient.changeType(b, DataType.DATETIME).toString();
                }
                // Si no, es un valor numérico (como el valor del maxímetro)
                return String.valueOf(GXDLMSClient.changeType(b, DataType.UINT32));
            } catch (Exception e) {
                return GXCommon.toHex(b, false);
            }
        }

        return String.valueOf(obj);
    }
}
