package com.example.syncro.objects.pricing;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreEnCursoFila;
import com.example.syncro.utils.AppLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import gurux.dlms.GXDateTime;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSDemandRegister;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;
import gurux.dlms.objects.GXDLMSRegister;

public class CurrentBillingReader {

    public static ArrayList<CierreEnCursoFila> readCurrentBilling(GXDLMSReader reader)
            throws Exception {

        AppLogger.i("Syncro", "Leyendo Cierres en Curso");

        ArrayList<CierreEnCursoFila> resultado = new ArrayList<>();

        int[]    contracts = {1, 2, 3};
        String[] obisCodes = {"0.0.21.0.11.255", "0.0.21.0.12.255", "0.0.21.0.13.255"};

        for (int i = 0; i < contracts.length; i++) {
            int contract = contracts[i];
            GXDLMSProfileGeneric pg = new GXDLMSProfileGeneric(obisCodes[i]);

            try {
                reader.read(pg, 7);
                if (pg.getEntriesInUse() == 0) continue;

                reader.read(pg, 3);
                List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> captureObjects =
                        pg.getCaptureObjects();

                if (captureObjects == null || captureObjects.isEmpty()) {
                    AppLogger.w("CurrentBilling",
                            "Capture objects vacíos para contrato " + contract
                                    + ", usando estructura hardcodeada");
                    pg.getCaptureObjects().addAll(buildCaptureObjects(contract)); // ← clave
                    captureObjects = pg.getCaptureObjects();
                }

                reader.read(pg, 2);
                Object[] buffer = pg.getBuffer();
                if (buffer == null || buffer.length == 0) continue;

                CierreEnCursoFila fila = parseBillingRow(buffer[0], captureObjects, contract);
                if (fila != null) resultado.add(fila);

            } catch (Exception e) {
                //Log.e("CurrentBilling", "Error contrato " + contract + ": " + e.getMessage());
                AppLogger.e("CurrentBilling", "Error contrato " + contract + ": " + e.getMessage());
            }
        }

        return resultado;
    }

    // Leer un contrato específico
    public static ArrayList<CierreEnCursoFila> readCurrentBilling(GXDLMSReader reader, int contract)
            throws Exception {

        AppLogger.i("Syncro", "Leyendo Cierres en Curso");

        ArrayList<CierreEnCursoFila> resultado = new ArrayList<>();

        String[] obisCodes = {"", "0.0.21.0.11.255", "0.0.21.0.12.255", "0.0.21.0.13.255"};

        if (contract < 1 || contract > 3) return resultado;

        GXDLMSProfileGeneric pg = new GXDLMSProfileGeneric(obisCodes[contract]);
        try {
            reader.read(pg, 7);
            if (pg.getEntriesInUse() == 0) return resultado;

            reader.read(pg, 3);
            List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> captureObjects =
                    pg.getCaptureObjects();

            if (captureObjects == null || captureObjects.isEmpty()) {
                AppLogger.w("CurrentBilling",
                        "Capture objects vacíos para contrato " + contract
                                + ", usando estructura hardcodeada");
                pg.getCaptureObjects().addAll(buildCaptureObjects(contract)); // ← clave
                captureObjects = pg.getCaptureObjects();
            }
            reader.read(pg, 2);
            Object[] buffer = pg.getBuffer();
            if (buffer == null || buffer.length == 0) return resultado;

            CierreEnCursoFila fila = parseBillingRow(buffer[0], captureObjects, contract);
            if (fila != null) resultado.add(fila);

        } catch (Exception e) {
            AppLogger.e("CurrentBilling", "Error contrato " + contract + ": " + e.getMessage());
        }

        return resultado;
    }

    private static List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> buildCaptureObjects(int contract) {
        List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> list = new ArrayList<>();

        // Tarifas: 1,2,3,4,5,6,0  (0 = total)
        int[] tariffs = {0, 1, 2, 3, 4, 5, 6};

        // col 0: timestamp
        addCO(list, new GXDLMSClock("0.0.1.0.0.255"), 2);

        // col 1-7:  aPlus
        for (int t : tariffs)
            addCO(list, new GXDLMSRegister("1.0.1.8." + (contract * 10 + t) + ".255"), 2);

        // col 8-14: aMinus
        for (int t : tariffs)
            addCO(list, new GXDLMSRegister("1.0.2.8." + (contract * 10 + t) + ".255"), 2);

        // col 15-21: qi
        for (int t : tariffs)
            addCO(list, new GXDLMSRegister("1.0.5.8." + (contract * 10 + t) + ".255"), 2);

        // col 22-28: qii
        for (int t : tariffs)
            addCO(list, new GXDLMSRegister("1.0.6.8." + (contract * 10 + t) + ".255"), 2);

        // col 29-35: qiii
        for (int t : tariffs)
            addCO(list, new GXDLMSRegister("1.0.7.8." + (contract * 10 + t) + ".255"), 2);

        // col 36-42: qiv
        for (int t : tariffs)
            addCO(list, new GXDLMSRegister("1.0.8.8." + (contract * 10 + t) + ".255"), 2);

        for (int t : tariffs) {
            addCO(list, new GXDLMSDemandRegister("1.0.1.6." + (contract * 10 + t) + ".255"), 2);
            addCO(list, new GXDLMSDemandRegister("1.0.1.6." + (contract * 10 + t) + ".255"), 5);
        }

        return list; // 1 + 8×7 = 57 entradas
    }

