package com.celnet.syncro.objects.contracts;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.*;
import gurux.dlms.enums.*;
import gurux.dlms.objects.*;
import java.util.*;
import gurux.dlms.enums.DataType;

public class ProgramContract {

    private static final String TAG = "ProgramContract";

    private static final String OBIS_ACTIVITY_CALENDAR = "0.0.13.0.%d.255";
    private static final String OBIS_SPECIAL_DAYS = "0.0.11.0.%d.255";
    private static final String OBIS_END_OF_BILLING    = "0.0.94.34.41.255";
    private static final String OBIS_CLOCK             = "0.0.1.0.0.255";

    private static final int NUM_PERIODOS = 6;

    // Desviación UTC+2 en minutos (valor correcto como signed short)
    private static final int DEVIATION_UTC2 = 128; // 0x0080

    public static void programarContrato(
            GXDLMSReader reader,
            int          contract,
            String       tarifa,
            long[]       thresholds,
            Date         activacion,
            String       cierreMes
    ) {
        AppLogger.i(TAG, "=== Programando contrato " + contract
                + (tarifa != null ? " tarifa " + tarifa : "") + " ===");

        try {
            // ── SESIÓN 1 ────────────────────────────────────────────────────
            if (tarifa != null) {
                escribirNombreCalendarioPasivo(reader, contract, tarifa);
                escribirSeasonProfilePasivo(reader, contract);
                escribirWeekProfilePasivo(reader, contract);
                escribirDayProfilePasivo(reader, contract);
            }

            try {
                escribirDiasEspeciales(reader, contract);
            } catch (Exception e) {
                AppLogger.i(TAG, "Special Days Table no soportada, omitiendo: " + e.getMessage());
            }

            if (cierreMes != null) {
                escribirCierreFacturacion(reader, contract, cierreMes);
            }

            if (thresholds != null && thresholds.length > 0) {
                escribirUmbralesPotencia(reader, contract, thresholds);
            }

            // ── SESIÓN 2: activación ────────────────────────────────────────
            if (tarifa != null) {
                AppLogger.i(TAG, "Reconnecting for activation session...");
                reader.reconnect();
                if(activacion != null) {
                    escribirFechaActivacion(reader, contract, activacion);
                }
            }

            AppLogger.i(TAG, "=== Contrato " + contract + " programado correctamente ===");

        } catch (Exception e) {
            AppLogger.e(TAG, "Error al programar contrato " + contract + ": " + e.getMessage());
        }
    }

    // ── Sesión 1 ─────────────────────────────────────────────────────────────

    private static void escribirNombreCalendarioPasivo(
            GXDLMSReader reader, int contract, String tarifa
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating calendar_name_passive");
        cal.setCalendarNamePassive(tarifa);
        reader.writeObject(cal, 6);
    }

    private static void escribirSeasonProfilePasivo(
            GXDLMSReader reader, int contract
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating season_profile_passive");

        // Construir seasons como raw List para evitar que Gurux serialice
        // fechas incorrectas al usar GXDateTime con wildcards
        List<Object> seasons = new ArrayList<>();
        seasons.add(buildSeasonRaw((byte)1,  1,  1, (byte)1));
        seasons.add(buildSeasonRaw((byte)2,  3,  1, (byte)2));
        seasons.add(buildSeasonRaw((byte)3,  4,  1, (byte)4));
        seasons.add(buildSeasonRaw((byte)4,  6,  1, (byte)3));
        seasons.add(buildSeasonRaw((byte)5,  7,  1, (byte)1));
        seasons.add(buildSeasonRaw((byte)6,  8,  1, (byte)3));
        seasons.add(buildSeasonRaw((byte)7, 10,  1, (byte)4));
        seasons.add(buildSeasonRaw((byte)8, 11,  1, (byte)2));
        seasons.add(buildSeasonRaw((byte)9, 12,  1, (byte)1));

        reader.getClient().updateValue(cal, 7, seasons);
        reader.writeObject(cal, 7);
    }

