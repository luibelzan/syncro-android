package com.celnet.syncro.models.iec870.params;

/**
 * Relación de transformación del contador (ASDU 156/157, objeto 0xC6): tensión en
 * dV (deciVolts) e intensidad en dA (deciAmps), primario y secundario.
 */
public class Iec870TrafoRatio {

    public long tensionPrimariaDv;
    public long tensionSecundariaDv;
    public long corrientePrimariaDa;
    public long corrienteSecundariaDa;

    public Iec870TrafoRatio() {
    }

    public Iec870TrafoRatio(long vPrimario, long vSecundario, long iPrimario, long iSecundario) {
        this.tensionPrimariaDv = vPrimario;
        this.tensionSecundariaDv = vSecundario;
        this.corrientePrimariaDa = iPrimario;
        this.corrienteSecundariaDa = iSecundario;
    }
}