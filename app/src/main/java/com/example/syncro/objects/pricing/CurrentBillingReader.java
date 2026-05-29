package com.example.syncro.objects.pricing;

import android.util.Log;

import com.example.syncro.client.GXDLMSReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gurux.dlms.GXDateTime;
import gurux.dlms.objects.GXDLMSCaptureObject;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSProfileGeneric;
import gurux.dlms.objects.GXDLMSRegister;

public class CurrentBillingReader {

    public static void readCurrentBilling(GXDLMSReader reader) throws Exception {

        try {
            System.out.println("------------------------------");
            System.out.println("Cierres en curso");
            System.out.println("------------------------------");

            int[] contracts = { 1, 2, 3 };
            String[] obisCodes = { "0.0.21.0.11.255", "0.0.21.0.12.255", "0.0.21.0.13.255" };

            for (int contractNum = 0; contractNum < contracts.length; contractNum++) {
                int contract = contracts[contractNum];

                System.out.println("Cierre contrato nº " + contract);

                // Leer Profile Generic para este contrato
                GXDLMSProfileGeneric pg = new GXDLMSProfileGeneric(obisCodes[contractNum]);

                try {
                    // 1. Leer entries_in_use
                    reader.read(pg, 7);
                    if (pg.getEntriesInUse() == 0) {
                        System.out.println("There are no billings on this period/contract...");
                        System.out.println("------------------------------");
                        continue;
                    }

                    // 2. Leer capture_objects PRIMERO
                    reader.read(pg, 3);
                    List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> captureObjects = pg.getCaptureObjects();

                    // 3. Leer buffer DESPUÉS
                    reader.read(pg, 2);
                    Object[] buffer = pg.getBuffer();

                    if (buffer == null || buffer.length == 0) {
                        System.out.println("No billing data available");
                        System.out.println("------------------------------");
                        continue;
                    }

                    parseAndPrintBillingRow(buffer[0], captureObjects, contract, reader);

                } catch (Exception e) {
                    System.out.println("Error reading contract " + contract + ": " + e.getMessage());
                }

                System.out.println("------------------------------");

            }
        } catch (Exception e) {
            Log.e("DLMS", "Error al conectar/desconectar", e);
            throw e; // Re-lanzar para que el llamador sepa que falló
        } finally {
            if(reader != null) {
                reader.close();
            }
        }
    }

    private static void parseAndPrintBillingRow(
            Object rowObj,
            List<Map.Entry<GXDLMSObject, GXDLMSCaptureObject>> captureObjectsList,
            int contract, GXDLMSReader communicator) throws Exception {

        Object[] row = (Object[]) rowObj;
        if (row.length == 0) return;

        // Timestamp (primera columna)
        GXDateTime clock = (GXDateTime) row[0];
        String timestamp = formatTimestamp(clock);
        System.out.println("Timestamp " + timestamp + "W");

        long[] aPlus = new long[7], aMinus = new long[7];
        long[] qi = new long[7], qii = new long[7], qiii = new long[7], qiv = new long[7];
        long[] maxDemand = new long[7];
        String[] maxDates = new String[7];
        Arrays.fill(maxDates, "FFFFFFFFFFFFFFFF");

        for (int col = 1; col < row.length && col < captureObjectsList.size(); col++) {
            Map.Entry<GXDLMSObject, GXDLMSCaptureObject> entry = captureObjectsList.get(col);
            GXDLMSObject dlmsObject = entry.getKey();
            GXDLMSCaptureObject captureObj = entry.getValue();

            String obisCode = dlmsObject.getLogicalName();
            int attributeIndex = captureObj.getAttributeIndex();
            Object value = row[col];

            if (value == null) continue;

            int periodIndex = getTariffIndex(obisCode, contract);
            if (periodIndex < 0) continue;

            if (attributeIndex == 2) {
                if (obisCode.startsWith("1.0.1.8.")) {
                    aPlus[periodIndex] = scaleValue(value, obisCode);
                } else if (obisCode.startsWith("1.0.2.8.")) {
                    aMinus[periodIndex] = scaleValue(value, obisCode);
                } else if (obisCode.startsWith("1.0.5.8.")) {
                    qi[periodIndex] = scaleValue(value, obisCode);
                } else if (obisCode.startsWith("1.0.6.8.")) {
                    qii[periodIndex] = scaleValue(value, obisCode);
                } else if (obisCode.startsWith("1.0.7.8.")) {
                    qiii[periodIndex] = scaleValue(value, obisCode);
                } else if (obisCode.startsWith("1.0.8.8.")) {
                    qiv[periodIndex] = scaleValue(value, obisCode);
                } else if (obisCode.startsWith("1.0.1.6.") || obisCode.startsWith("1.0.2.6.")) {
                    maxDemand[periodIndex] = scaleValue(value, obisCode);
                }
            } else if (attributeIndex == 5 && value instanceof GXDateTime) {
                maxDates[periodIndex] = formatTimestamp((GXDateTime) value) + "W";
            }
        }

        printEnergySection("Tarifa activa Importada", aPlus, "[kWh]");
        printEnergySection("Tarifa activa Exportada", aMinus, "[kWh]");
        printEnergySection("Tarifa reactiva QI", qi, "[kvarh]");
        printEnergySection("Tarifa reactiva QII", qii, "[kvarh]");
        printEnergySection("Tarifa reactiva QIII", qiii, "[kvarh]");
        printEnergySection("Tarifa reactiva QIV", qiv, "[kvarh]");

        for (int i = 1; i <= 6; i++) {
            System.out.printf("Max periodo %d = %d [W]%n", i, maxDemand[i - 1]);
            System.out.printf("Fecha/hora max = %sW%n", maxDates[i - 1]);
        }
        System.out.printf("Max Total = %d [W]%n", maxDemand[6]);
        System.out.printf("Fecha/hora max = %sW%n", maxDates[6]);
    }

