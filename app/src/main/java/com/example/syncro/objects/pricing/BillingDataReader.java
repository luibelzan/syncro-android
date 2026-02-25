package com.example.syncro.objects.pricing;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import gurux.dlms.GXDateTime;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class BillingDataReader {

    public static void readBillingDataContract1(GXDLMSReader reader) throws Exception {

        try {
            System.out.println("Leyendo perfil de facturación (Data Billing) del contrato 1...\n");

            // Objeto del perfil de facturación del contrato 1
            GXDLMSProfileGeneric billingProfile = new GXDLMSProfileGeneric("0.0.98.1.1.255");

            // 1️⃣ Leer las columnas del perfil
            reader.read(billingProfile, 3);
            System.out.println("Objetos capturados:");
            for (Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry : billingProfile.getCaptureObjects()) {
                GXDLMSObject obj = entry.getKey();
                GXDLMSCaptureObject attr = entry.getValue();
                System.out.printf(" - %s (%s), atributo %d%n",
                        obj.getLogicalName(), obj.getObjectType(), attr.getAttributeIndex());
            }

            // 2️⃣ Leer el buffer completo
            reader.read(billingProfile, 2);
            Object buffer = billingProfile.getBuffer();

            // 3️⃣ Procesar registros
            if (buffer instanceof Object[]) {
                Object[] entries = (Object[]) buffer;
                System.out.println("\nSe recibieron " + entries.length + " registros del perfil de facturación.\n");

                for (Object entry : entries) {
                    if (entry instanceof Object[]) {
                        Object[] row = (Object[]) entry;
                        System.out.println("---- Registro de Facturación ----");

                        for (int i = 0; i < row.length && i < billingProfile.getCaptureObjects().size(); i++) {
                            Object value = row[i];
                            GXDLMSObject obj = billingProfile.getCaptureObjects().get(i).getKey();
                            GXDLMSCaptureObject attr = billingProfile.getCaptureObjects().get(i).getValue();

                            String obis = obj.getLogicalName();
                            System.out.printf("OBIS %s (Atributo %d) -> ", obis, attr.getAttributeIndex());

                            // Manejo según tipo de dato
                            if (value == null) {
                                System.out.println("Valor: <nulo>");
                            } else if (value instanceof GXDateTime) {
                                GXDateTime dt = (GXDateTime) value;
                                System.out.println("Fecha/Hora: " + dt.toFormatString());
                            } else if (value instanceof byte[]) {
                                byte[] bytes = (byte[]) value;
                                // Intentar mostrar texto si es legible, sino hex
                                String text = new String(bytes, StandardCharsets.UTF_8).trim();
                                boolean legible = text.chars().allMatch(ch -> ch >= 32 && ch <= 126);
                                if (legible && !text.isEmpty()) {
                                    System.out.println("Valor (texto): " + text);
                                } else {
                                    System.out.println("Valor (hex): " + HexFormat.of().formatHex(bytes));
                                }
                            } else {
                                System.out.println("Valor: " + value);
                            }
                        }
                    } else {
                        System.out.println("Entrada inesperada: " + entry);
                    }
                }
            } else {
                System.out.println("Tipo de buffer inesperado: " + buffer.getClass());
            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al leer Billing Data Contract 1", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            if(reader != null) {
                reader.close();
            }
        }
    }
}
