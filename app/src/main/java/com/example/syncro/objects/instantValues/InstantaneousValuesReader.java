package com.example.syncro.objects.instantValues;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.models.InstantaneousValues;

import java.util.logging.Level;
import java.util.logging.Logger;

import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class InstantaneousValuesReader {

    private static final Logger LOGGER = Logger.getLogger(InstantaneousValuesReader.class.getName());

    /**
     * 🔹 Método principal adaptado a tu modelo
     */
    public static String leerValores(GXDLMSReader reader) throws Exception {

        LOGGER.info("Leyendo valores instantáneos...");
        StringBuilder sb = new StringBuilder();

        // ── Escalares — leer primero atrib 3 ─────────────────────────────────
        int scalerV   = readScaler("1.0.32.7.0.255", reader);
        int scalerA   = readScaler("1.0.31.7.0.255", reader);
        int scalerW   = readScaler("1.0.21.7.0.255", reader);
        int scalerVAr = readScaler("1.0.23.7.0.255", reader);

        // ── Timestamp ─────────────────────────────────────────────────────────
        sb.append("------------------------------\n");
        try {
            gurux.dlms.objects.GXDLMSClock clock =
                    new gurux.dlms.objects.GXDLMSClock("0.0.1.0.0.255");
            reader.read(clock, 2);
            gurux.dlms.GXDateTime dt = clock.getTime();
            java.util.Calendar c = dt.getMeterCalendar();
            sb.append(String.format("Timestamp : %04d/%02d/%02d %02d:%02d:%02d.000S%n",
                    c.get(java.util.Calendar.YEAR),
                    c.get(java.util.Calendar.MONTH) + 1,
                    c.get(java.util.Calendar.DAY_OF_MONTH),
                    c.get(java.util.Calendar.HOUR_OF_DAY),
                    c.get(java.util.Calendar.MINUTE),
                    c.get(java.util.Calendar.SECOND)));
        } catch (Exception e) {
            sb.append("Timestamp : N/A\n");
        }

        // ── Voltajes y corrientes por fase ────────────────────────────────────
        double v1 = 0, v2 = 0, v3 = 0;
        double a1 = 0, a2 = 0, a3 = 0;
        double fp1 = 0, fp2 = 0, fp3 = 0;

        try { v1 = readRegisterRaw("1.0.32.7.0.255", reader) * Math.pow(10, scalerV); } catch (Exception ignored) {}
        try { v2 = readRegisterRaw("1.0.52.7.0.255", reader) * Math.pow(10, scalerV); } catch (Exception ignored) {}
        try { v3 = readRegisterRaw("1.0.72.7.0.255", reader) * Math.pow(10, scalerV); } catch (Exception ignored) {}

        try { a1 = readRegisterRaw("1.0.31.7.0.255", reader) * Math.pow(10, scalerA); } catch (Exception ignored) {}
        try { a2 = readRegisterRaw("1.0.51.7.0.255", reader) * Math.pow(10, scalerA); } catch (Exception ignored) {}
        try { a3 = readRegisterRaw("1.0.71.7.0.255", reader) * Math.pow(10, scalerA); } catch (Exception ignored) {}

        int scalerFP = readScaler("1.0.33.7.0.255", reader);
        try { fp1 = readRegisterRaw("1.0.33.7.0.255", reader) * Math.pow(10, scalerFP); } catch (Exception ignored) {}
        try { fp2 = readRegisterRaw("1.0.53.7.0.255", reader) * Math.pow(10, scalerFP); } catch (Exception ignored) {}
        try { fp3 = readRegisterRaw("1.0.73.7.0.255", reader) * Math.pow(10, scalerFP); } catch (Exception ignored) {}

        double fpTotal = 0;
        try { fpTotal = readRegisterRaw("1.0.13.7.0.255", reader) * Math.pow(10, scalerFP); } catch (Exception ignored) {
            fpTotal = (fp1 + fp2 + fp3) / 3.0;
        }

        sb.append(String.format("Valores Tensión       Corriente        FP :%n"));
        sb.append(String.format("Fase 1 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v1, a1, fp1));
        sb.append(String.format("Fase 2 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v2, a2, fp2));
        sb.append(String.format("Fase 3 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v3, a3, fp3));
        sb.append(String.format("FP (sum of all phases: +P/S) : %,.3f%n", fpTotal));

        // ── Relaciones de transformación ──────────────────────────────────────
        // El software funcional usa OBIS 1.0.0.4.x.255 (campo A=1)
        double vPrim = 0, vSec = 0, aPrim = 0, aSec = 0;
        try {
            vPrim = readDataValue("1.0.0.4.3.255", reader);
            vSec  = readDataValue("1.0.0.4.6.255", reader);
        } catch (Exception e) {
            // fallback A=0
            try {
                vPrim = readDataValue("0.0.4.3.0.255", reader);
                vSec  = readDataValue("0.0.4.6.0.255", reader);
            } catch (Exception ignored) {}
        }
        try {
            aPrim = readDataValue("1.0.0.4.2.255", reader);
            aSec  = readDataValue("1.0.0.4.5.255", reader);
        } catch (Exception e) {
            try {
                aPrim = readDataValue("0.0.4.2.0.255", reader);
                aSec  = readDataValue("0.0.4.5.0.255", reader);
            } catch (Exception ignored) {}
        }

        double vRatio = (vSec != 0) ? vPrim / vSec : 1.0;
        double aRatio = (aSec != 0) ? aPrim / aSec : 1.0;

        sb.append(String.format("Relación de Transformacion de Tensión : [deciVolts]/[deciVolts] %n"));
        sb.append(String.format("Prim/sec  (%.0f / %.0f) = %,.3f%n", vPrim, vSec, vRatio));
        sb.append(String.format("Relación de Transformacion Corriente : [deciAmps]/[deciAmps]%n"));
        sb.append(String.format("Prim/sec (%.0f / %.0f) = %,.3f%n", aPrim, aSec, aRatio));

        // ── Potencias por fase ────────────────────────────────────────────────
        double p1p = 0, p2p = 0, p3p = 0;
        double p1m = 0, p2m = 0, p3m = 0;
        double q1p = 0, q2p = 0, q3p = 0;
        double q1m = 0, q2m = 0, q3m = 0;

        double kFactor = Math.pow(10, scalerW) / 1000.0; // W → kW
        double kFactorQ = Math.pow(10, scalerVAr) / 1000.0;

        try { p1p = readRegisterRaw("1.0.21.7.0.255", reader) * kFactor; } catch (Exception ignored) {}
        try { p2p = readRegisterRaw("1.0.41.7.0.255", reader) * kFactor; } catch (Exception ignored) {}
        try { p3p = readRegisterRaw("1.0.61.7.0.255", reader) * kFactor; } catch (Exception ignored) {}

        try { p1m = readRegisterRaw("1.0.22.7.0.255", reader) * kFactor; } catch (Exception ignored) {}
        try { p2m = readRegisterRaw("1.0.42.7.0.255", reader) * kFactor; } catch (Exception ignored) {}
        try { p3m = readRegisterRaw("1.0.62.7.0.255", reader) * kFactor; } catch (Exception ignored) {}

        try { q1p = readRegisterRaw("1.0.23.7.0.255", reader) * kFactorQ; } catch (Exception ignored) {}
        try { q2p = readRegisterRaw("1.0.43.7.0.255", reader) * kFactorQ; } catch (Exception ignored) {}
        try { q3p = readRegisterRaw("1.0.63.7.0.255", reader) * kFactorQ; } catch (Exception ignored) {}

        try { q1m = readRegisterRaw("1.0.24.7.0.255", reader) * kFactorQ; } catch (Exception ignored) {}
        try { q2m = readRegisterRaw("1.0.44.7.0.255", reader) * kFactorQ; } catch (Exception ignored) {}
        try { q3m = readRegisterRaw("1.0.64.7.0.255", reader) * kFactorQ; } catch (Exception ignored) {}

        double totalPp = p1p + p2p + p3p;
        double totalPm = p1m + p2m + p3m;
        double totalQp = q1p + q2p + q3p;
        double totalQm = q1m + q2m + q3m;

        sb.append(String.format("Valores       P+ [Kw]    P- [Kw]    Q+ [Kvar]  Q- [Kvar]  :%n"));
        sb.append(String.format("Fase 1 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p1p, p1m, q1p, q1m));
        sb.append(String.format("Fase 2 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p2p, p2m, q2p, q2m));
        sb.append(String.format("Fase 3 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p3p, p3m, q3p, q3m));
        sb.append(String.format("Total  :       %,.3f      %,.3f      %,.3f      %,.3f%n", totalPp, totalPm, totalQp, totalQm));

        // ── Energías ──────────────────────────────────────────────────────────
        // Leer scaler de energía desde atrib 3
        int scalerEA = readScalerEnergy("1.0.1.8.0.255", reader);
        int scalerEQ = readScalerEnergy("1.0.5.8.0.255", reader);
        double kEA = Math.pow(10, scalerEA) / 1000.0; // Wh → kWh
        double kEQ = Math.pow(10, scalerEQ) / 1000.0;

        double eaPlus = 0, eaMinus = 0;
        double eq1 = 0, eq2 = 0, eq3 = 0, eq4 = 0;

        try { eaPlus  = readRegisterRaw("1.0.1.8.0.255", reader) * kEA; } catch (Exception ignored) {}
        try { eaMinus = readRegisterRaw("1.0.2.8.0.255", reader) * kEA; } catch (Exception ignored) {}
        try { eq1 = readRegisterRaw("1.0.5.8.0.255", reader) * kEQ; } catch (Exception ignored) {}
        try { eq2 = readRegisterRaw("1.0.6.8.0.255", reader) * kEQ; } catch (Exception ignored) {}
        try { eq3 = readRegisterRaw("1.0.7.8.0.255", reader) * kEQ; } catch (Exception ignored) {}
        try { eq4 = readRegisterRaw("1.0.8.8.0.255", reader) * kEQ; } catch (Exception ignored) {}

        sb.append(String.format("Activa Importada   :   %,.3f  [Kwh]%n", eaPlus));
        sb.append(String.format("Activa Exportada   :   %,.3f  [Kwh]%n", eaMinus));
        sb.append(String.format("Reactiva Q1        :   %,.3f  [KVArh]%n", eq1));
        sb.append(String.format("Reactiva Q2        :   %,.3f  [KVArh]%n", eq2));
        sb.append(String.format("Reactiva Q3        :   %,.3f  [KVArh]%n", eq3));
        sb.append(String.format("Reactiva Q4        :   %,.3f  [KVArh]%n", eq4));

        return sb.toString();
    }

// ── Helpers ───────────────────────────────────────────────────────────────

    private static double readRegisterRaw(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 2);
        Object value = reg.getValue();
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static double readDataValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSData data = new GXDLMSData(obis);
        reader.read(data, 2);
        Object value = data.getValue();
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static int readScaler(String obis, GXDLMSReader reader) {
        try {
            GXDLMSRegister reg = new GXDLMSRegister(obis);
            reader.read(reg, 3);
            return (int) reg.getScaler();
        } catch (Exception e) {
            return 0;
        }
    }

    private static int readScalerEnergy(String obis, GXDLMSReader reader) {
        try {
            GXDLMSRegister reg = new GXDLMSRegister(obis);
            reader.read(reg, 3);
            return (int) reg.getScaler();
        } catch (Exception e) {
            return 0;
        }
    }
}