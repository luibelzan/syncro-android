package com.celnet.syncro.models.iec870.params;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fecha y hora del contador en formato "tiempo tipo B" (7 octetos):
 * ms (10 bits) | segundos (6 bits) | minutos (6 b) + TIS + IV | hora (5 b) + 2 b + SU
 * | día del mes (5 b) + día de la semana (3 b) | mes (4 b) + ETI/PTI | año (7 b) + 1 b.
 * SU = 1 es horario de verano (así lo indica la información de cambio de hora oficial
 * del contador).
 */
public class Iec870DateTime implements Serializable {

    private static final long serialVersionUID = 1L;

    public int year;     // 4 cifras
    public int month;    // 1..12
    public int day;      // 1..31
    public int hour;
    public int minute;
    public int second;
    public int millis;
    public boolean summer;

    /** Decodifica 7 octetos de tiempo tipo B a partir de {@code off}. */
    public static Iec870DateTime fromTimeB(byte[] d, int off) {
        Iec870DateTime t = new Iec870DateTime();
        int b0 = d[off] & 0xFF;
        int b1 = d[off + 1] & 0xFF;
        t.millis = b0 | ((b1 & 0x03) << 8);
        t.second = (b1 >> 2) & 0x3F;
        t.minute = d[off + 2] & 0x3F;
        t.hour = d[off + 3] & 0x1F;
        t.summer = (d[off + 3] & 0x80) != 0;
        t.day = d[off + 4] & 0x1F;
        if (t.day == 0) t.day = 1;
        t.month = d[off + 5] & 0x0F;
        if (t.month == 0) t.month = 1;
        t.year = 2000 + (d[off + 6] & 0x7F);
        return t;
    }

    /** Decodifica 5 octetos de tiempo tipo A (sin segundos) a partir de {@code off}. */
    public static Iec870DateTime fromTimeA(byte[] d, int off) {
        Iec870DateTime t = new Iec870DateTime();
        t.minute = d[off] & 0x3F;
        t.hour = d[off + 1] & 0x1F;
        t.summer = (d[off + 1] & 0x80) != 0;
        t.day = d[off + 2] & 0x1F;
        if (t.day == 0) t.day = 1;
        t.month = d[off + 3] & 0x0F;
        if (t.month == 0) t.month = 1;
        t.year = 2000 + (d[off + 4] & 0x7F);
        return t;
    }

    /** "dd/MM/yyyy HH:mm". */
    public String toShortText() {
        return String.format(Locale.US, "%02d/%02d/%04d %02d:%02d", day, month, year, hour, minute);
    }

    /** Codifica a 7 octetos de tiempo tipo B. */
    public byte[] toTimeB() {
        return new byte[]{
                (byte) (millis & 0xFF),
                (byte) (((millis >> 8) & 0x03) | ((second & 0x3F) << 2)),
                (byte) (minute & 0x3F),
                (byte) ((hour & 0x1F) | (summer ? 0x80 : 0)),
                (byte) ((day & 0x1F) | (diaSemanaIso() << 5)),
                (byte) (month & 0x0F),
                (byte) ((year % 100) & 0x7F)
        };
    }

    /** Codifica a 5 octetos de tiempo tipo A (sin segundos). */
    public byte[] toTimeA() {
        return new byte[]{
                (byte) (minute & 0x3F),
                (byte) ((hour & 0x1F) | (summer ? 0x80 : 0)),
                (byte) ((day & 0x1F) | (diaSemanaIso() << 5)),
                (byte) (month & 0x0F),
                (byte) ((year % 100) & 0x7F)
        };
    }

    /**
     * Fecha del móvil en la zona horaria dada, sin milisegundos. El horario de
     * verano se deduce de esa zona (en España, CEST).
     */
    public static Iec870DateTime fromDate(Date date, TimeZone zone) {
        Calendar c = new GregorianCalendar(zone);
        c.setTime(date);
        Iec870DateTime t = new Iec870DateTime();
        t.year = c.get(Calendar.YEAR);
        t.month = c.get(Calendar.MONTH) + 1;
        t.day = c.get(Calendar.DAY_OF_MONTH);
        t.hour = c.get(Calendar.HOUR_OF_DAY);
        t.minute = c.get(Calendar.MINUTE);
        t.second = c.get(Calendar.SECOND);
        t.millis = 0;
        t.summer = zone.inDaylightTime(date);
        return t;
    }

    /** Lunes = 1 .. domingo = 7. */
    private int diaSemanaIso() {
        Calendar c = new GregorianCalendar(year, month - 1, day);
        int dow = c.get(Calendar.DAY_OF_WEEK); // domingo = 1
        return dow == Calendar.SUNDAY ? 7 : dow - 1;
    }

    /** "yyyy-MM-dd HH:mm:ss" (el formato que espera ResultadosDateActivity). */
    public String toText() {
        return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d:%02d",
                year, month, day, hour, minute, second);
    }
}