    private static Map<String, Integer> readScalers(GXDLMSReader communicator, int contract) {
        Map<String, Integer> scalers = new HashMap<>();
        String[] bases = { "1.0.1.8", "1.0.2.8", "1.0.5.8", "1.0.6.8", "1.0.7.8", "1.0.8.8", "1.0.1.6", "1.0.2.6" };

        for (String base : bases) {
            for (int n = 0; n <= 6; n++) {
                String cn = contract + "" + n;
                String obis = base + "." + cn + ".255";
                try {
                    GXDLMSRegister reg = new GXDLMSRegister(obis);
                    communicator.read(reg, 3); // scaler_unit

                    double scalerValue = reg.getScaler(); // ← double
                    int scaler = (int) Math.round(scalerValue); // ← Convertir a int

                    scalers.put(obis, scaler);
                } catch (Exception e) {
                    // scaler = 0 por defecto
                    Log.w("SCALER", "Failed reading scaler for " + obis + ": " + e.getMessage());
                }
            }
        }
        return scalers;
    }

    private static int getTariffIndex(String obisCode, int contract) {
        try {
            // OBIS formato: "1-0:X.Y.E.255"
            // Necesitamos el campo E (5º elemento si separamos por '.' y '-' y ':')
            // Gurux devuelve como "1-0:1.8.10.255" → split por '.' da ["1-0:1","8","10","255"]
            // El campo E es parts[2]
            String[] parts = obisCode.split("[.:]");
            // "1-0:1.8.10.255" → split por '.' → ["1-0:1","8","10","255"]
            // Mejor normalizar primero
            String normalized = obisCode.replace("-", ".").replace(":", ".");
            // "1.0.1.8.10.255"
            String[] p = normalized.split("\\.");
            if (p.length < 6) return -1;

            int e = Integer.parseInt(p[4]); // campo E: 10,11..16 para C1, 20..26 para C2...
            int c = e / 10;  // número de contrato
            int n = e % 10;  // número de tarifa (0=total, 1-6=periodos)

            if (c != contract) return -1;
            return (n == 0) ? 6 : n - 1;  // índice 0-5 para periodos, 6 para total

        } catch (Exception e) {
            return -1;
        }
    }

    private static long scaleValue(Object value, int scaler) {
        if (!(value instanceof Number))
            return 0;
        double rawValue = ((Number) value).doubleValue();
        return Math.round(rawValue * Math.pow(10, scaler));
    }

    private static void printEnergySection(String label, long[] values, String unit) {
        for (int i = 1; i <= 6; i++) {
            System.out.printf("%s %d = %d %s%n", label, i, values[i - 1], unit);
        }
        System.out.printf("%s Total = %d %s%n", label, values[6], unit);
    }

    private static String formatTimestamp(GXDateTime dt) {
        if (dt == null || dt.getValue() == null) {
            return "FFFFFFFFFFFFFFFF";
        }
        try {
            java.util.Date date = dt.getValue();
            LocalDateTime ldt = date.toInstant()
                    .atZone(java.util.TimeZone.getDefault().toZoneId())
                    .toLocalDateTime();
            return ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"));
        } catch (Exception e) {
            return "FFFFFFFFFFFFFFFF";
        }
    }

    private static long scaleValue(Object value, String obisCode) {
        if (!(value instanceof Number)) return 0;
        long raw = ((Number) value).longValue();

        // Energía (X.8.) → raw en Wh, convertir a kWh dividiendo /1000
        if (obisCode.contains(".8.")) {
            return raw / 1000;
        }
        // Potencia/maxímetro (X.6.) → raw en W, sin conversión
        if (obisCode.contains(".6.")) {
            return raw;
        }
        return raw;
    }

}
