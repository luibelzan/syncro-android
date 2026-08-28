package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.client.GXDLMSSecureClient2;
import com.celnet.syncro.models.parameters.ControlModeResult;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.GXDateTime;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSActionSchedule;
import gurux.dlms.objects.GXDLMSDisconnectControl;
import gurux.dlms.objects.GXDLMSScriptTable;
import gurux.dlms.objects.enums.ControlState;
import gurux.dlms.objects.enums.SingleActionScheduleType;

import java.util.Calendar;

public class ControlDisconnectMode {

    // OBIS del Single Action Schedule que dispara el script de conexión/desconexión
    // (0-0:15.0.1.255, el mismo que aparece en el log de la trama que fallaba).
    private static final String OBIS_SINGLE_ACTION_SCHEDULE = "0.0.15.0.1.255";

    // OBIS de la tabla de scripts referenciada (0-0:10.0.106.255 en el log).
    private static final String OBIS_SCRIPT_TABLE = "0.0.10.0.106.255";

    // Selectores de script dentro de la tabla: 1 = desconectar, 2 = reconectar
    // (mismo criterio que ya usas en el método manual con methodId).
    private static final int SCRIPT_SELECTOR_DESCONECTAR = 1;
    private static final int SCRIPT_SELECTOR_RECONECTAR = 2;

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

            String estadoInicial = outputStateToString(dc.getOutputState());
            String controlStateInicial = controlStateToString(dc.getControlState());
            String modoControl = "Modo " + dc.getControlMode();

            AppLogger.i("Syncro", "Estado inicial del relé:");
            AppLogger.i("Syncro", " - Output State: " + estadoInicial);
            AppLogger.i("Syncro", " - Control State: " + controlStateInicial);
            AppLogger.i("Syncro", " - Control Mode: " + dc.getControlMode());

            int methodId = connect ? 2 : 1;
            String action = connect ? "reconectar" : "desconectar";

            AppLogger.i("Syncro", "Intentando " + action + "...");

            //byte[] parameters = new byte[]{0x01, 0x0F, 0x00};

            byte[][] data = client.method(dc, methodId, 0, DataType.INT8);
            GXReplyData reply = new GXReplyData();

            for (byte[] frame : data) {
                reply.clear();
                reader.readDLMSPacket(frame, reply);

                if (reply.getError() != 0) {
                    throw new RuntimeException("Error DLMS en acción: " + reply.getError());
                }
            }

            Thread.sleep(1000);

            reader.read(dc, 2);
            reader.read(dc, 3);

            String estadoFinal = outputStateToString(dc.getOutputState());
            String controlStateFinal = controlStateToString(dc.getControlState());

            AppLogger.i("Syncro", "Nuevo estado del relé:");
            AppLogger.i("Syncro", " - Output State: " + estadoFinal);
            AppLogger.i("Syncro", " - Control State: " + controlStateFinal);

            return new ControlModeResult(
                    true,
                    estadoInicial + " (" + controlStateInicial + ")",
                    estadoFinal + " (" + controlStateFinal + ")",
                    modoControl,
                    "Operación de " + action + " realizada correctamente"
            );

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al conectar/desconectar: " + e.getMessage());

