package com.example.syncro.objects.instantValues;

import com.example.syncro.client.GXDLMSReader;
import com.example.syncro.utils.MeterData;

import java.util.logging.Level;
import java.util.logging.Logger;

import gurux.dlms.objects.GXDLMSData;
import gurux.dlms.objects.GXDLMSRegister;

public class InstantaneousValuesReader {

    private static final Logger LOGGER = Logger.getLogger(InstantaneousValuesReader.class.getName());


    /**
     * Método principal que lee todos los datos del medidor
     * Replica la funcionalidad del log proporcionado
     *
     * @return MeterData con todos los valores leídos
     */
    public static MeterData readMeterData(GXDLMSReader reader) throws Exception {
        LOGGER.info("Iniciando lectura de datos del medidor...");

        MeterData meterData = new MeterData();

        // 1. Leer timestamp
        //meterData.setTimestamp(readTimestamp());

        // 2. Leer energías totales
        readEnergyValues(meterData, reader);

        // 3. Leer valores instantáneos (voltaje, corriente)
        readInstantaneousValues(meterData, reader);

        // 4. Leer relaciones de transformación
        //readTransformationRatios(meterData, reader);

        // 5. Leer factores de potencia
        readPowerFactors(meterData, reader);

        // 6. Leer potencias instantáneas
        //readInstantaneousPower(meterData, reader);

        LOGGER.info("Lectura completada exitosamente");
        return meterData;
    }



    /**
     * Lee los valores de energía acumulada
     * OBIS codes:
     * - 1.0.1.8.0.255: Activa Importada
     * - 1.0.2.8.0.255: Activa Exportada (o 1.0.5.8.0.255 según medidor)
     * - 1.0.3.8.0.255: Reactiva Q1 (o 5.0.3.8.0.255)
     * - 1.0.4.8.0.255: Reactiva Q2 (o 5.0.4.8.0.255)
     * - 1.0.7.8.0.255: Reactiva Q3 (o 5.0.7.8.0.255)
     * - 1.0.8.8.0.255: Reactiva Q4 (o 5.0.8.8.0.255)
     */
    private static void readEnergyValues(MeterData meterData, GXDLMSReader reader) throws Exception {
        LOGGER.info("Leyendo valores de energía...");

        try {
            // Activa Importada
            double activeImport = readRegisterValue("1.0.1.8.0.255", reader);
            meterData.getEnergyValues().setActiveEnergyImport(activeImport);

            // Activa Exportada - intentar diferentes OBIS codes
            double activeExport = 0;
            try {
                activeExport = readRegisterValue("1.0.2.8.0.255", reader);
            } catch (Exception e) {
                try {
                    //activeExport = readRegisterValue("1.0.5.8.0.255", reader);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "No se pudo leer activa exportada", ex);
                }
            }
            meterData.getEnergyValues().setActiveEnergyExport(activeExport);

            // Reactiva Q1-Q4
            try {
                double q1 = readRegisterValue("1.0.5.8.0.255", reader);
                meterData.getEnergyValues().setReactiveEnergyQ1(q1);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error leyendo Q1", e);
            }

            try {
                double q2 = readRegisterValue("1.0.6.8.0.255", reader);
                meterData.getEnergyValues().setReactiveEnergyQ2(q2);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error leyendo Q2", e);
            }

            try {
                double q3 = readRegisterValue("1.0.7.8.0.255", reader);
                meterData.getEnergyValues().setReactiveEnergyQ3(q3);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error leyendo Q3", e);
            }

            try {
                double q4 = readRegisterValue("1.0.8.8.0.255", reader);
                meterData.getEnergyValues().setReactiveEnergyQ4(q4);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error leyendo Q4", e);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error leyendo valores de energía", e);
            throw e;
        }
    }