    /**
     * Construye una season como lista raw de {name, datetime_bytes, weekName}.
     * Los bytes de datetime siguen el formato DLMS octet-string de 12 bytes:
     *   year(2) month(1) day(1) dow(1) hh(1) mm(1) ss(1) ms(1) dev(2) sts(1)
     * Con year=FFFF (skip), dow=7 (wildcard), hora=0, dev=0x0080, sts=0x80
     */
    private static List<Object> buildSeasonRaw(byte name, int month, int day, byte weekName) {
        List<Object> season = new ArrayList<>();
        season.add(new byte[]{name});
        byte[] dt = new byte[]{
                (byte) 0xFF, (byte) 0xFF,  // year = FFFF (skip)
                (byte) month,               // month
                (byte) day,                 // day
                (byte) 0x07,               // day_of_week = 7 (wildcard)
                (byte) 0x00,               // hour = 0
                (byte) 0x00,               // minute = 0
                (byte) 0x00,               // second = 0
                (byte) 0x00,               // millisecond = 0
                (byte) 0x00, (byte) 0x80,  // deviation = 0x0080 = 128 min = UTC+2
                (byte) 0x80                // status = 0x80 (DST active)
        };
        season.add(dt);
        season.add(new byte[]{weekName});
        return season;
    }

    private static void escribirWeekProfilePasivo(
            GXDLMSReader reader, int contract
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating week_profile_table_passive");

        GXDLMSWeekProfile w1 = makeWeekProfile((byte)1, 1, 1, 1, 1, 1, 5, 5);
        GXDLMSWeekProfile w2 = makeWeekProfile((byte)2, 2, 2, 2, 2, 2, 5, 5);
        GXDLMSWeekProfile w3 = makeWeekProfile((byte)3, 3, 3, 3, 3, 3, 5, 5);
        GXDLMSWeekProfile w4 = makeWeekProfile((byte)4, 4, 4, 4, 4, 4, 5, 5);

        cal.setWeekProfileTablePassive(new GXDLMSWeekProfile[]{w1, w2, w3, w4});
        reader.writeObject(cal, 8);
    }

    private static GXDLMSWeekProfile makeWeekProfile(
            byte name, int mon, int tue, int wed, int thu, int fri, int sat, int sun
    ) {
        GXDLMSWeekProfile w = new GXDLMSWeekProfile();
        w.setName(new byte[]{name});
        w.setMonday(mon); w.setTuesday(tue); w.setWednesday(wed);
        w.setThursday(thu); w.setFriday(fri); w.setSaturday(sat); w.setSunday(sun);
        return w;
    }

