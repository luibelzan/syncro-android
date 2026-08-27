package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSRegister;

public class DifferentialCurrentDetectionWriter {

    private static final String OBIS_UMBRAL_VARIACION = "1.0.91.35.0.255";
    private static final String OBIS_UMBRAL_TIEMPO = "1.0.91.44.0.255";
    private static final String OBIS_UMBRAL_CORRIENTE_MINIMA = "1.0.91.35.1.255";
    // Nota: 1-0:94.34.40.255 (corriente diferencial medida) es de solo
    // lectura según la documentación (columna de acceso "R-" en todos los
    // atributos), por eso no se programa aquí.

    /**
     * Escribe los tres umbrales configurables de Differential Current
     * Detection. Solo se escribe el atributo 2 (value) de cada Register;
     * el atributo 3 (scaler_unit) es de solo lectura según la documentación
     * y se usa únicamente para convertir el valor escalado que introduce
     * el usuario al valor crudo (raw) que espera el contador.
     */
    public static void programarDifferentialCurrentDetection(
            GXDLMSReader reader,
            double umbralVariacionPorcentaje,
            int umbralTiempoSegundos,
            double umbralCorrienteMinimaAmperios
    ) throws Exception {

        AppLogger.i("DifferentialCurrent", "Programando Differential Current Detection...");

        escribirValorEscalado(OBIS_UMBRAL_VARIACION, umbralVariacionPorcentaje, reader);
        escribirValorEscalado(OBIS_UMBRAL_TIEMPO, umbralTiempoSegundos, reader);
        escribirValorEscalado(OBIS_UMBRAL_CORRIENTE_MINIMA, umbralCorrienteMinimaAmperios, reader);

        AppLogger.i("DifferentialCurrent", "Programación completada correctamente.");
    }

    /**
     * Lee el scaler actual del contador (atributo 3) para ese registro,
     * convierte el valor escalado (el que ve/introduce el usuario) al
     * valor crudo esperado por el atributo 2, y lo escribe.
     */
    private static void escribirValorEscalado(String obis, double valorEscalado,
                                              GXDLMSReader reader) throws Exception {

        GXDLMSRegister reg = new GXDLMSRegister(obis);
        reader.read(reg, 3); // scaler_unit, necesario para la conversión

        int scalerInt = (int) reg.getScaler();
        if (scalerInt > 127) scalerInt -= 256;

        long raw = Math.round(valorEscalado / Math.pow(10, scalerInt));

        reg.setValue(raw);
        reg.setDataType(2, DataType.UINT16); // "long-unsigned" en DLMS = UInt16

        reader.writeObject(reg, 2);

        AppLogger.d("DifferentialCurrent", String.format(
                "OBIS %s  valorEscalado=%f  scaler=%d  raw_escrito=%d",
                obis, valorEscalado, scalerInt, raw));
    }
}