    /**
     * Lee valores instantáneos de voltaje y corriente
     * OBIS codes:
     * - 1.0.32.7.0.255: Voltaje L1
     * - 1.0.52.7.0.255: Voltaje L2
     * - 1.0.72.7.0.255: Voltaje L3
     * - 1.0.31.7.0.255: Corriente L1
     * - 1.0.51.7.0.255: Corriente L2
     * - 1.0.71.7.0.255: Corriente L3
     */
    private static void readInstantaneousValues(MeterData meterData, GXDLMSReader reader) throws Exception {
        LOGGER.info("Leyendo valores instantáneos...");

        try {
            // Voltajes
            double voltageL1 = readRegisterValue("1.0.32.7.0.255", reader);
            double voltageL2 = readRegisterValue("1.0.52.7.0.255", reader);
            double voltageL3 = readRegisterValue("1.0.72.7.0.255", reader);

            meterData.getInstantaneousValues().setVoltageL1(voltageL1);
            meterData.getInstantaneousValues().setVoltageL2(voltageL2);
            meterData.getInstantaneousValues().setVoltageL3(voltageL3);

            // Corrientes
            double currentL1 = readRegisterValue("1.0.31.7.0.255", reader);
            double currentL2 = readRegisterValue("1.0.51.7.0.255", reader);
            double currentL3 = readRegisterValue("1.0.71.7.0.255", reader);

            meterData.getInstantaneousValues().setCurrentL1(currentL1);
            meterData.getInstantaneousValues().setCurrentL2(currentL2);
            meterData.getInstantaneousValues().setCurrentL3(currentL3);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error leyendo valores instantáneos", e);
            throw e;
        }
    }

    /**
     * Lee las relaciones de transformación
     * OBIS codes:
     * - 1.1.0.4.2.255: Primario corriente
     * - 1.1.0.4.5.255: Secundario corriente
     * - 1.1.0.4.3.255: Primario voltaje
     * - 1.1.0.4.6.255: Secundario voltaje
     */
    private static void readTransformationRatios(MeterData meterData, GXDLMSReader reader) throws Exception {
        LOGGER.info("Leyendo relaciones de transformación...");

        try {
            // Relación de corriente
            int currentPrimary = (int) readRegisterValue("0.0.4.2.0.255", reader);
            int currentSecondary = (int) readRegisterValue("0.0.4.5.0.255", reader);

            meterData.getTransformationRatios().setCurrentPrimary(currentPrimary);
            meterData.getTransformationRatios().setCurrentSecondary(currentSecondary);

            LOGGER.info(String.format("Relación Corriente Prim/sec (%d / %d)",
                    currentPrimary, currentSecondary));

            // Relación de voltaje
            int voltagePrimary = (int) readRegisterValue("0.0.4.3.0.255", reader);
            int voltageSecondary = (int) readRegisterValue("0.0.4.6.0.255", reader);

            meterData.getTransformationRatios().setVoltagePrimary(voltagePrimary);
            meterData.getTransformationRatios().setVoltageSecondary(voltageSecondary);

            LOGGER.info(String.format("Relación Voltaje Prim/sec (%d / %d)",
                    voltagePrimary, voltageSecondary));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error leyendo relaciones de transformación", e);
            throw e;
        }
    }

