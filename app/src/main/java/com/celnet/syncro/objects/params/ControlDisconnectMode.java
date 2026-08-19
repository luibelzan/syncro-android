package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.models.parameters.ControlModeResult;
import com.celnet.syncro.utils.AppLogger;

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
            AppLogger.i("Syncro", "CONTROL DE CONEXIÓN / DESCONEXIÓN");

            GXDLMSDisconnectControl dc = new GXDLMSDisconnectControl("0.0.96.3.10.255");

            reader.read(dc, 2); // output_state
            reader.read(dc, 3); // control_state
            reader.read(dc, 4); // control_mode

            String estadoInicial = dc.getOutputState() ? "Conectado" : "Desconectado";
            String controlStateInicial = controlStateToString(dc.getControlState());

            AppLogger.i("Syncro", "Estado inicial del relé:");
            AppLogger.i("Syncro", " - Output State: " + estadoInicial);
            AppLogger.i("Syncro", " - Control State: " + controlStateInicial);
            AppLogger.i("Syncro", " - Control Mode: " + dc.getControlMode());

            int methodId = connect ? 2 : 1;
            String action = connect ? "reconectar" : "desconectar";

            AppLogger.i("Syncro", "Intentando " + action + "...");

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

            reader.read(dc, 2);
            reader.read(dc, 3);

            String estadoFinal = dc.getOutputState() ? "Conectado" : "Desconectado";
            String controlStateFinal = controlStateToString(dc.getControlState());

            AppLogger.i("Syncro", "Nuevo estado del relé:");
            AppLogger.i("Syncro", " - Output State: " + estadoFinal);
            AppLogger.i("Syncro", " - Control State: " + controlStateFinal);

            return new ControlModeResult(
                    true,
                    estadoInicial + " (" + controlStateInicial + ")",
                    estadoFinal + " (" + controlStateFinal + ")",
                    "Operación de " + action + " realizada correctamente"
            );

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al conectar/desconectar: " + e.getMessage());

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
            AppLogger.i("Syncro", "LECTURA ESTADO ICP");

            GXDLMSDisconnectControl dc = new GXDLMSDisconnectControl("0.0.96.3.10.255");

            reader.read(dc, 2); // output_state
            reader.read(dc, 3); // control_state
            reader.read(dc, 4); // control_mode

            String estado = dc.getOutputState() ? "Conectado" : "Desconectado";
            String controlState = controlStateToString(dc.getControlState());
            String controlMode = String.valueOf(dc.getControlMode());

            AppLogger.i("Syncro", "Estado actual del relé:");
            AppLogger.i("Syncro", " - Output State: " + estado);
            AppLogger.i("Syncro", " - Control State: " + controlState);
            AppLogger.i("Syncro", " - Control Mode: " + controlMode);

            return new ControlModeResult(
                    true,
                    estado + " (" + controlState + ")",
                    estado + " (" + controlState + ")",
                    "Lectura realizada correctamente (Modo: " + controlMode + ")"
            );

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al leer estado ICP: " + e.getMessage());

            return new ControlModeResult(
                    false,
                    "Desconocido",
                    "Desconocido",
                    "Error al leer estado: " + e.getMessage()
            );
        }
    }
}