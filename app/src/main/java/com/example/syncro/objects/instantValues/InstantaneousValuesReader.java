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
    public static InstantaneousValues leerValores(GXDLMSReader reader) throws Exception {

        LOGGER.info("Leyendo valores instantáneos...");

        InstantaneousValues datos = new InstantaneousValues();

        try {
            // 🔹 Serial
            try {
                GXDLMSData serial = new GXDLMSData("0.0.96.1.1.255");
                reader.read(serial, 2);
                datos.serial = String.valueOf(serial.getValue());
            } catch (Exception e) {
                LOGGER.warning("No se pudo leer el serial");
            }

            // 🔹 Timestamp
            try {
                GXDLMSData time = new GXDLMSData("0.0.1.0.0.255");
                reader.read(time, 2);
                datos.fechaHora = String.valueOf(time.getValue());
            } catch (Exception e) {
                LOGGER.warning("No se pudo leer la fecha");
            }

            // =========================
            // 🔹 VOLTAJES
            // =========================
            double v1 = readRegisterValue("1.0.32.7.0.255", reader);
            double v2 = readRegisterValue("1.0.52.7.0.255", reader);
            double v3 = readRegisterValue("1.0.72.7.0.255", reader);

            datos.vFase1 = String.valueOf(v1);
            datos.vFase2 = String.valueOf(v2);
            datos.vFase3 = String.valueOf(v3);

            // =========================
            // 🔹 CORRIENTES
            // =========================
            double a1 = readRegisterValue("1.0.31.7.0.255", reader);
            double a2 = readRegisterValue("1.0.51.7.0.255", reader);
            double a3 = readRegisterValue("1.0.71.7.0.255", reader);

            datos.aFase1 = String.valueOf(a1);
            datos.aFase2 = String.valueOf(a2);
            datos.aFase3 = String.valueOf(a3);

            // =========================
            // 🔹 FACTOR DE POTENCIA
            // =========================
            double fp;
            try {
                fp = readRegisterValue("1.0.13.7.0.255", reader);
            } catch (Exception e) {
                double fp1 = readRegisterValue("1.0.33.7.0.255", reader);
                double fp2 = readRegisterValue("1.0.53.7.0.255", reader);
                double fp3 = readRegisterValue("1.0.73.7.0.255", reader);
                fp = (fp1 + fp2 + fp3) / 3.0;
            }

            datos.factorPotencia = String.valueOf(fp);

            // =========================
            // 🔹 POTENCIAS (TOTALES)
            // =========================
            double pPlus = 0, pMinus = 0, qPlus = 0, qMinus = 0;

            try {
                pPlus += readRegisterValue("1.0.21.7.0.255", reader);
                pPlus += readRegisterValue("1.0.41.7.0.255", reader);
                pPlus += readRegisterValue("1.0.61.7.0.255", reader);

                pMinus += readRegisterValue("1.0.22.7.0.255", reader);
                pMinus += readRegisterValue("1.0.42.7.0.255", reader);
                pMinus += readRegisterValue("1.0.62.7.0.255", reader);

                qPlus += readRegisterValue("1.0.23.7.0.255", reader);
                qPlus += readRegisterValue("1.0.43.7.0.255", reader);
                qPlus += readRegisterValue("1.0.63.7.0.255", reader);

                qMinus += readRegisterValue("1.0.24.7.0.255", reader);
                qMinus += readRegisterValue("1.0.44.7.0.255", reader);
                qMinus += readRegisterValue("1.0.64.7.0.255", reader);

            } catch (Exception e) {
                LOGGER.warning("Error leyendo potencias");
            }

            datos.pActivaPlus = String.valueOf(pPlus);
            datos.pActivaMinus = String.valueOf(pMinus);
            datos.pReactivaPlus = String.valueOf(qPlus);
            datos.pReactivaMinus = String.valueOf(qMinus);

            // =========================
            // 🔹 RELACIONES (opcional)
            // =========================
            try {
                int currentPrimary = (int) readRegisterValue("0.0.4.2.0.255", reader);
                int currentSecondary = (int) readRegisterValue("0.0.4.5.0.255", reader);

                datos.aRelation = currentPrimary + "/" + currentSecondary;

                int voltagePrimary = (int) readRegisterValue("0.0.4.3.0.255", reader);
                int voltageSecondary = (int) readRegisterValue("0.0.4.6.0.255", reader);

                datos.volRelation = voltagePrimary + "/" + voltageSecondary;

            } catch (Exception e) {
                LOGGER.warning("No se pudieron leer relaciones");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error general leyendo valores", e);
            throw e;
        }

        return datos;
    }

    /**
     * 🔹 Método auxiliar (lo mantenemos igual)
     */
    private static double readRegisterValue(String obisCode, GXDLMSReader reader) throws Exception {
        try {
            GXDLMSRegister register = new GXDLMSRegister(obisCode);
            reader.read(register, 2);

            Object value = register.getValue();
            if (value == null) return 0.0;

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
}