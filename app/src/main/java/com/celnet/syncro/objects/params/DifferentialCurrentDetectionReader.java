package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.models.parameters.DifferentialCurrentDetectionInfo;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.objects.GXDLMSRegister;

public class DifferentialCurrentDetectionReader {

    // OBIS según documentación oficial "9.10 Differential current detection"
    private static final String OBIS_UMBRAL_VARIACION = "1.0.91.35.0.255";
    private static final String OBIS_UMBRAL_TIEMPO = "1.0.91.44.0.255";
    private static final String OBIS_UMBRAL_CORRIENTE_MINIMA = "1.0.91.35.1.255";
    private static final String OBIS_CORRIENTE_DIFERENCIAL = "1.0.94.34.40.255";

    public static DifferentialCurrentDetectionInfo leerDifferentialCurrentDetection(
            GXDLMSReader reader) throws Exception {

        AppLogger.i("DifferentialCurrent", "Leyendo Differential Current Detection...");

        DifferentialCurrentDetectionInfo info = new DifferentialCurrentDetectionInfo();

        info.setUmbralVariacionPorcentaje(leerValorEscalado(OBIS_UMBRAL_VARIACION, reader));
        info.setUmbralTiempoSegundos((int) Math.round(leerValorEscalado(OBIS_UMBRAL_TIEMPO, reader)));
        info.setUmbralCorrienteMinimaAmperios(leerValorEscalado(OBIS_UMBRAL_CORRIENTE_MINIMA, reader));
        info.setCorrienteDiferencialActualAmperios(leerValorEscalado(OBIS_CORRIENTE_DIFERENCIAL, reader));

        AppLogger.i("DifferentialCurrent", "Lectura completada:\n" + info.toString());

        return info;
    }

    /**
     * Lee el atributo 3 (scaler_unit) y el atributo 2 (value) de un Register,
     * y devuelve el valor ya escalado (raw * 10^scaler). Mismo criterio que
     * readScaledValue() en InstantaneousValuesReader.
     */
    private static double leerValorEscalado(String obis, GXDLMSReader reader) throws Exception {
        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 3); // scaler_unit
        reader.read(reg, 2); // value

        Object value = reg.getValue();
        double raw = 0;
        if (value instanceof Number) {
            raw = ((Number) value).doubleValue();
        } else if (value != null) {
            raw = Double.parseDouble(value.toString());
        }

        int scalerInt = (int) reg.getScaler();
        if (scalerInt > 127) scalerInt -= 256;

        double resultado = raw * Math.pow(10, scalerInt);

        AppLogger.d("DifferentialCurrent", String.format(
                "OBIS %s  raw=%.0f  scaler=%d  resultado=%f", obis, raw, scalerInt, resultado));

        return resultado;
    }
}