    private static void addCO(
            List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> list,
            GXDLMSObject obj, int attr) {
        list.add(new AbstractMap.SimpleEntry<>(obj, new GXDLMSCaptureObject(attr, 0)));
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    private static CierreEnCursoFila parseBillingRow(
            Object rowObj,
            List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> captureObjectsList,
            int contract) {

        Object[] row = (Object[]) rowObj;
        if (row.length == 0) return null;

        String timestamp = (row[0] instanceof GXDateTime)
                ? formatTimestamp((GXDateTime) row[0]) : "N/A";

        // Índice 0-5 = periodos 1-6 | Índice 6 = Total
        long[]   aPlus     = new long[7];
        long[]   aMinus    = new long[7];
        long[]   qi        = new long[7];
        long[]   qii       = new long[7];
        long[]   qiii      = new long[7];
        long[]   qiv       = new long[7];
        long[]   maxDemand = new long[7];
        String[] maxDates  = new String[7];
        Arrays.fill(maxDates, "N/A");

        for (int col = 1; col < row.length && col < captureObjectsList.size(); col++) {
            Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry = captureObjectsList.get(col);
            String obisCode  = entry.getKey().getLogicalName();
            int    attrIndex = entry.getValue().getAttributeIndex();
            Object value     = row[col];

            if (value == null) continue;

            int idx = getTariffIndex(obisCode, contract);
            if (idx < 0) continue;

            if (attrIndex == 2) {
                if      (obisCode.startsWith("1.0.1.8.")) aPlus    [idx] = scaleEnergy(value);
                else if (obisCode.startsWith("1.0.2.8.")) aMinus   [idx] = scaleEnergy(value);
                else if (obisCode.startsWith("1.0.5.8.")) qi       [idx] = scaleEnergy(value);
                else if (obisCode.startsWith("1.0.6.8.")) qii      [idx] = scaleEnergy(value);
                else if (obisCode.startsWith("1.0.7.8.")) qiii     [idx] = scaleEnergy(value);
                else if (obisCode.startsWith("1.0.8.8.")) qiv      [idx] = scaleEnergy(value);
                else if (obisCode.startsWith("1.0.1.6.") ||
                        obisCode.startsWith("1.0.2.6.")) maxDemand[idx] = scalePower(value);
            } else if (attrIndex == 5) {
                if (value instanceof GXDateTime) {
                    maxDates[idx] = formatTimestamp((GXDateTime) value);
                } else if (value instanceof byte[]) {
                    maxDates[idx] = parseDlmsDateTimeBytes((byte[]) value);
                }
            }
        }

        return new CierreEnCursoFila(
                timestamp, contract,
                aPlus, aMinus,
                qi, qii, qiii, qiv,
                maxDemand, maxDates
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static int getTariffIndex(String obisCode, int contract) {
        try {
            String[] p = obisCode.replace("-", ".").replace(":", ".").split("\\.");
            if (p.length < 6) return -1;
            int e = Integer.parseInt(p[4]);
            int c = e / 10;
            int n = e % 10;
            if (c != contract) return -1;
            return (n == 0) ? 6 : n - 1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static long scaleEnergy(Object value) {
        if (!(value instanceof Number)) return 0;
        return ((Number) value).longValue() / 1000; // Wh → kWh
    }

    private static long scalePower(Object value) {
        if (!(value instanceof Number)) return 0;
        return ((Number) value).longValue(); // W sin conversión
    }

    private static String formatTimestamp(GXDateTime dt) {
        if (dt == null || dt.getValue() == null) return "N/A";
        try {
            LocalDateTime ldt = dt.getValue().toInstant()
                    .atZone(java.util.TimeZone.getDefault().toZoneId())
                    .toLocalDateTime();
            return ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String parseDlmsDateTimeBytes(byte[] b) {
        if (b == null || b.length < 12) return "N/A";
        try {
            int year    = ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
            int month   =   b[2] & 0xFF;
            int day     =   b[3] & 0xFF;
            int hour    =   b[5] & 0xFF;
            int minute  =   b[6] & 0xFF;
            int second  =   b[7] & 0xFF;

            // 0xFF en cualquier campo = valor no especificado
            if (year == 0xFFFF || month == 0xFF || day == 0xFF) return "N/A";

            return String.format("%04d/%02d/%02d %02d:%02d:%02d",
                    year, month, day, hour, minute, second);
        } catch (Exception e) {
            return "N/A";
        }
    }
}