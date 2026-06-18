package com.celnet.syncro.objects.instantValues;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.ParametrosS06;
import com.celnet.syncro.utils.AppLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXDateTime;
import gurux.dlms.enums.ObjectType;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSLimiter;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;
import gurux.dlms.objects.GXDLMSRegister;

public class ParametersReader {

    public static ParametrosS06 read(GXDLMSReader reader) throws Exception {

        AppLogger.i("ParametrosS06", "Leyendo parámetros S06");

        ParametrosS06 p = new ParametrosS06();

        // ── Fecha ────────────────────────────────────────────────────────────
        // TX: C0 01 C1 00 08  00 00 01 00 00 FF  02
        try {
            GXDLMSClock clock = new GXDLMSClock("0.0.1.0.0.255");
            reader.read(clock, 2);
            if (clock.getTime() != null) p.fecha = formatTimestamp(clock.getTime());
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "fecha: " + e.getMessage());
        }

        // ── Serial number ────────────────────────────────────────────────────
        // TX: C0 01 C1 00 01  00 00 60 01 00 FF  02
        // RX: 09 0A  "0141717528"
        p.serialNumber = readString(reader, "0.0.96.1.0.255", 2, "serialNumber");

        // ── UNESA Manufacturer / Model Type / Manufacturing year ─────────────
        // TX: C0 01 C1 00 01  00 00 60 01 01 FF  02
        // RX: 09 06  20 51 44 49 31 35  →  bytes: [space, Q, D, I, 1, 5]
        //     pos 0-1 = " Q" (manufacturer)
        //     pos 2-3 = "DI" (model type)
        //     pos 4-5 = "15" (manufacturing year)
        try {
            GXDLMSData d = new GXDLMSData("0.0.96.1.1.255");
            reader.read(d, 2);
            Object v = d.getValue();
            byte[] b = (v instanceof byte[]) ? (byte[]) v : v.toString().getBytes();
            p.unesaManufacturer = b.length >= 2 ? new String(b, 0, 2).trim()          : "N/A";
            p.unesaModelType    = b.length >= 4 ? new String(b, 2, 2).trim()          : "N/A";
            p.manufacturingYear = b.length >= 5 ? new String(b, 4, b.length - 4).trim() : "N/A";
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "unesaRaw: " + e.getMessage());
        }

        // ── Type of equipment / Protocol ─────────────────────────────────────
        // TX: C0 01 C1 00 01  00 00 60 01 02 FF  02
        // RX: 09 12  "contador  DLMS0106"  (18 bytes: 10 tipo + 8 protocolo)
        try {
            GXDLMSData d = new GXDLMSData("0.0.96.1.2.255");
            reader.read(d, 2);
            Object v = d.getValue();
            byte[] b = (v instanceof byte[]) ? (byte[]) v : v.toString().getBytes();
            if (b.length >= 18) {
                p.typeOfEquipment = new String(b, 0, 10).trim();
                p.protocol        = new String(b, 10, 8).trim();
            } else {
                p.typeOfEquipment = new String(b).trim();
            }
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "typeRaw: " + e.getMessage());
        }

        // ── Firmware version ─────────────────────────────────────────────────
        // TX: C0 01 C1 00 01  01 00 00 02 00 FF  02
        // RX: 09 05  "V0311"
        p.firmwareVersion = readString(reader, "1.0.0.2.0.255", 2, "firmwareVersion");

        // ── Prime Firmware version ───────────────────────────────────────────
        // TX: C0 01 C1 00 56  00 00 1C 07 00 FF  02  → class_id=86
        // RX: 09 08  "00-2201a"
        // Prime Firmware version
        try {
            GXDLMSObject obj = GXDLMSClient.createObject(ObjectType.forValue(86));
            obj.setLogicalName("0.0.28.7.0.255");
            reader.read(obj, 2);
            Object v = obj.getValues()[1];
            if (v instanceof byte[]) p.primeFirmwareVersion = new String((byte[]) v).trim();
            else if (v != null)      p.primeFirmwareVersion = v.toString().trim();
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "primeFirmwareVersion: " + e.getMessage());
        }

        // ── Id. Comunic. Multicast ───────────────────────────────────────────
        // TX: C0 01 C1 00 01  00 00 60 01 05 FF  02
        // RX: 09 18  "111111111111111111111111"  (24 chars)
        p.idComunicMulticast = readString(reader, "0.0.96.1.5.255", 2, "idComunicMulticast");

        // ── Prime MAC address ────────────────────────────────────────────────
        // TX: C0 01 C1 00 2B  00 00 1C 06 00 FF  02  → class_id=43
        // RX: 09 06  00 80 E1 16 93 49
        try {
            GXDLMSObject obj = GXDLMSClient.createObject(ObjectType.forValue(43));
            obj.setLogicalName("0.0.28.6.0.255");
            reader.read(obj, 2);
            Object v = obj.getValues()[1];
            if (v instanceof byte[]) {
                byte[] b = (byte[]) v;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < b.length; i++) {
                    if (i > 0) sb.append(":");
                    sb.append(String.format("%02X", b[i] & 0xFF));
                }
                p.primeMacAddress = sb.toString();
            } else if (v != null) {
                p.primeMacAddress = v.toString();
            }
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "primeMacAddress: " + e.getMessage());
        }

        // ── Primary / Secondary voltage y current ────────────────────────────
        // TX: C0 01 C1 00 01  01 00 00 04 03 FF  02  → 12 08FC  (2300/10=230,0 V)
        // TX: C0 01 C1 00 01  01 00 00 04 06 FF  02  → 12 08FC
        // TX: C0 01 C1 00 01  01 00 00 04 02 FF  02  → 12 0064  (100/10=10,0 A)
        // TX: C0 01 C1 00 01  01 00 00 04 05 FF  02  → 12 0064
        p.primaryVoltage   = readUint16Div10(reader, "1.0.0.4.3.255", 2, "primaryVoltage");
        p.secondaryVoltage = readUint16Div10(reader, "1.0.0.4.6.255", 2, "secondaryVoltage");
        p.primaryCurrent   = readUint16Div10(reader, "1.0.0.4.2.255", 2, "primaryCurrent");
        p.secondaryCurrent = readUint16Div10(reader, "1.0.0.4.5.255", 2, "secondaryCurrent");

        // ── Time threshold Voltage sags ──────────────────────────────────────
        // TX: C0 01 C1 00 0F  00 00 28 00 03 FF  07  → resultado=3 (minutos → ×60=180s)
        // class_id=15 = GXDLMSLimiter, attr 7 = minOverThresholdDuration
        // ── Time threshold Voltage sags ──────────────────────────────────────
        try {
            //GXDLMSObject obj = GXDLMSClient.createObject(ObjectType.forValue(15));
            //obj.setLogicalName("0.0.40.0.4.255");
            //reader.read(obj, 7);
            //Object v = obj.getValues()[6]; // attribute 7 → index 6 (1-based -> 0-based)
            p.thresholdVoltageSags = readRegisterLong(reader, "1.0.12.43.0.255", 2, "thresholdVoltageSags");
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "thresholdVoltageSags: " + e.getMessage());
        }

        // ── Time threshold Voltage swells ────────────────────────────────────
        // TX: C0 01 C1 00 03  01 00 0C 2B 00 FF  02  → 12 00B4  (180)
        p.thresholdVoltageSwells = readRegisterLong(reader, "1.0.12.43.0.255", 2, "thresholdVoltageSwells");

        // ── Load profile Period 1 ────────────────────────────────────────────
        // TX: C0 01 C1 00 07  01 00 63 01 00 FF  04  → 06 00000E10  (3600)
        try {
            GXDLMSProfileGeneric lp = new GXDLMSProfileGeneric("1.0.99.1.0.255");
            reader.read(lp, 4);
            p.loadProfilePeriod1 = toLong(lp.getCapturePeriod());
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "loadProfilePeriod1: " + e.getMessage());
        }

        // ── Demand close to contracted power ─────────────────────────────────
        // TX: C0 01 C1 00 03  00 00 5E 22 46 FF  02  → 12 251C  (9500/100=95,00%)
        p.demandCloseContractedPower =
                readRegisterLong(reader, "0.0.94.34.70.255", 2, "demandCloseContractedPower") / 100.0;

        // ── Reference voltage ────────────────────────────────────────────────
        // TX: C0 01 C1 00 03  01 00 00 06 04 FF  02  → 12 00E6  (230)
        p.referenceVoltage = readRegisterLong(reader, "1.0.0.6.4.255", 2, "referenceVoltage");

        // ── Long Power Failure threshold ─────────────────────────────────────
        // TX: C0 01 C1 00 03  00 00 60 07 14 FF  02  → 12 00B4  (180)
        p.longPowerFailureThreshold =
                readRegisterLong(reader, "0.0.96.7.20.255", 2, "longPowerFailureThreshold");

        // ── Voltage sag threshold ────────────────────────────────────────────
        // TX: C0 01 C1 00 03  01 00 0C 1F 00 FF  02  → 12 02BC  (700/100=7,00%)
        p.voltageSagThreshold =
                readRegisterLong(reader, "1.0.12.31.0.255", 2, "voltageSagThreshold") / 100.0;

        // ── Voltage swell threshold ──────────────────────────────────────────
        // TX: C0 01 C1 00 03  01 00 0C 23 00 FF  02  → 12 02BC  (700/100=7,00%)
        p.voltageSwellThreshold =
                readRegisterLong(reader, "1.0.12.35.0.255", 2, "voltageSwellThreshold") / 100.0;

        // ── Voltage cut-off threshold ────────────────────────────────────────
        // TX: C0 01 C1 00 03  00 00 5E 22 3C FF  02  → 12 1388  (5000/100=50,00%)
        p.voltageCutOffThreshold =
                readRegisterLong(reader, "0.0.94.34.60.255", 2, "voltageCutOffThreshold") / 100.0;

        // ── Automatic monthly billing ────────────────────────────────────────
        // TX: C0 01 C1 00 16  00 00 0F 01 01 FF  04  → class_id=22
        // RX: 01 01 02 02 09 04 00000000 09 05 FFFFFFFF01FF → array con 1 entrada = Y
        try {
            GXDLMSObject obj = GXDLMSClient.createObject(ObjectType.forValue(22));
            obj.setLogicalName("0.0.15.1.1.255");
            reader.read(obj, 4);
            Object v = obj.getValues()[3];
            if (v instanceof Object[]) {
                p.automaticMonthlyBilling = ((Object[]) v).length > 0 ? "Y" : "N";
            }
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", "automaticMonthlyBilling: " + e.getMessage());
        }

        // ── Scroll Display Mode ──────────────────────────────────────────────
        // TX: C0 01 C1 00 01  00 00 60 01 07 FF  02  → 09 01 42  ("B")
        p.scrollDisplayMode = readString(reader, "0.0.96.1.7.255", 2, "scrollDisplayMode");

        // ── Time for Scroll Display ──────────────────────────────────────────
        // TX: C0 01 C1 00 03  00 00 5E 22 6E FF  02  → 06 00000005  (5)
        p.timeForScrollDisplay =
                readRegisterLong(reader, "0.0.94.34.110.255", 2, "timeForScrollDisplay");

        return p;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String readString(GXDLMSReader reader, String obis, int attr, String field) {
        try {
            GXDLMSData d = new GXDLMSData(obis);
            reader.read(d, attr);
            Object v = d.getValue();
            if (v instanceof byte[]) return new String((byte[]) v).trim();
            if (v != null) return v.toString().trim();
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", field + ": " + e.getMessage());
        }
        return "N/A";
    }

    private static long readRegisterLong(GXDLMSReader reader, String obis, int attr, String field) {
        try {
            GXDLMSRegister r = new GXDLMSRegister(obis);
            reader.read(r, attr);
            return toLong(r.getValue());
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", field + ": " + e.getMessage());
        }
        return 0;
    }

    private static double readUint16Div10(GXDLMSReader reader, String obis, int attr, String field) {
        try {
            GXDLMSData d = new GXDLMSData(obis);
            reader.read(d, attr);
            return toLong(d.getValue()) / 10.0;
        } catch (Exception e) {
            AppLogger.w("ParametrosS06", field + ": " + e.getMessage());
        }
        return 0.0;
    }

    private static long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return 0;
    }

    private static String formatTimestamp(GXDateTime dt) {
        if (dt == null || dt.getValue() == null) return "N/A";
        try {
            LocalDateTime ldt = dt.getValue().toInstant()
                    .atZone(TimeZone.getDefault().toZoneId())
                    .toLocalDateTime();
            return ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        } catch (Exception e) {
            return "N/A";
        }
    }
}