package com.celnet.syncro.objects.events;

import gurux.dlms.GXDateTime;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Formatea valores crudos de columnas DLMS (fechas, estructuras, números)
 * al formato plano usado en los campos D1/D2 de los reportes S09, por
 * ejemplo una fecha compacta "20260827083521000S" o una lista
 * "null,null,null,0.0".
 */
public final class EventExtraDataFormatter {

    private EventExtraDataFormatter() {
    }

    public static String format(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof GXDateTime) {
            return formatDateTime((GXDateTime) value);
        }
        if (value instanceof byte[]) {
            return formatRawBytes((byte[]) value);
        }
        if (value instanceof Object[]) {
            return formatList((Object[]) value);
        }
        if (value instanceof List<?>) {
            return formatList(((List<?>) value).toArray());
        }
        return String.valueOf(value);
    }

    /** Une varios valores (columnas discretas) separados por comas. */
    public static String formatList(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(format(values[i]));
        }
        return sb.toString();
    }

    private static String formatDateTime(GXDateTime dt) {
        Calendar c = dt.getMeterCalendar();
        return String.format(Locale.US, "%04d%02d%02d%02d%02d%02d%03dS",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND),
                c.get(Calendar.MILLISECOND));
    }

    /**
     * Muchas columnas de fecha en logs DLMS llegan como byte[12] sin
     * convertir a GXDateTime (Gurux no siempre las tipa automáticamente
     * cuando el capture_object no viene registrado como DateTime). Un
     * octet_string[12] de fecha/hora DLMS tiene el formato estándar:
     * año(2 bytes) mes(1) día(1) día_semana(1) hora(1) min(1) seg(1)
     * centésimas(1) desviación(2) estado(1).
     */
    private static String formatRawBytes(byte[] bytes) {
        if (bytes.length == 12) {
            try {
                int year = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
                int month = bytes[2] & 0xFF;
                int day = bytes[3] & 0xFF;
                int hour = bytes[5] & 0xFF;
                int minute = bytes[6] & 0xFF;
                int second = bytes[7] & 0xFF;

                // Valores no especificados en DLMS (0xFF/0xFFFF) no son una
                // fecha válida; en ese caso se cae al formato hexadecimal.
                if (year != 0xFFFF && in1to12(month) && in1to31(day)) {
                    return String.format(Locale.US, "%04d%02d%02d%02d%02d%02d000S",
                            year, month, day, hour, minute, second);
                }
            } catch (Exception ignored) {
                // cae al formato hexadecimal de abajo
            }
        }
        return bytesToHex(bytes);
    }

    private static boolean in1to12(int v) {
        return v >= 1 && v <= 12;
    }

    private static boolean in1to31(int v) {
        return v >= 1 && v <= 31;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}