    private static void escribirDayProfilePasivo(
            GXDLMSReader reader, int contract
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating day_profile_table_passive");

        String scriptObis = "0.0.0.0.0.0";

        GXDLMSDayProfile day1 = makeDayProfile(1, new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 6),
                makeDayAction("08:00:00", scriptObis, 2),
                makeDayAction("09:00:00", scriptObis, 1),
                makeDayAction("14:00:00", scriptObis, 2),
                makeDayAction("18:00:00", scriptObis, 1),
                makeDayAction("22:00:00", scriptObis, 2),
        });
        GXDLMSDayProfile day2 = makeDayProfile(2, new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 6),
                makeDayAction("08:00:00", scriptObis, 3),
                makeDayAction("09:00:00", scriptObis, 2),
                makeDayAction("14:00:00", scriptObis, 3),
                makeDayAction("18:00:00", scriptObis, 2),
                makeDayAction("22:00:00", scriptObis, 3),
        });
        GXDLMSDayProfile day3 = makeDayProfile(3, new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 6),
                makeDayAction("08:00:00", scriptObis, 4),
                makeDayAction("09:00:00", scriptObis, 3),
                makeDayAction("14:00:00", scriptObis, 4),
                makeDayAction("18:00:00", scriptObis, 3),
                makeDayAction("22:00:00", scriptObis, 4),
        });
        GXDLMSDayProfile day4 = makeDayProfile(4, new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 6),
                makeDayAction("08:00:00", scriptObis, 5),
                makeDayAction("09:00:00", scriptObis, 4),
                makeDayAction("14:00:00", scriptObis, 5),
                makeDayAction("18:00:00", scriptObis, 4),
                makeDayAction("22:00:00", scriptObis, 5),
        });
        GXDLMSDayProfile day5 = makeDayProfile(5, new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 6),
        });

        cal.setDayProfileTablePassive(new GXDLMSDayProfile[]{day1, day2, day3, day4, day5});
        reader.writeObject(cal, 9);
    }

    private static GXDLMSDayProfile makeDayProfile(
            int dayId, GXDLMSDayProfileAction[] actions
    ) {
        GXDLMSDayProfile dp = new GXDLMSDayProfile();
        dp.setDayId(dayId);
        dp.setDaySchedules(actions);
        return dp;
    }

    private static GXDLMSDayProfileAction makeDayAction(
            String time, String scriptObis, int scriptSelector
    ) throws Exception {
        GXDLMSDayProfileAction action = new GXDLMSDayProfileAction();
        action.setStartTime(new GXTime(time));
        action.setScriptLogicalName(scriptObis);
        action.setScriptSelector(scriptSelector);
        return action;
    }

    private static void escribirDiasEspeciales(GXDLMSReader reader, int contract) throws Exception {
        // El contador usa 0.0.11.0.4.255 para escritura del pasivo
        // independientemente del contrato activo
        String obis = "0.0.11.0.4.255";
        AppLogger.i(TAG, "Updating Special Days Table " + obis);
        GXDLMSSpecialDaysTable sdt = new GXDLMSSpecialDaysTable(obis);
        GXDLMSSpecialDay[] specialDays = {
                makeSpecialDay(1, -1,  1,  1, 5),
                makeSpecialDay(2, -1,  1,  6, 5),
                makeSpecialDay(3, -1,  5,  1, 5),
                makeSpecialDay(4, -1,  8, 15, 5),
                makeSpecialDay(5, -1, 10, 12, 5),
                makeSpecialDay(6, -1, 11,  1, 5),
                makeSpecialDay(7, -1, 12,  6, 5),
                makeSpecialDay(8, -1, 12,  8, 5),
                makeSpecialDay(9, -1, 12, 25, 5),
        };
        sdt.setEntries(specialDays);
        reader.writeObject(sdt, 2);
    }

    private static GXDLMSSpecialDay makeSpecialDay(
            int index, int year, int month, int day, int dayType
    ) {
        GXDLMSSpecialDay sd = new GXDLMSSpecialDay();
        sd.setIndex(index);
        GXDate date = new GXDate(year, month, day);
        date.setSkip(EnumSet.of(DateTimeSkips.YEAR, DateTimeSkips.DAY_OF_WEEK));
        sd.setDate(date);
        sd.setDayId(dayType);
        return sd;
    }

    private static void escribirCierreFacturacion(
            GXDLMSReader reader, int contract, String cierreMes
    ) throws Exception {
        AppLogger.i(TAG, "Writing passive end of billing " + contract);
        GXDLMSData endOfBilling = new GXDLMSData(OBIS_END_OF_BILLING);
        String[] parts = cierreMes.split("/");

        int year  = parts[0].equalsIgnoreCase("FFFF") ? 0xFFFF : Integer.parseInt(parts[0]);
        int month = parseHexOrDec(parts[1]);
        int day   = parseHexOrDec(parts[2]);

        byte[] dt = new byte[12];
        dt[0]  = (byte) ((year >> 8) & 0xFF);
        dt[1]  = (byte) (year & 0xFF);
        dt[2]  = (byte) month;
        dt[3]  = (byte) day;
        dt[4]  = (byte) 0xFF;
        dt[5]  = (byte) 0x00;
        dt[6]  = (byte) 0x00;
        dt[7]  = (byte) 0x00;
        dt[8]  = (byte) 0x00;
        dt[9]  = (byte) 0x00;
        dt[10] = (byte) 0x80;
        dt[11] = (byte) 0xFF;
        endOfBilling.setDataType(2, DataType.OCTET_STRING);
        endOfBilling.setValue(dt);
        reader.writeObject(endOfBilling, 2);
        AppLogger.i(TAG, "End of billing updated");
    }

    private static int parseHexOrDec(String value) {
        if (value == null || value.isEmpty()) return 0xFF;
        try {
            // Si contiene letras A-F es hex (FD, FE, FF)
            if (value.matches(".*[A-Fa-f].*")) {
                return Integer.parseInt(value, 16);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0xFF; // fallback wildcard
        }
    }

    private static void escribirUmbralesPotencia(
            GXDLMSReader reader, int contract, long[] thresholds
    ) throws Exception {
        int numPeriodos = Math.min(thresholds.length, NUM_PERIODOS);
        for (int p = 0; p < numPeriodos; p++) {
            int periodoNum = p + 1;
            long valorW    = thresholds[p];
            String obisThreshold = String.format("0.1.94.34.%d.255", 0x0A + periodoNum);
            AppLogger.i(TAG, "Writing new threshold " + periodoNum + " -> " + valorW);
            GXDLMSRegister reg = new GXDLMSRegister(obisThreshold);
            reg.setDataType(2, DataType.UINT32);
            reg.setValue(valorW);
            reader.writeObject(reg, 2);
        }
        AppLogger.i(TAG, "Passive Thresholds updated");
    }

    // ── Sesión 2 ─────────────────────────────────────────────────────────────

    private static void escribirFechaActivacion(
            GXDLMSReader reader, int contract, Date activacion
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);

        byte[] dtBytes;

        if (activacion != null) {
            // Leer reloj del contador para confirmar desviación
            try {
                GXDLMSClock clock = new GXDLMSClock(OBIS_CLOCK);
                reader.read(clock, 2);
                GXDateTime meterTime = clock.getTime();
                if (meterTime != null) {
                    AppLogger.i(TAG, "Meter time raw: " + meterTime);
                }
            } catch (Exception e) {
                AppLogger.i(TAG, "No se pudo leer el reloj del contador");
            }

            Calendar c = Calendar.getInstance();
            c.setTime(activacion);

            int year   = c.get(Calendar.YEAR);
            int month  = c.get(Calendar.MONTH) + 1;
            int day    = c.get(Calendar.DAY_OF_MONTH);
            int hour   = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            int second = c.get(Calendar.SECOND);

            // Construir octet-string de 12 bytes directamente
            // para garantizar que dev=0x0080 y dow=FF (skip)
            dtBytes = new byte[]{
                    (byte)((year >> 8) & 0xFF), (byte)(year & 0xFF), // year
                    (byte) month,               // month
                    (byte) day,                 // day
                    (byte) 0xFF,               // day_of_week = FF (skip)
                    (byte) hour,               // hour
                    (byte) minute,             // minute
                    (byte) second,             // second
                    (byte) 0x00,               // millisecond
                    (byte) 0x00, (byte) 0x80,  // deviation = 0x0080 = 128 min = UTC+2
                    (byte) 0x00                // status = 0x00
            };

            AppLogger.i(TAG, "Writing passive calendar activation time: " + activacion
                    + " deviation=0x0080 (128 min UTC+2)");

        } else {
            // Activación inmediata: todos los campos FF (wildcarded)
            dtBytes = new byte[]{
                    (byte)0xFF, (byte)0xFF,  // year = FF FF
                    (byte)0xFF,              // month = FF
                    (byte)0xFF,              // day = FF
                    (byte)0xFF,              // dow = FF
                    (byte)0xFF,              // hour = FF
                    (byte)0xFF,              // minute = FF
                    (byte)0xFF,              // second = FF
                    (byte)0xFF,              // ms = FF
                    (byte)0xFF, (byte)0xFF,  // deviation = FF FF
                    (byte)0xFF               // status = FF
            };
            AppLogger.i(TAG, "Writing passive calendar activation time: immediate (null)");
        }

        // Usar updateValue con el octet-string raw para evitar
        // que Gurux reinterprete la desviación como signed
        reader.getClient().updateValue(cal, 10, dtBytes);
        reader.writeObject(cal, 10);

        AppLogger.i(TAG, "Passive Contract Updated !");
    }
}