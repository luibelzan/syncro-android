package com.celnet.syncro.objects.contracts;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import java.util.Calendar;
import java.util.List;

import gurux.dlms.GXDateTime;
import gurux.dlms.objects.GXDLMSActivityCalendar;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSDayProfile;
import gurux.dlms.objects.GXDLMSDayProfileAction;
import gurux.dlms.objects.GXDLMSRegister;
import gurux.dlms.objects.GXDLMSScheduleEntry;
import gurux.dlms.objects.GXDLMSSeasonProfile;
import gurux.dlms.objects.GXDLMSSpecialDay;
import gurux.dlms.objects.GXDLMSSpecialDaysTable;
import gurux.dlms.objects.GXDLMSWeekProfile;

public class ViewContract {

    private static final String TAG = "ViewContract";

    public static String leerContrato(GXDLMSReader reader, int contract) throws Exception {
        StringBuilder sb = new StringBuilder();
        String obisCalendar = String.format("0.0.13.0.%d.255", contract);

        sb.append("------------------------------\n");
        sb.append("Contrato ").append(contract).append("\n");
        sb.append("------------------------------\n");

        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obisCalendar);

        // ── Calendario ACTIVO ─────────────────────────────────────────────────
        reader.read(cal, 2);
        String nombreActivo = cal.getCalendarNameActive();
        if (nombreActivo == null || nombreActivo.isEmpty() || nombreActivo.equals("\0\0\0\0\0\0")) {
            sb.append("  -  Contrato Activo\n");
        } else {
            sb.append("  ").append(nombreActivo).append("  -  Contrato Activo\n");
        }
        sb.append("------------------------------\n");

        reader.read(cal, 3);
        reader.read(cal, 4);
        reader.read(cal, 5);

        sb.append("Days Profile:\n");
        sb.append(String.format(" %-12s %-12s %-6s%n", "Profile", "Start time", "Tariff"));
        GXDLMSDayProfile[] dayProfilesActive = cal.getDayProfileTableActive();
        if (dayProfilesActive != null) {
            for (GXDLMSDayProfile dp : dayProfilesActive) {
                for (GXDLMSDayProfileAction action : dp.getDaySchedules()) {
                    Calendar tc = action.getStartTime().getMeterCalendar();
                    String time = String.format("%02d:%02d",
                            tc.get(Calendar.HOUR_OF_DAY),
                            tc.get(Calendar.MINUTE));
                    sb.append(String.format(" %-12d %-12s %-6d%n",
                            dp.getDayId(), time, action.getScriptSelector()));
                }
            }
        }

