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

public class DisconnectEventLog {

    private static final String TAG = "DisconnectEventLog";
    private static Map<String, EventInfo> eventMap = new HashMap<>();

    public static List<EventFila> readDisconnectEventLog(Context context, GXDLMSReader reader, String from, String to) {
        List<EventFila> result = new ArrayList<>();
        try {
            String obisDisconnect = "0.0.99.98.2.255";
            GXDLMSProfileGeneric disconnectLog = new GXDLMSProfileGeneric(obisDisconnect);

            AppLogger.i(TAG, "Leyendo estructura de Disconnect Event Log...");
            reader.read(disconnectLog, 3);

            if (disconnectLog.getCaptureObjects() == null || disconnectLog.getCaptureObjects().isEmpty()) {
                AppLogger.i(TAG, "ZIV detectado o estructura vacía. Aplicando plantilla manual...");

                gurux.dlms.objects.GXDLMSClock clock = new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
                gurux.dlms.objects.GXDLMSData eventCode = new gurux.dlms.objects.GXDLMSData("0.0.96.11.7.255");

                gurux.dlms.objects.GXDLMSCaptureObject capClock = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);
                gurux.dlms.objects.GXDLMSCaptureObject capEvent = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);

                disconnectLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(clock, capClock));
                disconnectLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(eventCode, capEvent));
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

            AppLogger.i(TAG, "Solicitando eventos de desconexión...");
            Object[] rows = reader.readRowsByRange(disconnectLog, start, end);

            if (rows != null && rows.length > 0) {
                AppLogger.i(TAG, "Eventos encontrados: " + rows.length);

                for (Object row : rows) {
                    Object[] fila = (Object[]) row;
                    String fecha = fila[0].toString();
                    int id = Integer.parseInt(fila[1].toString());
                    EventDescription info = getEventDescription(context, id, 2);

                    EventFila evento = new EventFila(
                            fecha,
                            id,
                            info.description,
                            info.grp
                    );

                    result.add(evento);
                }

            } else {
                AppLogger.i(TAG, "No se encontraron registros de desconexión.");
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Error en Disconnect Log: " + e.getMessage());
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