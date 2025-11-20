package com.example.syncro.objects;

import android.content.Context;
import android.util.Log;

import com.example.syncro.client.DLMSConnection;
import com.example.syncro.client.GXDLMSReader;

import java.util.Map;

import gurux.dlms.GXDateTime;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class StandarEventLogReader {
    public static void readStandarEventLog(Context context) throws Exception {
        DLMSConnection con = DLMSConnection.initializeConnection2(context);
        GXDLMSReader reader = con.reader;
        try {
            System.out.println("Leyendo perfil de cierres mensuales...\n");

            GXDLMSProfileGeneric profile = new GXDLMSProfileGeneric("0.0.99.98.0.255");

            // Leer columnas del perfil (capture objects)
            reader.read(profile, 3);
            System.out.println("Objetos capturados:");
            for (java.util.Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry : profile.getCaptureObjects()) {
                GXDLMSObject obj = entry.getKey();
                System.out.println(" - " + obj.getLogicalName() + " (" + obj.getObjectType() + ")");
            }

            // Leer el buffer (cierres mensuales)
            reader.read(profile, 2);
            Object buffer = profile.getBuffer();

            // Procesar los datos recibidos
            if (buffer instanceof Object[]) {
                Object[] entries = (Object[]) buffer;
                System.out.println("\nSe recibieron " + entries.length + " registros del perfil mensual.\n");

                for (Object entry : entries) {
                    if (entry instanceof Object[]) {
                        Object[] row = (Object[]) entry;
                        System.out.println("---- Registro ----");
                        for (Object value : row) {
                            if (value instanceof GXDateTime) {
                                GXDateTime dt = (GXDateTime) value;
                                System.out.println("Fecha/Hora: " + dt.toFormatString());
                            } else {
                                System.out.println("Valor: " + value);
                            }
                        }
                    } else {
                        System.out.println("Entrada inesperada: " + entry);
                    }
                }

                System.out.println("\nEstructura del perfil:");
                int index = 1;
                for (Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry : profile.getCaptureObjects()) {
                    GXDLMSObject obj = entry.getKey();
                    GXDLMSCaptureObject attr = entry.getValue();
                    System.out.printf(" %d) %s (%s) atributo %d%n",
                            index++, obj.getLogicalName(), obj.getObjectType(), attr.getAttributeIndex());
                }

            } else {
                System.out.println("Tipo de buffer inesperado: " + buffer.getClass());
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Standar Event Log", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            DLMSConnection.closeConnection(con.reader, con.serial);
        }
    }
}
