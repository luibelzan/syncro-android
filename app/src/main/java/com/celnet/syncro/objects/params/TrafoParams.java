package com.celnet.syncro.objects.params;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.enums.DataType;
import gurux.dlms.objects.GXDLMSData;

/**
 * Relación de transformación (TI/TT) del contador.
 *
 * Cuatro objetos Data (clase 1, atributo 2, long-unsigned):
 *   1.0.0.4.2.255  corriente  numerador   (primario)
 *   1.0.0.4.5.255  corriente  denominador (secundario)
 *   1.0.0.4.3.255  tensión    numerador   (primario)
 *   1.0.0.4.6.255  tensión    denominador (secundario)
 * Los valores van en décimas (deciAmps / deciVolts).
 *
 * Al programar se escribe primero el denominador y después el numerador, igual que
 * el software de referencia (verificado con un contador ZIV).
 */
public class TrafoParams {

    private static final String TAG = "TrafoParams";

    private static final String OBIS_CORRIENTE_NUM = "1.0.0.4.2.255";
    private static final String OBIS_CORRIENTE_DEN = "1.0.0.4.5.255";
    private static final String OBIS_TENSION_NUM   = "1.0.0.4.3.255";
    private static final String OBIS_TENSION_DEN   = "1.0.0.4.6.255";

    /** Máximo de un long-unsigned (UINT16). */
    public static final int MAX_VALOR = 65535;

    public static class RelacionesResult {
        // -1 = no se pudo leer
        public int tensionPrimario = -1;
        public int tensionSecundario = -1;
        public int corrientePrimario = -1;
        public int corrienteSecundario = -1;

        public boolean algunValor() {
            return tensionPrimario >= 0 || tensionSecundario >= 0
                    || corrientePrimario >= 0 || corrienteSecundario >= 0;
        }
    }

    public static RelacionesResult leerRelaciones(GXDLMSReader reader) {
        RelacionesResult r = new RelacionesResult();
        r.corrientePrimario    = leerValor(reader, OBIS_CORRIENTE_NUM, "RT corriente numerador");
        r.corrienteSecundario  = leerValor(reader, OBIS_CORRIENTE_DEN, "RT corriente denominador");
        r.tensionPrimario      = leerValor(reader, OBIS_TENSION_NUM,   "RT tensión numerador");
        r.tensionSecundario    = leerValor(reader, OBIS_TENSION_DEN,   "RT tensión denominador");
        return r;
    }

    private static int leerValor(GXDLMSReader reader, String obis, String nombre) {
        try {
            AppLogger.i(TAG, "Reading " + nombre);
            GXDLMSData obj = new GXDLMSData(obis);
            reader.read(obj, 2);
            Object val = obj.getValue();
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            if (val instanceof byte[]) {
                int v = 0;
                for (byte x : (byte[]) val) v = (v << 8) | (x & 0xFF);
                return v;
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Error reading " + nombre + ": " + e.getMessage());
        }
        return -1;
    }

    public static class ProgramarResult {
        public boolean corrienteOk = true;
        public boolean tensionOk = true;
        public String errorCorriente = null;
        public String errorTension = null;

        public boolean todoOk() {
            return corrienteOk && tensionOk;
        }
    }

    /**
     * Programa solo las relaciones cuyos dos valores no son null: corriente si se pasan
     * ambos valores de corriente, tensión si se pasan ambos de tensión.
     * Los valores deben estar entre 1 y {@link #MAX_VALOR}.
     */
    public static ProgramarResult programarRelaciones(GXDLMSReader reader,
                                                      Integer tensionPrimario,
                                                      Integer tensionSecundario,
                                                      Integer corrientePrimario,
                                                      Integer corrienteSecundario) {
        ProgramarResult result = new ProgramarResult();

        if (corrientePrimario != null && corrienteSecundario != null) {
            try {
                AppLogger.i(TAG, "Programando RT – corriente");
                escribirPar(reader, OBIS_CORRIENTE_DEN, corrienteSecundario,
                        OBIS_CORRIENTE_NUM, corrientePrimario);
                AppLogger.i(TAG, "RT – corriente = " + corrientePrimario + " / "
                        + corrienteSecundario + " [deciAmps]");
            } catch (Exception e) {
                AppLogger.e(TAG, "Error escribiendo RT corriente: " + e.getMessage());
                result.corrienteOk = false;
                result.errorCorriente = e.getMessage();
            }
        }

        if (tensionPrimario != null && tensionSecundario != null) {
            try {
                AppLogger.i(TAG, "Programando RT – Tensión");
                escribirPar(reader, OBIS_TENSION_DEN, tensionSecundario,
                        OBIS_TENSION_NUM, tensionPrimario);
                AppLogger.i(TAG, "RT – Tensión = " + tensionPrimario + " / "
                        + tensionSecundario + " [deciVolts]");
            } catch (Exception e) {
                AppLogger.e(TAG, "Error escribiendo RT tensión: " + e.getMessage());
                result.tensionOk = false;
                result.errorTension = e.getMessage();
            }
        }

        return result;
    }

    /** Escribe denominador y después numerador. */
    private static void escribirPar(GXDLMSReader reader,
                                    String obisDenominador, int denominador,
                                    String obisNumerador, int numerador) throws Exception {
        if (denominador < 1 || denominador > MAX_VALOR || numerador < 1 || numerador > MAX_VALOR) {
            throw new IllegalArgumentException("Los valores deben estar entre 1 y " + MAX_VALOR);
        }
        escribirUint16(reader, obisDenominador, denominador);
        escribirUint16(reader, obisNumerador, numerador);
    }

    private static void escribirUint16(GXDLMSReader reader, String obis, int valor) throws Exception {
        GXDLMSData obj = new GXDLMSData(obis);
        obj.setDataType(2, DataType.UINT16);
        obj.setValue(valor);
        reader.writeObject(obj, 2);
    }
}