package com.example.syncro.objects.params;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.client.GXDLMSSecureClient2;
import com.example.syncro.models.ControlModeResult;

import gurux.dlms.GXReplyData;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSDisconnectControl;
import gurux.dlms.objects.enums.ControlState;

public class ControlDisconnectMode {

    public static ControlModeResult setControlDisconnectMode(
            GXDLMSReader reader,
            GXDLMSSecureClient2 client,
            boolean connect
    ) throws Exception {

        try {
            System.out.println("\n=== CONTROL DE CONEXIÓN / DESCONEXIÓN ===");

            GXDLMSDisconnectControl dc = new GXDLMSDisconnectControl("0.0.96.3.10.255");

            // Leer atributos iniciales
            reader.read(dc, 2); // output_state
            reader.read(dc, 3); // control_state
            reader.read(dc, 4); // control_mode

            String estadoInicial = dc.getOutputState() ? "Conectado" : "Desconectado";
            String controlStateInicial = controlStateToString(dc.getControlState());

            System.out.println("Estado inicial del relé:");
            System.out.println(" - Output State: " + estadoInicial);
            System.out.println(" - Control State: " + controlStateInicial);
            System.out.println(" - Control Mode: " + dc.getControlMode());

            int methodId = connect ? 2 : 1;
            String action = connect ? "reconectar" : "desconectar";

            System.out.println("\nIntentando " + action + "...");

            // Parámetros (ajústalos si tu contador requiere otros)
            byte[] parameters = new byte[]{0x01, 0x0F, 0x00};

            byte[][] data = client.method(dc, methodId, parameters, DataType.OCTET_STRING);
            GXReplyData reply = new GXReplyData();

            for (byte[] frame : data) {
                reply.clear();
                reader.readDLMSPacket(frame, reply);

                if (reply.getError() != 0) {
                    throw new RuntimeException("Error DLMS en acción: " + reply.getError());
                }
            }

            // Leer estado final
            reader.read(dc, 2);
            reader.read(dc, 3);

            String estadoFinal = dc.getOutputState() ? "Conectado" : "Desconectado";
            String controlStateFinal = controlStateToString(dc.getControlState());

            System.out.println("\nNuevo estado del relé:");
            System.out.println(" - Output State: " + estadoFinal);
            System.out.println(" - Control State: " + controlStateFinal);

            return new ControlModeResult(
                    true,
                    estadoInicial + " (" + controlStateInicial + ")",
                    estadoFinal + " (" + controlStateFinal + ")",
                    "Operación de " + action + " realizada correctamente"
            );

        } catch (Exception e) {
            Log.e("DLMS", "Error al conectar/desconectar", e);

            return new ControlModeResult(
                    false,
                    "Desconocido",
                    "Desconocido",
                    "Error: " + e.getMessage()
            );
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

    public static ControlModeResult readControlDisconnectMode(
            GXDLMSReader reader,
            GXDLMSSecureClient2 client
    ) {

        try {
            System.out.println("\n=== LECTURA ESTADO ICP ===");

            GXDLMSDisconnectControl dc = new GXDLMSDisconnectControl("0.0.96.3.10.255");

            // Leer atributos
            reader.read(dc, 2); // output_state
            reader.read(dc, 3); // control_state
            reader.read(dc, 4); // control_mode

            String estado = dc.getOutputState() ? "Conectado" : "Desconectado";
            String controlState = controlStateToString(dc.getControlState());
            String controlMode = String.valueOf(dc.getControlMode());

            System.out.println("Estado actual del relé:");
            System.out.println(" - Output State: " + estado);
            System.out.println(" - Control State: " + controlState);
            System.out.println(" - Control Mode: " + controlMode);

            return new ControlModeResult(
                    true,
                    estado + " (" + controlState + ")",
                    estado + " (" + controlState + ")",
                    "Lectura realizada correctamente (Modo: " + controlMode + ")"
            );

        } catch (Exception e) {
            Log.e("DLMS", "Error al leer estado ICP", e);

            return new ControlModeResult(
                    false,
                    "Desconocido",
                    "Desconocido",
                    "Error al leer estado: " + e.getMessage()
            );
        }
    }
}
