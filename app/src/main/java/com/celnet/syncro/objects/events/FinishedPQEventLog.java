package com.celnet.syncro.objects.events;

import android.content.Context;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.events.EventDescription;
import com.celnet.syncro.models.events.EventFila;
import com.celnet.syncro.models.events.EventInfo;
import com.celnet.syncro.utils.AppLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class FinishedPQEventLog {

    private static final String TAG = "FinishedPQEventLog";
    private static Map<String, EventInfo> eventMap = new HashMap<>();

    public static List<EventFila> readFinishedPQEventLog(Context context, GXDLMSReader reader, String from, String to) {
        List<EventFila> result = new ArrayList<>();
        try {
            String obisFinishedPQ = "0.0.99.98.9.255";
            GXDLMSProfileGeneric finishedPQLog = new GXDLMSProfileGeneric(obisFinishedPQ);

            AppLogger.i(TAG, "Leyendo estructura de Finished PQ Event Log...");
            reader.read(finishedPQLog, 3);

            if (finishedPQLog.getCaptureObjects() == null || finishedPQLog.getCaptureObjects().isEmpty()) {
                AppLogger.i(TAG, "ZIV detectado o estructura vacía. Aplicando plantilla manual...");

                gurux.dlms.objects.GXDLMSClock clock = new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
                gurux.dlms.objects.GXDLMSData eventCode = new gurux.dlms.objects.GXDLMSData("0.0.96.11.9.255");

                gurux.dlms.objects.GXDLMSCaptureObject capClock = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);
                gurux.dlms.objects.GXDLMSCaptureObject capEvent = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);

                finishedPQLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(clock, capClock));
                finishedPQLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(eventCode, capEvent));
                // NOTA: la plantilla manual (fallback ZIV) solo cubre clock +
                // event code (2 columnas). Sin los capture_objects reales del
                // contador no se pueden inferir las 5 columnas adicionales
                // (timestamp_begin + 4 valores de tensión), así que en ese
                // caso D1/D2 quedarán vacíos (fila.length == 2).
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(formatter.parse(from));
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(formatter.parse(to));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 45);
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);

            GXDateTime start = new GXDateTime(calStart.getTime());
            GXDateTime end = new GXDateTime(calEnd.getTime());

            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION);
            skips.add(DateTimeSkips.STATUS);

            start.setSkip(skips);
            end.setSkip(skips);

            AppLogger.i(TAG, "Solicitando eventos PQ finalizados...");
            Object[] rows = reader.readRowsByRange(finishedPQLog, start, end);

            if (rows != null && rows.length > 0) {
                AppLogger.i(TAG, "Eventos encontrados: " + rows.length);

                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    String fecha = fila[0].toString();
                    int id = Integer.parseInt(fila[1].toString());
                    EventDescription info = getEventDescription(context, id, 3);

                    // Según documentación del fabricante (Finished Power
                    // Quality Event Log, 0-0:99.98.9.255):
                    //   col 2 = Timestamp begin of event               → D1
                    //   col 3-6 = tensión finalizada fase R/S/T/promedio → D2
                    // Solo la fase que originó el evento lleva valor real;
                    // el resto llegan como "null" (ya gestionado por
                    // EventExtraDataFormatter.format).
                    String d1 = null;
                    String d2 = null;
                    if (fila.length > 2) {
                        d1 = EventExtraDataFormatter.format(fila[2]);
                    }
                    if (fila.length > 6) {
                        d2 = EventExtraDataFormatter.formatList(fila[3], fila[4], fila[5], fila[6]);
                    }

                    EventFila evento = new EventFila(
                            fecha,
                            id,
                            info.description,
                            info.grp,
                            d1,
                            d2
                    );

                    result.add(evento);
                }

            } else {
                AppLogger.i(TAG, "No hay registros de eventos PQ finalizados.");
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Error en Finished PQ Log: " + e.getMessage());
        }
        return result;
    }

    private static void loadEvents(Context context) {
        if (!eventMap.isEmpty()) return;

        try {
            InputStream is = context.getAssets().open("events_table.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            JSONArray array = new JSONArray(json.toString());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                EventInfo info = new EventInfo();
                info.grp = obj.getInt("grp");
                info.cod = obj.getInt("cod");
                info.desc = obj.getString("desc");
                String key = info.grp + "-" + info.cod;

                eventMap.put(key, info);
            }

            AppLogger.i(TAG, "Tabla de eventos cargada: " + eventMap.size() + " entradas.");

        } catch (Exception e) {
            AppLogger.e(TAG, "Error cargando events_table.json: " + e.getMessage());
        }
    }

    private static EventDescription getEventDescription(Context context, int code, int group) {
        loadEvents(context);

        String key = group + "-" + code;
        EventInfo info = eventMap.get(key);

        if (info != null) {
            return new EventDescription(info.grp, info.desc);
        }

        AppLogger.i(TAG, "Evento desconocido — grupo: " + group + ", código: " + code);
        return new EventDescription(group, "Unknown Event");
    }
}