package com.example.syncro.objects.events;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.EventDescription;
import com.example.syncro.models.EventFila;
import com.example.syncro.models.EventInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gurux.dlms.GXDateTime;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class StandarEventLogReader {
    private static Map<String, EventInfo> eventMap = new HashMap<>();

    public static List<EventFila> readStandardEventLog(Context context, GXDLMSReader reader, String from, String to) {
        List<EventFila> result = new ArrayList<>();
        try {
            String obisLog = "0.0.99.98.0.255";
            GXDLMSProfileGeneric eventLog = new GXDLMSProfileGeneric(obisLog);

            System.out.println("Leyendo estructura de Standard Event Log...");
            reader.read(eventLog, 3); // Atributo 3: Capture Objects

            // --- SOLUCIÓN PARA ZIV: Check de estructura vacía ---
            if (eventLog.getCaptureObjects() == null || eventLog.getCaptureObjects().isEmpty()) {
                System.out.println("ZIV detectado o estructura vacía. Aplicando plantilla manual...");

                // 1. Definimos los objetos lógicos
                gurux.dlms.objects.GXDLMSClock clock = new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
                gurux.dlms.objects.GXDLMSData eventCode = new gurux.dlms.objects.GXDLMSData("0.0.96.11.7.255");

                // 2. Creamos las definiciones de captura (Attribute 2 es el valor del objeto)
                gurux.dlms.objects.GXDLMSCaptureObject capClock = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);
                gurux.dlms.objects.GXDLMSCaptureObject capEvent = new gurux.dlms.objects.GXDLMSCaptureObject(2, 0);

                // 3. Añadimos a la lista usando SimpleEntry para cumplir con Map.Entry
                eventLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(clock, capClock));
                eventLog.getCaptureObjects().add(new java.util.AbstractMap.SimpleEntry<>(eventCode, capEvent));
            }

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

            // ZIV suele ser muy estricto con los bytes de estado en el filtrado por rango
            Set<DateTimeSkips> skips = EnumSet.of(
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS);

            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Solicitando eventos desde " + from + " hasta " + to);

            // En ZIV por TCP/Gateway, a veces es mejor leer por entradas (AllRows)
            // si el filtro por rango da error, pero intentemos primero el rango:
            Object[] rows = reader.readRowsByRange(eventLog, start, end);

            if (rows != null && rows.length > 0) {

                int contador = 1;

                for (Object row : rows) {

                    Object[] fila = (Object[]) row;
                    String fecha = fila[0].toString();
                    int id = Integer.parseInt(fila[1].toString());
                    EventDescription info = getEventDescription(context, id, 1);

                    EventFila evento = new EventFila(
                            fecha,
                            id,
                            info.description,
                            info.grp
                    );

                    result.add(evento);

                    contador++;
                }

            } else {
                System.out.println("No se encontraron eventos comunes.");
            }
        } catch (Exception e) {
            // Tip Industrial: Si falla por "Access Error" al filtrar, el Gateway podría
            // estar bloqueando Selective Access
            System.err.println("Error leyendo logs: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private static void loadEvents(Context context) {

        if(!eventMap.isEmpty()) return;

        try {

            InputStream is = context.getAssets().open("events_table.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder json = new StringBuilder();
            String line;

            while((line = reader.readLine()) != null){
                json.append(line);
            }

            JSONArray array = new JSONArray(json.toString());

            for(int i=0;i<array.length();i++){

                JSONObject obj = array.getJSONObject(i);

                EventInfo info = new EventInfo();
                info.grp = obj.getInt("grp");
                info.cod = obj.getInt("cod");
                info.desc = obj.getString("desc");
                String key = info.grp + "-" + info.cod;

                eventMap.put(key, info);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static EventDescription getEventDescription(Context context, int code, int group) {

        loadEvents(context);

        String key = group + "-" + code;

        EventInfo info = eventMap.get(key);

        if(info != null){
            return new EventDescription(info.grp, info.desc);
        }

        return new EventDescription(group, "Unknown Event");
    }
}
