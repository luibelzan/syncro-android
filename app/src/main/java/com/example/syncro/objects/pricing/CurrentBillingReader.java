package com.example.syncro.objects.pricing;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.CierreEnCursoFila;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import gurux.dlms.GXDateTime;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;

public class CurrentBillingReader {

    public static ArrayList<CierreEnCursoFila> readCurrentBilling(GXDLMSReader reader)
            throws Exception {

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

                reader.read(pg, 2);
                Object[] buffer = pg.getBuffer();
                if (buffer == null || buffer.length == 0) continue;

                CierreEnCursoFila fila = parseBillingRow(buffer[0], captureObjects, contract);
                if (fila != null) resultado.add(fila);

            } catch (Exception e) {
                Log.e("CurrentBilling", "Error contrato " + contract + ": " + e.getMessage());
            }
        }

        return resultado;
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
            } else if (attrIndex == 5 && value instanceof GXDateTime) {
                maxDates[idx] = formatTimestamp((GXDateTime) value);
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
}