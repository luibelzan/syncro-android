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
}