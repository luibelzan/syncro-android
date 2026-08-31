package com.celnet.syncro.objects.loadProfiles;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.curvas.CurvaCorrienteFila;
import com.celnet.syncro.models.curvas.CurvaEnergiaFaseFila;
import com.celnet.syncro.models.curvas.CurvaFila;
import com.celnet.syncro.models.curvas.CurvaVoltajeFila;
import com.celnet.syncro.models.curvas.TipoCurva;
import com.celnet.syncro.utils.AppLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class LoadProfileReader {

    public static ArrayList<CurvaFila> leerCurvaCarga(GXDLMSReader reader, String fechaInicio, String fechaFin) throws Exception {
        AppLogger.w("Syncro", "Leyendo Curvas Horarias");
        ArrayList<CurvaFila> resultados = new ArrayList<>();

            GXDLMSProfileGeneric lp1 = new GXDLMSProfileGeneric("1.0.99.1.0.255");

            reader.read(lp1, 3);

            if (lp1.getCaptureObjects().isEmpty()) {
                AppLogger.w("LoadProfile", "Estructura vacía (ZIV detectado): Aplicando mapeo manual...");
                addObject(lp1, new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSData("0.0.96.10.2.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.1.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.2.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.3.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.4.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.5.8.0.255"));
                addObject(lp1, new gurux.dlms.objects.GXDLMSRegister("1.0.6.8.0.255"));
            } else {
                AppLogger.i("LoadProfile", "Estructura recibida del medidor. Columnas: " + lp1.getCaptureObjects().size());
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(fechaInicio));
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(fechaFin));
            calEnd.set(Calendar.HOUR_OF_DAY, 0);
            calEnd.set(Calendar.MINUTE, 0);
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

            AppLogger.i("LoadProfile", "Solicitando rango: " + fechaInicio + " a " + fechaFin);

            Object[] rows = reader.readRowsByRange(lp1, start, end);

            if (rows != null) {
                AppLogger.i("LoadProfile", "Filas recibidas: " + rows.length);
                for (Object row : rows) {
                    Object[] cols = (Object[]) row;

                    String fechaHora = cols[0].toString();
                    String bc = cols.length > 1 ? cols[1].toString() : "-";
                    String ai = cols.length > 2 ? cols[2].toString() : "-";
                    String ae = cols.length > 3 ? cols[3].toString() : "-";
                    String r1 = cols.length > 4 ? cols[4].toString() : "-";
                    String r2 = cols.length > 4 ? cols[5].toString() : "-";
                    String r3 = cols.length > 4 ? cols[6].toString() : "-";
                    String r4 = cols.length > 4 ? cols[7].toString() : "-";

                    resultados.add(new CurvaFila(fechaHora, bc, ai, ae, r1, r2, r3, r4));
                }
            } else {
                AppLogger.w("LoadProfile", "Buffer vacío: no se recibieron filas.");
            }

        return resultados;
    }

    private static void addObject(GXDLMSProfileGeneric pg, gurux.dlms.objects.GXDLMSObject obj) {
        pg.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(obj,
                new gurux.dlms.objects.GXDLMSCaptureObject(2, 0)));
    }

    public static ArrayList<CurvaVoltajeFila> leerCurvaVoltaje(GXDLMSReader reader, String fechaInicio, String fechaFin) throws Exception {
        AppLogger.w("Syncro", "Leyendo LP4 - Tensiones máximas, medias y mínimas");
        ArrayList<CurvaVoltajeFila> resultados = new ArrayList<>();

        GXDLMSProfileGeneric lp4 = new GXDLMSProfileGeneric(TipoCurva.VOLTAGE_S44.obis);
        try {
            reader.read(lp4, 3);
        } catch (gurux.dlms.GXDLMSException e) {
            AppLogger.e("LoadProfile", "El contador no soporta LP4 (voltajes): " + e.getMessage());
            throw new Exception("Este contador no dispone de la curva de tensiones (S44). "
                    + "Puede que este modelo o su configuración actual no incluya este objeto.", e);
        }

        if (lp4.getCaptureObjects().isEmpty()) {
            throw new Exception("Estructura vacía en LP4: falta mapeo manual de columnas.");
        }
        AppLogger.i("LoadProfile", "Estructura LP4 recibida del medidor. Columnas: " + lp4.getCaptureObjects().size());

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(formatter.parse(fechaInicio));
        calStart.set(Calendar.HOUR_OF_DAY, 0); calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0); calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(formatter.parse(fechaFin));
        calEnd.set(Calendar.HOUR_OF_DAY, 0); calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0); calEnd.set(Calendar.MILLISECOND, 0);

        GXDateTime start = new GXDateTime(calStart.getTime());
        GXDateTime end = new GXDateTime(calEnd.getTime());
        EnumSet<DateTimeSkips> skips = EnumSet.of(DateTimeSkips.MILLISECOND, DateTimeSkips.DEVIATION, DateTimeSkips.STATUS);
        start.setSkip(skips);
        end.setSkip(skips);

        Object[] rows = reader.readRowsByRange(lp4, start, end);

        if (rows != null) {
            AppLogger.i("LoadProfile", "LP4 - Filas recibidas: " + rows.length);
            for (Object row : rows) {
                Object[] cols = (Object[]) row;

                String fechaHora = cols[0] != null ? cols[0].toString() : "-";
                String status    = formatStatus(cols.length > 1 ? cols[1] : null);
                String maxL1 = formatRegistro(cols.length > 2 ? cols[2] : null);
                String maxL2 = formatRegistro(cols.length > 3 ? cols[3] : null);
                String maxL3 = formatRegistro(cols.length > 4 ? cols[4] : null);
                String avL1  = formatRegistro(cols.length > 5 ? cols[5] : null);
                String avL2  = formatRegistro(cols.length > 6 ? cols[6] : null);
                String avL3  = formatRegistro(cols.length > 7 ? cols[7] : null);
                String minL1 = formatRegistro(cols.length > 8 ? cols[8] : null);
                String minL2 = formatRegistro(cols.length > 9 ? cols[9] : null);
                String minL3 = formatRegistro(cols.length > 10 ? cols[10] : null);

                resultados.add(new CurvaVoltajeFila(fechaHora, status,
                        maxL1, maxL2, maxL3, avL1, avL2, avL3, minL1, minL2, minL3));
            }
        }

        return resultados;
    }

    /**
     * Formatea un valor de registro de tensión. El contador puede devolver null
     * (NULL-DATA) para fases sin medida (p.ej. contadores monofásicos con L2/L3
     * sin cablear) — se muestra como "0,0" igual que el software de referencia.
     *
     * ⚠️ Escala asumida: /10 (típica de registros de tensión con scaler -1).
     * No he podido confirmar el scaler real (atributo 3 del Register) con este log;
     * si algún valor no cuadra con lo que ves en el contador, dímelo y añado
     * la lectura del scaler real en vez de asumir /10.
     */
    private static String formatRegistro(Object value) {
        if (value == null) {
            return "0,0";
        }
        if (value instanceof Number) {
            double escalado = ((Number) value).doubleValue() / 10.0;
            return String.format(java.util.Locale.forLanguageTag("es-ES"), "%.1f", escalado);
        }
        return value.toString();
    }

    private static String formatStatus(Object value) {
        if (value == null) {
            return "(00)";
        }
        if (value instanceof Number) {
            return String.format("(%02X)", ((Number) value).intValue());
        }
        return "(" + value + ")";
    }

    public static ArrayList<CurvaCorrienteFila> leerCurvaCorriente(GXDLMSReader reader, String fechaInicio, String fechaFin) throws Exception {
        AppLogger.w("Syncro", "Leyendo LP5 - Corrientes máximas, medias y mínimas");
        ArrayList<CurvaCorrienteFila> resultados = new ArrayList<>();

        GXDLMSProfileGeneric lp5 = new GXDLMSProfileGeneric(TipoCurva.CURRENT_S45.obis);

        try {
            reader.read(lp5, 3);
        } catch (Exception e) {
            AppLogger.e("LoadProfile", "El contador no soporta LP5 (corrientes): " + e.getMessage());
            throw new Exception("Este contador no dispone de la curva de corrientes (S45). "
                    + "Puede que este modelo o su configuración actual no incluya este objeto.", e);
        }

        if (lp5.getCaptureObjects().isEmpty()) {
            AppLogger.w("LoadProfile", "Estructura vacía en LP5 (posible caso ZIV): aplicando mapeo manual confirmado por documentación.");
            addObject(lp5, new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255"));
            addObject(lp5, new gurux.dlms.objects.GXDLMSData("0.0.96.10.9.255"));
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.31.26.0.255")); // Max IL1
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.51.26.0.255")); // Max IL2
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.71.26.0.255")); // Max IL3
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.91.26.0.255")); // Max IN
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.31.24.0.255")); // Av IL1
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.51.24.0.255")); // Av IL2
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.71.24.0.255")); // Av IL3
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.91.24.0.255")); // Av IN
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.31.23.0.255")); // Min IL1
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.51.23.0.255")); // Min IL2
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.71.23.0.255")); // Min IL3
            addObject(lp5, new gurux.dlms.objects.GXDLMSRegister("1.0.91.23.0.255")); // Min IN
        } else {
            AppLogger.i("LoadProfile", "Estructura LP5 recibida del medidor. Columnas: " + lp5.getCaptureObjects().size());
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(formatter.parse(fechaInicio));
        calStart.set(Calendar.HOUR_OF_DAY, 0); calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0); calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(formatter.parse(fechaFin));
        calEnd.set(Calendar.HOUR_OF_DAY, 0); calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0); calEnd.set(Calendar.MILLISECOND, 0);

        GXDateTime start = new GXDateTime(calStart.getTime());
        GXDateTime end = new GXDateTime(calEnd.getTime());
        EnumSet<DateTimeSkips> skips = EnumSet.of(DateTimeSkips.MILLISECOND, DateTimeSkips.DEVIATION, DateTimeSkips.STATUS);
        start.setSkip(skips);
        end.setSkip(skips);

        Object[] rows = reader.readRowsByRange(lp5, start, end);

        if (rows != null) {
            AppLogger.i("LoadProfile", "LP5 - Filas recibidas: " + rows.length);
            for (Object row : rows) {
                Object[] cols = (Object[]) row;

                String fechaHora = cols[0] != null ? cols[0].toString() : "-";
                String status = formatStatus(cols.length > 1 ? cols[1] : null);
                String maxL1 = formatRegistro(cols.length > 2  ? cols[2]  : null);
                String maxL2 = formatRegistro(cols.length > 3  ? cols[3]  : null);
                String maxL3 = formatRegistro(cols.length > 4  ? cols[4]  : null);
                String maxN  = formatRegistro(cols.length > 5  ? cols[5]  : null);
                String avL1  = formatRegistro(cols.length > 6  ? cols[6]  : null);
                String avL2  = formatRegistro(cols.length > 7  ? cols[7]  : null);
                String avL3  = formatRegistro(cols.length > 8  ? cols[8]  : null);
                String avN   = formatRegistro(cols.length > 9  ? cols[9]  : null);
                String minL1 = formatRegistro(cols.length > 10 ? cols[10] : null);
                String minL2 = formatRegistro(cols.length > 11 ? cols[11] : null);
                String minL3 = formatRegistro(cols.length > 12 ? cols[12] : null);
                String minN  = formatRegistro(cols.length > 13 ? cols[13] : null);

                resultados.add(new CurvaCorrienteFila(fechaHora, status,
                        maxL1, maxL2, maxL3, maxN, avL1, avL2, avL3, avN, minL1, minL2, minL3, minN));
            }
        }

        return resultados;
    }

    public static ArrayList<CurvaEnergiaFaseFila> leerCurvaEnergiaPorFase(GXDLMSReader reader, String fechaInicio, String fechaFin) throws Exception {
        AppLogger.w("Syncro", "Leyendo LP6 - Energías por fase");
        ArrayList<CurvaEnergiaFaseFila> resultados = new ArrayList<>();

        GXDLMSProfileGeneric lp6 = new GXDLMSProfileGeneric(TipoCurva.ENERGY_PHASE_S43.obis);

        try {
            reader.read(lp6, 3);
        } catch (Exception e) {
            AppLogger.e("LoadProfile", "El contador no soporta LP6 (energías por fase): " + e.getMessage());
            throw new Exception("Este contador no dispone del informe S43. "
                    + "Este informe solo está disponible en contadores con Companion 2.0 Trifásicos.", e);
        }

        if (lp6.getCaptureObjects().isEmpty()) {
            AppLogger.w("LoadProfile", "Estructura vacía en LP6: aplicando mapeo manual confirmado por documentación.");
            addObject(lp6, new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255"));
            addObject(lp6, new gurux.dlms.objects.GXDLMSData("0.0.96.10.14.255"));
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.21.29.0.255")); // EA+ R
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.22.29.0.255")); // EA- R
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.25.29.0.255")); // Q1 R
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.26.29.0.255")); // Q2 R
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.27.29.0.255")); // Q3 R
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.28.29.0.255")); // Q4 R
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.41.29.0.255")); // EA+ S
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.42.29.0.255")); // EA- S
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.45.29.0.255")); // Q1 S
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.46.29.0.255")); // Q2 S
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.47.29.0.255")); // Q3 S
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.48.29.0.255")); // Q4 S
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.61.29.0.255")); // EA+ T
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.62.29.0.255")); // EA- T
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.65.29.0.255")); // Q1 T
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.66.29.0.255")); // Q2 T
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.67.29.0.255")); // Q3 T
            addObject(lp6, new gurux.dlms.objects.GXDLMSRegister("1.0.68.29.0.255")); // Q4 T
        } else {
            AppLogger.i("LoadProfile", "Estructura LP6 recibida del medidor. Columnas: " + lp6.getCaptureObjects().size());
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(formatter.parse(fechaInicio));
        calStart.set(Calendar.HOUR_OF_DAY, 0); calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0); calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(formatter.parse(fechaFin));
        calEnd.set(Calendar.HOUR_OF_DAY, 0); calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0); calEnd.set(Calendar.MILLISECOND, 0);

        GXDateTime start = new GXDateTime(calStart.getTime());
        GXDateTime end = new GXDateTime(calEnd.getTime());
        EnumSet<DateTimeSkips> skips = EnumSet.of(DateTimeSkips.MILLISECOND, DateTimeSkips.DEVIATION, DateTimeSkips.STATUS);
        start.setSkip(skips);
        end.setSkip(skips);

        Object[] rows = reader.readRowsByRange(lp6, start, end);

        if (rows != null) {
            AppLogger.i("LoadProfile", "LP6 - Filas recibidas: " + rows.length);
            for (Object row : rows) {
                Object[] cols = (Object[]) row;

                String fechaHora = cols[0] != null ? cols[0].toString() : "-";
                String status = formatStatus(cols.length > 1 ? cols[1] : null);

                String[] valores = new String[18];
                for (int i = 0; i < 18; i++) {
                    Object v = cols.length > (i + 2) ? cols[i + 2] : null;
                    valores[i] = formatRegistro(v);
                }

                resultados.add(new CurvaEnergiaFaseFila(fechaHora, status,
                        valores[0], valores[1], valores[2], valores[3], valores[4], valores[5],   // R
                        valores[6], valores[7], valores[8], valores[9], valores[10], valores[11], // S
                        valores[12], valores[13], valores[14], valores[15], valores[16], valores[17])); // T
            }
        }

        return resultados;
    }
}