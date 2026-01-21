package com.example.syncro.objects;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gurux.dlms.GXArray;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDateTime;
import gurux.dlms.GXStructure;
import gurux.dlms.enums.DateTimeSkips;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class LoadProfileReader {

    public static void readLoadProfileDayByDay(GXDLMSReader reader, GXDLMSSecureClient2 client, String fechaInicio, String fechaFin) {
        try {
            // 1. Definir el objeto Profile Generic (LP1)
            GXDLMSProfileGeneric lp1 = new GXDLMSProfileGeneric("1.0.99.1.0.255");

            // 2. Leer objetos de captura (Atributo 3) para mapear columnas
            System.out.println("Leyendo escalares (Capture Objects)...");
            reader.read(lp1, 3);

            // 3. Configurar el rango de tiempo
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(formatter.parse(fechaInicio));
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(formatter.parse(fechaFin));
            endCal.set(Calendar.HOUR_OF_DAY, 0);
            endCal.set(Calendar.MINUTE, 0);
            endCal.set(Calendar.SECOND, 0);

            GXDateTime start = new GXDateTime(startCal);
            GXDateTime end = new GXDateTime(endCal);

            // 4. Configurar omisiones (Skips) usando el SET directamente
            // Según el error, setSkip espera Set<DateTimeSkips>, no un int.
            Set<DateTimeSkips> skips = new HashSet<>();
            skips.add(DateTimeSkips.DEVIATION); // Evita el error de zona horaria (FF C4)
            skips.add(DateTimeSkips.STATUS);    // Evita el error de byte de estado

            // PASAR EL SET DIRECTAMENTE
            start.setSkip(skips);
            end.setSkip(skips);

            System.out.println("Leyendo LP1 desde " + fechaInicio + " hasta " + fechaFin);

            // 5. Leer los datos por rango
            // El reader gestiona la segmentación y los paquetes "Next Data Block"
            Object[] rows = reader.readRowsByRange(lp1, start, end);

            // 6. Procesar y mostrar los datos
            if (rows != null && rows.length > 0) {
                System.out.println(rows.length + " registro/s recuperados.");

                for (Object row : rows) {
                    Object[] columns = (Object[]) row;
                    StringBuilder sb = new StringBuilder();
                    for (Object col : columns) {
                        sb.append(col).append(" | ");
                    }
                    System.out.println(sb.toString());
                }
            } else {
                System.out.println("No se encontraron registros o el medidor devolvió buffer vacío.");
            }

        } catch (Exception e) {
            System.err.println("Error durante la lectura del perfil: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
