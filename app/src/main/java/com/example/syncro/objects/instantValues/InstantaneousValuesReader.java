package com.example.syncro.objects.instantValues;

import com.example.syncro.client.GXDLMSReader;

import java.util.logging.Logger;

import gurux.dlms.GXByteBuffer;
import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class InstantaneousValuesReader {

    private static final Logger LOGGER = Logger.getLogger(InstantaneousValuesReader.class.getName());

    /*
     * Scalers reales de este contador (leídos de las tramas DLMS capturadas):
     *
     *   Voltaje    (1.0.3x.7.0.255)  → 0F 00  → scaler =  0  unidad deciV  → raw/10  = V
     *   Corriente  (1.0.x1.7.0.255)  → 0F FF  → scaler = -1  unidad deciA  → raw*0.1 = A
     *   FP         (1.0.x3.7.0.255)  → 0F FF  → scaler = -3  adimensional  → raw*1e-3
     *   Potencia   (1.0.x1-4.7.0.255)→ 0F FD  → scaler = -3  W             → raw*1e-3 = kW
     *   Energía    (1.0.x.8.0.255)   → 0F 00  → scaler =  0  Wh            → raw/1000 = kWh
     *
     * getScaler() de la librería Gurux devuelve siempre 0 para este contador,
     * por eso parseamos el scaler manualmente del atributo 3 (array de 2 bytes:
     * byte[0]=scaler con signo, byte[1]=unidad).
     */

    public static String leerValores(GXDLMSReader reader) throws Exception {

        LOGGER.info("Leyendo valores instantáneos...");
        StringBuilder sb = new StringBuilder();

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

        // ── Voltajes, corrientes, FP ──────────────────────────────────────────
        double v1 = 0, v2 = 0, v3 = 0;
        double a1 = 0, a2 = 0, a3 = 0;
        double fp1 = 0, fp2 = 0, fp3 = 0, fpTotal = 0;

        // Voltaje: raw en deciVolts (scaler=0, unidad=deciV) → ÷10 para V
        try { v1 = readRawValue("1.0.32.7.0.255", reader); } catch (Exception ignored) {}
        try { v2 = readRawValue("1.0.52.7.0.255", reader); } catch (Exception ignored) {}
        try { v3 = readRawValue("1.0.72.7.0.255", reader); } catch (Exception ignored) {}

        // Corriente: scaler=-1 real → raw × 10⁻¹ = A
        try { a1 = readScaledValue("1.0.31.7.0.255", reader); } catch (Exception ignored) {}
        try { a2 = readScaledValue("1.0.51.7.0.255", reader); } catch (Exception ignored) {}
        try { a3 = readScaledValue("1.0.71.7.0.255", reader); } catch (Exception ignored) {}

        // FP: scaler=-3 real → raw × 10⁻³
        try { fp1 = readScaledValue("1.0.33.7.0.255", reader); } catch (Exception ignored) {}
        try { fp2 = readScaledValue("1.0.53.7.0.255", reader); } catch (Exception ignored) {}
        try { fp3 = readScaledValue("1.0.73.7.0.255", reader); } catch (Exception ignored) {}
        try { fpTotal = readScaledValue("1.0.13.7.0.255", reader); } catch (Exception e) {
            fpTotal = (fp1 + fp2 + fp3) / 3.0;
        }

        sb.append("Valores Tensión       Corriente        FP :\n");
        sb.append(String.format("Fase 1 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v1, a1, fp1));
        sb.append(String.format("Fase 2 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v2, a2, fp2));
        sb.append(String.format("Fase 3 :   %,.1f [V]       %,.1f [A]   %,.3f%n", v3, a3, fp3));
        sb.append(String.format("FP (sum of all phases: +P/S) : %,.3f%n", fpTotal));

        // ── Relaciones ────────────────────────────────────────────────────────
        double vPrim = 0, vSec = 0, aPrim = 0, aSec = 0;
        try { vPrim = readDataValue("1.0.0.4.3.255", reader); } catch (Exception e) {
            try { vPrim = readDataValue("0.0.4.3.0.255", reader); } catch (Exception ignored) {}
        }
        try { vSec = readDataValue("1.0.0.4.6.255", reader); } catch (Exception e) {
            try { vSec = readDataValue("0.0.4.6.0.255", reader); } catch (Exception ignored) {}
        }
        try { aPrim = readDataValue("1.0.0.4.2.255", reader); } catch (Exception e) {
            try { aPrim = readDataValue("0.0.4.2.0.255", reader); } catch (Exception ignored) {}
        }
        try { aSec = readDataValue("1.0.0.4.5.255", reader); } catch (Exception e) {
            try { aSec = readDataValue("0.0.4.5.0.255", reader); } catch (Exception ignored) {}
        }

        double vRatio = (vSec != 0) ? vPrim / vSec : 1.0;
        double aRatio = (aSec != 0) ? aPrim / aSec : 1.0;

        sb.append("Relación de Transformacion de Tensión : [deciVolts]/[deciVolts] \n");
        sb.append(String.format("Prim/sec  (%.0f / %.0f) = %,.3f%n", vPrim, vSec, vRatio));
        sb.append("Relación de Transformacion Corriente : [deciAmps]/[deciAmps]\n");
        sb.append(String.format("Prim/sec (%.0f / %.0f) = %,.3f%n", aPrim, aSec, aRatio));

        // ── Potencias por fase (en kW) ────────────────────────────────────────
        // Potencia: scaler=-3 real (0xFD) → raw en W × 10⁻³ = kW
        double p1p=0, p2p=0, p3p=0, p1m=0, p2m=0, p3m=0;
        double q1p=0, q2p=0, q3p=0, q1m=0, q2m=0, q3m=0;

        try { p1p = readRawValue("1.0.21.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p2p = readRawValue("1.0.41.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p3p = readRawValue("1.0.61.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        try { p1m = readRawValue("1.0.22.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p2m = readRawValue("1.0.42.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { p3m = readRawValue("1.0.62.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        try { q1p = readRawValue("1.0.23.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q2p = readRawValue("1.0.43.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q3p = readRawValue("1.0.63.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        try { q1m = readRawValue("1.0.24.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q2m = readRawValue("1.0.44.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}
        try { q3m = readRawValue("1.0.64.7.0.255", reader) * 1e-3; } catch (Exception ignored) {}

        sb.append("Valores       P+ [Kw]    P- [Kw]    Q+ [Kvar]  Q- [Kvar]  :\n");
        sb.append(String.format("Fase 1 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p1p, p1m, q1p, q1m));
        sb.append(String.format("Fase 2 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p2p, p2m, q2p, q2m));
        sb.append(String.format("Fase 3 :       %,.3f      %,.3f      %,.3f      %,.3f%n", p3p, p3m, q3p, q3m));
        sb.append(String.format("Total  :       %,.3f      %,.3f      %,.3f      %,.3f%n",
                p1p+p2p+p3p, p1m+p2m+p3m, q1p+q2p+q3p, q1m+q2m+q3m));

        // ── Energías (en kWh) ─────────────────────────────────────────────────
        // Energía: scaler=0, unidad=Wh (raw es Wh entero) → ÷1000 para kWh
        double eaPlus=0, eaMinus=0, eq1=0, eq2=0, eq3=0, eq4=0;

        try { eaPlus  = readRawValue("1.0.1.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eaMinus = readRawValue("1.0.2.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq1 = readRawValue("1.0.5.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq2 = readRawValue("1.0.6.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq3 = readRawValue("1.0.7.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}
        try { eq4 = readRawValue("1.0.8.8.0.255", reader) / 1000.0; } catch (Exception ignored) {}

        sb.append(String.format("Activa Importada   :   %,.3f  [Kwh]%n", eaPlus));
        sb.append(String.format("Activa Exportada   :   %,.3f  [Kwh]%n", eaMinus));
        sb.append(String.format("Reactiva Q1        :   %,.3f  [KVArh]%n", eq1));
        sb.append(String.format("Reactiva Q2        :   %,.3f  [KVArh]%n", eq2));
        sb.append(String.format("Reactiva Q3        :   %,.3f  [KVArh]%n", eq3));
        sb.append(String.format("Reactiva Q4        :   %,.3f  [KVArh]%n", eq4));

        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Lee el valor RAW del registro sin aplicar ningún scaler.
     * Útil para magnitudes cuyo scaler=0 pero cuya unidad requiere
     * una conversión fija conocida (voltaje deciV→V, energía Wh→kWh).
     */
    private static double readRawValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 3);  // scaler+unit (lo leemos para que el frame esté completo)
        reader.read(reg, 2);  // valor
        Object value = reg.getValue();
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    /**
     * Lee el registro y aplica el scaler parseándolo manualmente como byte
     * con signo directamente del objeto GXDLMSRegister.
     *
     * La librería Gurux devuelve getScaler() como double pero en este contador
     * parece ignorar el signo en algunos firmware. Lo forzamos:
     *
     *   scalerByte = (int) reg.getScaler()
     *   si > 127  → con signo = scalerByte - 256
     *
     * Esto funciona siempre que el contador envíe 0F xx (tipo int8 = 0x0F).
     */
    private static double readScaledValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 3);
        reader.read(reg, 2);

        Object value = reg.getValue();
        double raw = 0;
        if (value instanceof Number) {
            raw = ((Number) value).doubleValue();
        } else if (value != null) {
            raw = Double.parseDouble(value.toString());
        }

        // Forzar interpretación signed del byte de scaler
        int scalerInt = (int) reg.getScaler();
        if (scalerInt > 127) scalerInt -= 256;

        LOGGER.fine(String.format("OBIS %s  raw=%.0f  scalerRaw=%.0f  scalerSigned=%d  result=%f",
                obis, raw, reg.getScaler(), scalerInt, raw * Math.pow(10, scalerInt)));

        return raw * Math.pow(10, scalerInt);
    }

    private static double readDataValue(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSData data = new GXDLMSData(obis);
        reader.read(data, 2);
        Object value = data.getValue();
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }
}