        sb.append("Weeks Profile:\n");
        sb.append(String.format(" %-6s %-4s %-4s %-4s %-4s %-4s %-4s %-4s%n",
                "Week", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        GXDLMSWeekProfile[] weekProfilesActive = cal.getWeekProfileTableActive();
        if (weekProfilesActive != null) {
            for (GXDLMSWeekProfile wp : weekProfilesActive) {
                String weekName = weekByteToString(wp.getName());
                sb.append(String.format("  %-5s %-4d %-4d %-4d %-4d %-4d %-4d %-4d%n",
                        weekName,
                        wp.getMonday(), wp.getTuesday(), wp.getWednesday(),
                        wp.getThursday(), wp.getFriday(), wp.getSaturday(), wp.getSunday()));
            }
        }

        sb.append("Seasons:\n");
        sb.append(String.format(" %-12s %-12s %-6s%n", "Season", "Start Date", "Week"));
        GXDLMSSeasonProfile[] seasonProfilesActive = cal.getSeasonProfileActive();
        if (seasonProfilesActive != null) {
            for (int i = 0; i < seasonProfilesActive.length; i++) {
                GXDLMSSeasonProfile sp = seasonProfilesActive[i];
                String fecha = formatGXDate(sp.getStart());
                String weekName = weekByteToString(sp.getWeekName());
                sb.append(String.format(" %-13d %-12s %-6s%n", i + 1, fecha, weekName));
            }
        }

        GXDLMSSpecialDay[] holidays = null;
        try {
            GXDLMSSpecialDaysTable sdt = new GXDLMSSpecialDaysTable(
                    String.format("0.0.11.0.%d.255", contract));
            reader.read(sdt, 2);
            holidays = sdt.getEntries();
        } catch (Exception ignored) {}

        sb.append("Holidays:\n");
        sb.append(String.format(" %-12s %-6s%n", "Date", "Profile"));
        if (holidays != null && holidays.length > 0) {
            for (GXDLMSSpecialDay sd : holidays) {
                sb.append(String.format(" %-12s  %d%n",
                        formatGXDate(sd.getDate()), sd.getDayId()));
            }
        } else {
            sb.append(" N/A\n");
        }

        sb.append("Activado:\n");
        try {
            GXDLMSData activationData = new GXDLMSData("0.1.94.34.130.255");
            reader.read(activationData, 2);
            Object raw = activationData.getValue();
            Object[] arr = null;
            if (raw instanceof Object[]) arr = (Object[]) raw;
            else if (raw instanceof java.util.List) arr = ((java.util.List<?>) raw).toArray();

            if (arr != null && arr.length > 0 && arr[0] instanceof byte[]) {
                GXDateTime dt = (GXDateTime) gurux.dlms.GXDLMSClient
                        .changeType((byte[]) arr[0], gurux.dlms.enums.DataType.DATETIME);
                sb.append("  ").append(formatDateTime(dt)).append("\n \n");
            } else if (arr != null && arr.length > 0 && arr[0] instanceof GXDateTime) {
                sb.append("  ").append(formatDateTime((GXDateTime) arr[0])).append("\n \n");
            } else {
                sb.append("  FFFFFFFFFFFFFFFFFW\n \n");
            }
        } catch (Exception e) {
            sb.append("  FFFFFFFFFFFFFFFFFW\n \n");
        }

        sb.append("Cierre de facturación periodo 1\n");
        try {
            gurux.dlms.objects.GXDLMSSchedule schedule = new gurux.dlms.objects.GXDLMSSchedule(
                    String.format("0.0.15.1.%d.255", contract));

            try {
                java.lang.reflect.Field f = schedule.getClass().getSuperclass().getDeclaredField("objectType");
                f.setAccessible(true);
                f.set(schedule, gurux.dlms.enums.ObjectType.SCHEDULE);
                AppLogger.i(TAG, "objectType forzado: " + schedule.getObjectType());
            } catch (Exception ex) {
                AppLogger.e(TAG, "No se pudo forzar objectType: " + ex.getMessage());
            }

            reader.read(schedule, 4);
            List<GXDLMSScheduleEntry> entries = schedule.getEntries();
            if (entries != null && !entries.isEmpty()) {
                GXDLMSScheduleEntry entry = entries.get(0);
                for (java.lang.reflect.Method m : entry.getClass().getMethods()) {
                    if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
                        try {
                            Object val = m.invoke(entry);
                            AppLogger.i(TAG, "  " + m.getName() + " => " + val);
                        } catch (Exception ignored) {}
                    }
                }
                sb.append("  N/A (ver log)\n");
            } else {
                sb.append("  N/A\n");
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Error cierre activo: " + e.getMessage());
            sb.append("  N/A\n");
        }

        sb.append("Umbrales de potencia Activos :\n");
        for (int p = 1; p <= 6; p++) {
            String obisT = String.format("0.1.94.34.%d.255", p);
            GXDLMSRegister reg = new GXDLMSRegister(obisT);
            long val = 0;
            try {
                reader.read(reg, 2);
                val = ((Number) reg.getValue()).longValue();
            } catch (Exception ignored) {}
            sb.append(String.format(" Tarifa %d : %d [W]%n", p, val));
        }

        // ── Calendario PASIVO ─────────────────────────────────────────────────
        reader.read(cal, 6);
        String nombrePasivo = cal.getCalendarNamePassive();
        sb.append("------------------------------\n");
        if (nombrePasivo == null || nombrePasivo.isEmpty()) {
            sb.append("  -  Contrato Pasivo\n");
        } else {
            sb.append("  ").append(nombrePasivo).append("  -  Contrato Pasivo\n");
        }
        sb.append("------------------------------\n");

        reader.read(cal, 7);
        reader.read(cal, 8);
        reader.read(cal, 9);

        sb.append("Days Profile:\n");
        sb.append(String.format(" %-12s %-12s %-6s%n", "Profile", "Start time", "Tariff"));
        GXDLMSDayProfile[] dayProfilesPassive = cal.getDayProfileTablePassive();
        if (dayProfilesPassive != null) {
            for (GXDLMSDayProfile dp : dayProfilesPassive) {
                for (GXDLMSDayProfileAction action : dp.getDaySchedules()) {
                    Calendar tc = action.getStartTime().getMeterCalendar();
                    String time = String.format("%02d:%02d",
                            tc.get(Calendar.HOUR_OF_DAY),
                            tc.get(Calendar.MINUTE));
                    sb.append(String.format(" %-12d %-12s %-6d%n",
                            dp.getDayId(), time, action.getScriptSelector()));
                }
            }
        }

        sb.append("Weeks Profile:\n");
        sb.append(String.format(" %-6s %-4s %-4s %-4s %-4s %-4s %-4s %-4s%n",
                "Week", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        GXDLMSWeekProfile[] weekProfilesPassive = cal.getWeekProfileTablePassive();
        if (weekProfilesPassive != null) {
            for (GXDLMSWeekProfile wp : weekProfilesPassive) {
                String weekName = weekByteToString(wp.getName());
                sb.append(String.format("  %-5s %-4d %-4d %-4d %-4d %-4d %-4d %-4d%n",
                        weekName,
                        wp.getMonday(), wp.getTuesday(), wp.getWednesday(),
                        wp.getThursday(), wp.getFriday(), wp.getSaturday(), wp.getSunday()));
            }
        }

        sb.append("Seasons:\n");
        sb.append(String.format(" %-12s %-12s %-6s%n", "Season", "Start Date", "Week"));
        GXDLMSSeasonProfile[] seasonProfilesPassive = cal.getSeasonProfilePassive();
        if (seasonProfilesPassive != null) {
            for (int i = 0; i < seasonProfilesPassive.length; i++) {
                GXDLMSSeasonProfile sp = seasonProfilesPassive[i];
                String fecha = formatGXDate(sp.getStart());
                String weekName = weekByteToString(sp.getWeekName());
                sb.append(String.format(" %-13d %-12s %-6s%n", i + 1, fecha, weekName));
            }
        }

        sb.append("Holidays:\n");
        sb.append(String.format(" %-12s %-6s%n", "Date", "Profile"));
        if (holidays != null && holidays.length > 0) {
            for (GXDLMSSpecialDay sd : holidays) {
                sb.append(String.format(" %-12s  %d%n",
                        formatGXDate(sd.getDate()), sd.getDayId()));
            }
        } else {
            sb.append(" N/A\n");
        }

        sb.append("Fecha pasiva de activación:\n");
        try {
            Object raw = reader.read(cal, 10);
            if (raw instanceof GXDateTime) {
                sb.append("  ").append(formatDateTime((GXDateTime) raw)).append("\n \n");
            } else {
                sb.append("  FFFFFFFFFFFFFFFFFW\n \n");
            }
        } catch (Exception e) {
            sb.append("  FFFFFFFFFFFFFFFFFW\n \n");
        }

        sb.append("Cierre de facturación periodo 1\n");
        try {
            GXDLMSData endBillingPassive = new GXDLMSData("0.0.94.34.41.255");
            reader.read(endBillingPassive, 2);
            sb.append("  ").append(formatBillingDate(endBillingPassive.getValue())).append("\n");
        } catch (Exception e) {
            sb.append("  N/A\n");
        }

        sb.append("Umbrales de potencia pasivos :\n");
        for (int p = 1; p <= 6; p++) {
            String obisT = String.format("0.1.94.34.%d.255", 0x0A + p);
            GXDLMSRegister reg = new GXDLMSRegister(obisT);
            long val = 0;
            try {
                reader.read(reg, 2);
                val = ((Number) reg.getValue()).longValue();
            } catch (Exception ignored) {}
            sb.append(String.format(" Tarifa %d : %d [W]%n", p, val));
        }

        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String formatDateTime(GXDateTime dt) {
        if (dt == null) return "FFFFFFFFFFFFFFFFFW";
        try {
            Calendar c = dt.getMeterCalendar();
            if (c == null) return "FFFFFFFFFFFFFFFFFW";
            return String.format("%04d/%02d/%02d %02d:%02d:%02d.000W",
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH),
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    c.get(Calendar.SECOND));
        } catch (Exception e) {
            return "FFFFFFFFFFFFFFFFFW";
        }
    }

    private static String formatGXDate(GXDateTime dt) {
        if (dt == null) return "FF/FF/*";
        try {
            Calendar c = dt.getMeterCalendar();
            int month = c.get(Calendar.MONTH) + 1;
            int day   = c.get(Calendar.DAY_OF_MONTH);
            return String.format("%02d/%02d/*", day, month);
        } catch (Exception e) {
            return "FF/FF/*";
        }
    }

    private static String formatBillingDate(Object value) {
        if (value == null) return "FFFF/FF/FF";
        try {
            if (value instanceof GXDateTime) {
                GXDateTime dt = (GXDateTime) value;
                java.util.Set<gurux.dlms.enums.DateTimeSkips> skips = dt.getSkip();

                int year  = skips.contains(gurux.dlms.enums.DateTimeSkips.YEAR)  ? 0xFFFF : dt.getMeterCalendar().get(Calendar.YEAR);
                int month = skips.contains(gurux.dlms.enums.DateTimeSkips.MONTH) ? 0xFF   : dt.getMeterCalendar().get(Calendar.MONTH) + 1;
                int day   = skips.contains(gurux.dlms.enums.DateTimeSkips.DAY)   ? 0xFF   : dt.getMeterCalendar().get(Calendar.DAY_OF_MONTH);

                String y = (year  == 0xFFFF) ? "FFFF" : String.format("%04d", year);
                String m = (month == 0xFF)   ? "FF"   : String.format("%02d", month);
                String d = (day   == 0xFF)   ? "FF"   : String.format("%02d", day);
                return y + "/" + m + "/" + d;
            }
            if (value instanceof byte[]) {
                byte[] bytes = (byte[]) value;
                if (bytes.length >= 4) {
                    int year  = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
                    int month = bytes[2] & 0xFF;
                    int day   = bytes[3] & 0xFF;
                    String y = (year  == 0xFFFF) ? "FFFF" : String.format("%04d", year);
                    String m = (month == 0xFF)   ? "FF"   : String.format("%02d", month);
                    String d = (day   == 0xFF)   ? "FF"   : String.format("%02d", day);
                    return y + "/" + m + "/" + d;
                }
            }
            return "FFFF/FF/FF";
        } catch (Exception e) {
            return "FFFF/FF/FF";
        }
    }

    private static byte[] gxDateTimeToBytes(GXDateTime dt) {
        try {
            Object raw = dt.getValue();
            if (raw instanceof byte[]) return (byte[]) raw;
        } catch (Exception ignored) {}

        try {
            java.lang.reflect.Field f = GXDateTime.class.getDeclaredField("value");
            f.setAccessible(true);
            Object v = f.get(dt);
            if (v instanceof byte[]) return (byte[]) v;
        } catch (Exception ignored) {}

        try {
            gurux.dlms.GXByteBuffer bb = new gurux.dlms.GXByteBuffer();
            gurux.dlms.internal.GXCommon.setData(null, bb,
                    gurux.dlms.enums.DataType.DATETIME, dt);
            byte[] all = bb.array();
            byte[] result = new byte[12];
            System.arraycopy(all, 2, result, 0, 12);
            return result;
        } catch (Exception ignored) {}

        return null;
    }

    private static String weekByteToString(byte[] name) {
        if (name == null || name.length == 0) return "";
        if (name.length == 1) return String.valueOf(name[0] & 0xFF);
        String s = new String(name).trim();
        return s.replaceAll("[^\\x20-\\x7E]", "").trim();
    }

    private static String extraerFechaCierre(Object val) {
        try {
            Object[] arr = null;
            if (val instanceof Object[])         arr = (Object[]) val;
            else if (val instanceof List)        arr = ((List<?>) val).toArray();
            else if (val instanceof byte[])      return formatBillingDate(val);
            else if (val instanceof GXDateTime)  return formatBillingDate(val);

            if (arr == null || arr.length == 0) return "N/A";

            Object first = arr[0];
            Object[] entry = null;
            if (first instanceof Object[])        entry = (Object[]) first;
            else if (first instanceof List)       entry = ((List<?>) first).toArray();
            else if (first instanceof byte[])     return formatBillingDate(first);
            else if (first instanceof GXDateTime) return formatBillingDate(first);

            if (entry == null || entry.length == 0) return "N/A";

            byte[] candidate4 = null;
            byte[] candidate5 = null;

            for (Object field : entry) {
                if (field instanceof byte[]) {
                    byte[] b = (byte[]) field;
                    if (b.length == 5)      candidate5 = b;
                    else if (b.length == 4) candidate4 = b;
                    else if (b.length == 12) return formatBillingDate(b);
                } else if (field instanceof GXDateTime) {
                    return formatBillingDate(field);
                }
            }

            if (candidate5 != null) return formatBillingDate(candidate5);
            if (candidate4 != null) return formatBillingDate(candidate4);

            return "N/A";

        } catch (Exception e) {
            return "N/A";
        }
    }
}