package com.example.syncro.objects.params;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;

import gurux.dlms.GXReplyData;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSDisconnectControl;
import gurux.dlms.objects.enums.ControlState;

public class ControlDisconnectMode {

    public static void setControlDisconnectMode(GXDLMSReader reader, GXDLMSSecureClient2 client, boolean connect) throws Exception {

        try {
            System.out.println("\n=== CONTROL DE CONEXIÓN / DESCONEXIÓN ===");

            GXDLMSDisconnectControl dc = new GXDLMSDisconnectControl("0.0.96.3.10.255");

            // Leer atributos básicos
            reader.read(dc, 2); // output_state
            reader.read(dc, 3); // control_state
            reader.read(dc, 4); // control_mode

            System.out.println("Estado inicial del relé:");
            System.out.println(" - Output State (físico): " + (dc.getOutputState() ? "Conectado" : "Desconectado"));
            System.out.println(" - Control State (lógico): " + controlStateToString(dc.getControlState()));
            System.out.println(" - Control Mode: " + dc.getControlMode());

            int methodId = connect ? 2 : 1;
            String action = connect ? "reconectar" : "desconectar";
            System.out.println("\nIntentando " + action + "...");

            // Crear solicitud con parámetros específicos
            byte[] parameters = new byte[] { 0x01, 0x0F, 0x00 }; // Parámetros usados en el software funcional
            byte[][] data = client.method(dc, methodId, parameters, DataType.OCTET_STRING);

            GXReplyData reply = new GXReplyData();

            for (byte[] frame : data) {
                reply.clear();
                reader.readDLMSPacket(frame, reply);
                if (reply.getError() != 0) {
                    throw new RuntimeException("Error DLMS en acción: " + reply.getError());
                }
            }
            // Leer nuevamente el estado
            reader.read(dc, 2);
            reader.read(dc, 3);

            System.out.println("\nNuevo estado del relé:");
            System.out.println(" - Output State: " + (dc.getOutputState() ? "Conectado" : "Desconectado"));
            System.out.println(" - Control State: " + controlStateToString(dc.getControlState()));
        } catch (Exception e) {
            Log.e("DLMS", "Error al conectar/desconectar", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        }
    }

    private static String controlStateToString(ControlState state) {
        if (state == null) {
            return "Desconocido (null)";
        }

        switch (state) {
            case DISCONNECTED:
                return "Desconectado";
            case CONNECTED:
                return "Conectado";
            case READY_FOR_RECONNECTION:
                return "Listo para reconexión manual";
            default:
                return "Desconocido (" + state + ")";
        }
    }
}
