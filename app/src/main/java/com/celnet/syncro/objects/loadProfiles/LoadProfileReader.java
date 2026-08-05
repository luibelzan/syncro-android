package com.celnet.syncro.objects.loadProfiles;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.CurvaFila;
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
}