    /**
     * Lee los factores de potencia instantáneos
     * OBIS codes:
     * - 1.0.33.7.0.255: FP L1
     * - 1.0.53.7.0.255: FP L2
     * - 1.0.73.7.0.255: FP L3
     * - 1.0.13.7.0.255: FP Total
     */
    private static void readPowerFactors(MeterData meterData, GXDLMSReader reader) throws Exception {
        LOGGER.info("Leyendo factores de potencia...");

        try {
            double pfL1 = readRegisterValue("1.0.33.7.0.255", reader);
            double pfL2 = readRegisterValue("1.0.53.7.0.255", reader);
            double pfL3 = readRegisterValue("1.0.73.7.0.255", reader);

            meterData.getInstantaneousValues().setPowerFactorL1(pfL1);
            meterData.getInstantaneousValues().setPowerFactorL2(pfL2);
            meterData.getInstantaneousValues().setPowerFactorL3(pfL3);

            try {
                double pfTotal = readRegisterValue("1.0.13.7.0.255", reader);
                meterData.getInstantaneousValues().setPowerFactorTotal(pfTotal);
            } catch (Exception e) {
                // Si no está disponible, calcular promedio
                double pfTotal = (pfL1 + pfL2 + pfL3) / 3.0;
                meterData.getInstantaneousValues().setPowerFactorTotal(pfTotal);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error leyendo factores de potencia", e);
            throw e;
        }
    }

    /**
     * Lee las potencias instantáneas
     * OBIS codes para cada fase (L1, L2, L3):
     * - X.0.21.7.0.255: P+ L1
     * - X.0.22.7.0.255: P- L1
     * - X.0.23.7.0.255: Q+ L1
     * - X.0.24.7.0.255: Q- L1
     * (similar para L2 = 41-44, L3 = 61-64)
     */
    private static void readInstantaneousPower(MeterData meterData, GXDLMSReader reader) throws Exception {
        LOGGER.info("Leyendo potencias instantáneas...");

        try {
            // Fase 1
            double pPlusL1 = readRegisterValue("1.0.21.7.0.255", reader);
            double pMinusL1 = readRegisterValue("1.0.22.7.0.255", reader);
            double qPlusL1 = readRegisterValue("1.0.23.7.0.255", reader);
            double qMinusL1 = readRegisterValue("1.0.24.7.0.255", reader);

            meterData.getPowerValues().setActivePowerPlusL1(pPlusL1);
            meterData.getPowerValues().setActivePowerMinusL1(pMinusL1);
            meterData.getPowerValues().setReactivePowerPlusL1(qPlusL1);
            meterData.getPowerValues().setReactivePowerMinusL1(qMinusL1);

            // Fase 2
            double pPlusL2 = readRegisterValue("1.0.41.7.0.255", reader);
            double pMinusL2 = readRegisterValue("1.0.42.7.0.255", reader);
            double qPlusL2 = readRegisterValue("1.0.43.7.0.255", reader);
            double qMinusL2 = readRegisterValue("1.0.44.7.0.255", reader);

            meterData.getPowerValues().setActivePowerPlusL2(pPlusL2);
            meterData.getPowerValues().setActivePowerMinusL2(pMinusL2);
            meterData.getPowerValues().setReactivePowerPlusL2(qPlusL2);
            meterData.getPowerValues().setReactivePowerMinusL2(qMinusL2);

            // Fase 3
            double pPlusL3 = readRegisterValue("1.0.61.7.0.255", reader);
            double pMinusL3 = readRegisterValue("1.0.62.7.0.255", reader);
            double qPlusL3 = readRegisterValue("1.0.63.7.0.255", reader);
            double qMinusL3 = readRegisterValue("1.0.64.7.0.255", reader);

            meterData.getPowerValues().setActivePowerPlusL3(pPlusL3);
            meterData.getPowerValues().setActivePowerMinusL3(pMinusL3);
            meterData.getPowerValues().setReactivePowerPlusL3(qPlusL3);
            meterData.getPowerValues().setReactivePowerMinusL3(qMinusL3);

            // Calcular totales
            double totalPPlus = pPlusL1 + pPlusL2 + pPlusL3;
            double totalPMinus = pMinusL1 + pMinusL2 + pMinusL3;
            double totalQPlus = qPlusL1 + qPlusL2 + qPlusL3;
            double totalQMinus = qMinusL1 + qMinusL2 + qMinusL3;

            meterData.getPowerValues().setTotalActivePowerPlus(totalPPlus);
            meterData.getPowerValues().setTotalActivePowerMinus(totalPMinus);
            meterData.getPowerValues().setTotalReactivePowerPlus(totalQPlus);
            meterData.getPowerValues().setTotalReactivePowerMinus(totalQMinus);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error leyendo potencias instantáneas", e);
            throw e;
        }
    }

    /**
     * Método auxiliar para leer un valor de registro usando OBIS code
     *
     * @param obisCode Código OBIS (ej: "1.0.1.8.0.255")
     * @return Valor leído como double
     */
    private static double readRegisterValue(String obisCode, GXDLMSReader reader) throws Exception {
        try {
            GXDLMSRegister register = new GXDLMSRegister(obisCode);
            reader.read(register, 2); // Attribute 2 contiene el valor

            Object value = register.getValue();
            if (value == null) {
                return 0.0;
            }

            // Convertir a double
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }

            return 0.0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error leyendo OBIS " + obisCode, e);
            throw e;
        }
    }

    /**
     * Obtiene información del medidor
     */
    public String getMeterInfo(GXDLMSReader reader) throws Exception {
        StringBuilder info = new StringBuilder();

        try {
            // Leer fabricante
            GXDLMSData manufacturer = new GXDLMSData("0.0.96.1.0.255");
            reader.read(manufacturer, 2);
            info.append("Fabricante: ").append(manufacturer.getValue()).append("");

            // Leer número de serie
            GXDLMSData serialNumber = new GXDLMSData("0.0.96.1.1.255");
            reader.read(serialNumber, 2);
            info.append("Número de Serie: ").append(serialNumber.getValue()).append("");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error obteniendo info del medidor", e);
        }

        return info.toString();
    }
}
