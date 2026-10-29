package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import java.nio.charset.StandardCharsets;

import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSAssociationLogicalName;

/**
 * Cambio de contraseñas del contador DLMS. Cada contraseña es el secreto (atributo 7, OCTET STRING
 * con las 8 cifras en ASCII) de un objeto Association LN (clase 15).
 *
 * Verificado con el log del software funcional solo para la contraseña de lectura
 * (0.0.40.0.2.255). Las de mngmt/Local security y firmware no se han implementado: hace falta su
 * log, y escribirlas a ciegas podría dejar el contador sin acceso.
 */
public class PasswordParams {

    /** Association LN de la contraseña de lectura. */
    public static final String OBIS_PASSWORD_LECTURA = "0.0.40.0.2.255";

    /** Association LN de la contraseña de gestión (mngmt / Local security). */
    public static final String OBIS_PASSWORD_MGMT = "0.0.40.0.3.255";

    /** Association LN de la contraseña de firmware (verificado con log). */
    public static final String OBIS_PASSWORD_FIRMWARE = "0.0.40.0.4.255";

    private static final int ATTR_SECRET = 7;

    public static class ProgramarResult {
        public boolean lecturaOk = true;
        public String errorLectura = null;
        public boolean mgmtOk = true;
        public String errorMgmt = null;
        public boolean firmwareOk = true;
        public String errorFirmware = null;

        public boolean todoOk() {
            return lecturaOk && mgmtOk && firmwareOk;
        }
    }

    /** Contraseña válida: de 1 a 8 cifras (se rellena con ceros a la izquierda hasta 8). */
    public static String normalizar(String password) {
        if (password == null || !password.matches("\\d{1,8}")) {
            throw new IllegalArgumentException("La contraseña debe tener de 1 a 8 cifras");
        }
        return String.format("%8s", password).replace(' ', '0');
    }

    /** Programa la contraseña de lectura. Bloqueante. */
    public static ProgramarResult programarPasswordLectura(GXDLMSReader reader, String password) {
        ProgramarResult result = new ProgramarResult();
        try {
            String valor = normalizar(password);
            AppLogger.i("PasswordParams", "Updating read password");
            escribirSecreto(reader, OBIS_PASSWORD_LECTURA, valor);
            AppLogger.i("PasswordParams", "Updated read password : " + valor);
        } catch (Exception e) {
            AppLogger.e("PasswordParams", "Error while updating read password: " + e.getMessage());
            result.lecturaOk = false;
            result.errorLectura = mensajeError(e);
        }
        return result;
    }

    /**
     * Programa la contraseña de gestión (0.0.40.0.3.255). Verificado con log. OJO: la sesión actual
     * sigue abierta, pero la próxima conexión exigirá la nueva contraseña de gestión.
     */
    public static ProgramarResult programarPasswordMgmt(GXDLMSReader reader, String password,
                                                        ProgramarResult result) {
        if (result == null) {
            result = new ProgramarResult();
        }
        try {
            String valor = normalizar(password);
            AppLogger.i("PasswordParams", "Updating managment password");
            escribirSecreto(reader, OBIS_PASSWORD_MGMT, valor);
            AppLogger.i("PasswordParams", "Updated managment password : " + valor);
        } catch (Exception e) {
            AppLogger.e("PasswordParams", "Error while updating managment password: " + e.getMessage());
            result.mgmtOk = false;
            result.errorMgmt = mensajeError(e);
        }
        return result;
    }

    /**
     * Programa la contraseña de firmware (0.0.40.0.4.255). El reader debe pertenecer a una sesión de
     * firmware (cliente HDLC 3, autenticada con la contraseña de firmware actual), NO a la sesión de
     * gestión: el log del software funcional cierra la de gestión y abre otra.
     */
    public static ProgramarResult programarPasswordFirmware(GXDLMSReader reader, String password,
                                                            ProgramarResult result) {
        if (result == null) {
            result = new ProgramarResult();
        }
        try {
            String valor = normalizar(password);
            AppLogger.i("PasswordParams", "Updating firmware password");
            escribirSecreto(reader, OBIS_PASSWORD_FIRMWARE, valor);
            AppLogger.i("PasswordParams", "Updated firmware password : " + valor);
        } catch (Exception e) {
            AppLogger.e("PasswordParams", "Error while updating firmware password: " + e.getMessage());
            result.firmwareOk = false;
            result.errorFirmware = mensajeError(e);
        }
        return result;
    }

    private static void escribirSecreto(GXDLMSReader reader, String obis, String valor)
            throws Exception {
        GXDLMSAssociationLogicalName asociacion = new GXDLMSAssociationLogicalName(obis);
        asociacion.setDataType(ATTR_SECRET, DataType.OCTET_STRING);
        asociacion.setSecret(valor.getBytes(StandardCharsets.US_ASCII));
        reader.writeObject(asociacion, ATTR_SECRET);
    }

    private static String mensajeError(Exception e) {
        String m = e.getMessage() != null ? e.getMessage() : e.toString();
        if (m.toLowerCase().contains("denied")) {
            return "El contador ha denegado la escritura (READ_WRITE_DENIED). Compruebe que el modo "
                    + "de escritura está activado en el CN.";
        }
        return m;
    }
}