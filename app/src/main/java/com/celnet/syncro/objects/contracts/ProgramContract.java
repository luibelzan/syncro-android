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
    private static final String OBIS_SPECIAL_DAYS      = "0.0.11.0.0.255";
    private static final String OBIS_END_OF_BILLING    = "0.0.94.34.41.255";
    private static final String OBIS_THRESHOLD         = "0.0.17.0.%d.255";

    private static final int NUM_PERIODOS = 6;

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
            if (tarifa != null) {
                escribirCalendarioPasivo(reader, contract, tarifa, activacion);
            }

            try {
                escribirDiasEspeciales(reader);
            } catch (Exception e) {
                AppLogger.i(TAG, "Special Days Table no soportada, omitiendo: " + e.getMessage());
            }

            if (cierreMes != null) {
                escribirCierreFacturacion(reader, contract, cierreMes);
            }

            if (thresholds != null && thresholds.length > 0) {
                escribirUmbralesPotencia(reader, contract, thresholds);
            }

            AppLogger.i(TAG, "=== Contrato " + contract + " programado correctamente ===");

        } catch (Exception e) {
            AppLogger.e(TAG, "Error al programar contrato " + contract + ": " + e.getMessage());
        }
    }

    private static void escribirCalendarioPasivo(
            GXDLMSReader reader,
            int          contract,
            String       tarifa,
            Date         activacion
    ) throws Exception {

        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);

        AppLogger.i(TAG, "Updating calendar_name_passive");
        cal.setCalendarNamePassive(tarifa);
        reader.writeObject(cal, 6);

        AppLogger.i(TAG, "Skipping season/week/day profiles (precargados en contador)");

        AppLogger.i(TAG, "Writing passive calendar activation time");
        if (activacion != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(activacion);

            int year  = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day   = c.get(Calendar.DAY_OF_MONTH);
            int hour  = c.get(Calendar.HOUR_OF_DAY);
            int min   = c.get(Calendar.MINUTE);
            int sec   = c.get(Calendar.SECOND);

            GXDateTime activacionDT = new GXDateTime(
                    year, month, day, hour, min, sec, -1
            );
            activacionDT.setSkip(EnumSet.of(
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            ));

            reader.getClient().updateValue(cal, 10, activacionDT);
            reader.writeObject(cal, 10);

            AppLogger.i(TAG, "Passive Contract Updated !");
        } else {
            AppLogger.i(TAG, "Passive Contract Updated ! (activation automatic)");
        }
    }

    private static GXDLMSDayProfile[] buildDayProfiles2_0TD() throws Exception {

        String scriptObis = "0.0.0.0.0.0";

        GXDLMSDayProfile day1 = new GXDLMSDayProfile();
        day1.setDayId(1);
        day1.setDaySchedules(new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 3),
                makeDayAction("08:00:00", scriptObis, 1),
                makeDayAction("10:00:00", scriptObis, 2),
                makeDayAction("14:00:00", scriptObis, 1),
                makeDayAction("18:00:00", scriptObis, 2),
                makeDayAction("22:00:00", scriptObis, 3),
        });

        GXDLMSDayProfile day2 = new GXDLMSDayProfile();
        day2.setDayId(2);
        day2.setDaySchedules(new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 3),
        });

        return new GXDLMSDayProfile[]{ day1, day2 };
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

    private static void escribirDiasEspeciales(GXDLMSReader reader) throws Exception {
        AppLogger.i(TAG, "Updating Special Days Table...");

        GXDLMSSpecialDaysTable sdt = new GXDLMSSpecialDaysTable(OBIS_SPECIAL_DAYS);

        GXDLMSSpecialDay[] specialDays = {
                makeSpecialDay(1, -1,  1,  1, 2),
                makeSpecialDay(2, -1,  1,  6, 2),
                makeSpecialDay(3, -1,  5,  1, 2),
                makeSpecialDay(4, -1,  8, 15, 2),
                makeSpecialDay(5, -1, 10, 12, 2),
                makeSpecialDay(6, -1, 11,  1, 2),
                makeSpecialDay(7, -1, 12,  6, 2),
                makeSpecialDay(8, -1, 12,  8, 2),
                makeSpecialDay(9, -1, 12, 25, 2),
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
            GXDLMSReader reader,
            int          contract,
            String       cierreMes
    ) throws Exception {
        AppLogger.i(TAG, "Writing passive end of billing " + contract);

        GXDLMSData endOfBilling = new GXDLMSData(OBIS_END_OF_BILLING);

        String[] parts = cierreMes.split("/");
        int year  = parts[0].equalsIgnoreCase("FFFF") ? 0xFFFF : Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day   = Integer.parseInt(parts[2]);

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
        dt[9]  = (byte) 0x80;
        dt[10] = (byte) 0x00;
        dt[11] = (byte) 0xFF;

        endOfBilling.setDataType(2, DataType.OCTET_STRING);
        endOfBilling.setValue(dt);
        reader.writeObject(endOfBilling, 2);

        AppLogger.i(TAG, "End of billing updated");
    }

    private static void escribirUmbralesPotencia(
            GXDLMSReader reader,
            int          contract,
            long[]       thresholds
    ) throws Exception {

        int numPeriodos = Math.min(thresholds.length, NUM_PERIODOS);

        for (int p = 0; p < numPeriodos; p++) {
            int periodoNum = p + 1;
            long valorW    = thresholds[p];

            String obisThreshold = String.format(
                    "0.1.94.34.%d.255", 0x0A + periodoNum
            );

            AppLogger.i(TAG, "Writing new threshold " + periodoNum + " -> " + valorW);

            GXDLMSRegister reg = new GXDLMSRegister(obisThreshold);
            reg.setDataType(2, DataType.UINT32);
            reg.setValue(valorW);
            reader.writeObject(reg, 2);
        }

        AppLogger.i(TAG, "Passive Thresholds updated");
    }
}