            return new ControlModeResult(
                    false,
                    "Desconocido",
                    "Desconocido",
                    "Desconocido",
                    "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Programa una conexión/desconexión automática del relé para una fecha y
     * hora concretas, usando GXDLMSActionSchedule (class_id 22, Single Action
     * Schedule, OBIS 0-0:15.0.1.255) que dispara el script de la tabla de
     * scripts (OBIS 0-0:10.0.106.255).
     *
     * A diferencia del intento manual original (que solo escribía el
     * atributo 4 con una structure{time, date} y nunca el atributo 3),
     * aquí se escriben los tres atributos relevantes:
     *   - atributo 2 (executed_script): vía setTarget() + setExecutedScriptSelector()
     *   - atributo 3 (type): NO se escribe (read-write-denied en este contador,
     *     confirmado en campo); se deja el valor por defecto de la clase
     *     (SingleActionScheduleType1, ejecución única).
     *   - atributo 4 (execution_time): un único GXDateTime con fecha+hora
     *     completas; GXDLMSActionSchedule lo serializa internamente como
     *     structure{octet_string(4)=time, octet_string(5)=date}, el mismo
     *     formato que el contador ya aceptó en la trama original.
     */
    public static ControlModeResult programarDesconexionAutomatica(
            GXDLMSReader reader,
            boolean connect,
            Calendar fechaActivacion
    ) {

        try {
            String action = connect ? "reconexión" : "desconexión";
            AppLogger.i("Syncro", "PROGRAMANDO " + action.toUpperCase() + " AUTOMÁTICA");

            GXDLMSActionSchedule schedule =
                    new GXDLMSActionSchedule(OBIS_SINGLE_ACTION_SCHEDULE);

            int scriptSelector = connect ? SCRIPT_SELECTOR_RECONECTAR : SCRIPT_SELECTOR_DESCONECTAR;

            // ── Atributo 2: executed_script ──────────────────────────────────
            GXDLMSScriptTable scriptTable = new GXDLMSScriptTable(OBIS_SCRIPT_TABLE);
            schedule.setTarget(scriptTable);
            schedule.setExecutedScriptSelector(scriptSelector);

            // ── Atributo 3: type — NO se escribe. El contador lo deniega
            //    (Read-Write denied, comprobado en campo); es de solo lectura
            //    en este equipo. El valor por defecto de la clase ya es
            //    SingleActionScheduleType1 (ejecución única), que es lo que
            //    necesitamos, así que no hace falta tocarlo.
            schedule.setType(SingleActionScheduleType.SingleActionScheduleType1);

            // ── Atributo 4: execution_time (fecha/hora completa de disparo) ──
            GXDateTime fechaEjecucion = new GXDateTime(fechaActivacion);
            schedule.setExecutionTime(new GXDateTime[]{fechaEjecucion});

            AppLogger.i("Syncro", "Script referenciado : " + OBIS_SCRIPT_TABLE
                    + " selector " + scriptSelector);
            AppLogger.i("Syncro", "Fecha/hora programada : "
                    + fechaActivacion.get(Calendar.YEAR) + "/"
                    + (fechaActivacion.get(Calendar.MONTH) + 1) + "/"
                    + fechaActivacion.get(Calendar.DAY_OF_MONTH) + " "
                    + fechaActivacion.get(Calendar.HOUR_OF_DAY) + ":"
                    + fechaActivacion.get(Calendar.MINUTE) + ":"
                    + fechaActivacion.get(Calendar.SECOND));

            reader.writeObject(schedule, 2); // executed_script
            reader.writeObject(schedule, 4); // execution_time

            AppLogger.i("Syncro", "Programación de " + action + " automática realizada correctamente.");

            return new ControlModeResult(
                    true,
                    "Programado",
                    "Programado",
                    "Single Action Schedule",
                    action.substring(0, 1).toUpperCase() + action.substring(1)
                            + " automática programada correctamente"
            );

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al programar la desconexión/reconexión automática: " + e.getMessage());

            return new ControlModeResult(
                    false,
                    "Desconocido",
                    "Desconocido",
                    "Desconocido",
                    "Error: " + e.getMessage()
            );
        }
    }

    private static String outputStateToString(boolean outputState) {
        return outputState ? "Cerrado" : "Abierto";
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

            String estado = outputStateToString(dc.getOutputState());
            String controlState = controlStateToString(dc.getControlState());
            String modoControl = "Modo " + dc.getControlMode();

            AppLogger.i("Syncro", "Estado actual del relé:");
            AppLogger.i("Syncro", " - Output State: " + estado);
            AppLogger.i("Syncro", " - Control State: " + controlState);
            AppLogger.i("Syncro", " - Control Mode: " + dc.getControlMode());

            return new ControlModeResult(
                    true,
                    estado + " (" + controlState + ")",
                    estado + " (" + controlState + ")",
                    modoControl,
                    "Lectura realizada correctamente"
            );

        } catch (Exception e) {
            AppLogger.e("DLMS", "Error al leer estado ICP: " + e.getMessage());

            return new ControlModeResult(
                    false,
                    "Desconocido",
                    "Desconocido",
                    "Desconocido",
                    "Error al leer estado: " + e.getMessage()
            );
        }
    }
}