package com.celnet.syncro.models.iec870.instantValues;

import java.io.Serializable;

/**
 * Valores instantáneos leídos por IEC-870 (ASDU 156/157 y 162/163).
 *
 * Unidades tal como llegan del contador: tensiones en dV (décimas de voltio) e
 * intensidades en dA (décimas de amperio) de SECUNDARIO; potencias en kW / kVAr;
 * energías en kWh / kVArh. Los valores de primario se calculan con la relación
 * de transformación.
 */
public class Iec870InstantValues implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Número de serie del contador, p. ej. "C50991687". */
    public String serie = "";
    /** "yyyy/MM/dd HH:mm:00" + letra S/W, como el informe del software funcional. */
    public String fechaHora = "";

    // Relación de transformación (dV/dV y dA/dA)
    public long tensionPrimariaDv;
    public long tensionSecundariaDv;
    public long corrientePrimariaDa;
    public long corrienteSecundariaDa;

    // Tensión e intensidad de secundario por fase (índice 0 = fase 1)
    public final long[] tensionDv = new long[3];
    public final long[] corrienteDa = new long[3];
    public final boolean[] ivInvalido = new boolean[3];

    // Potencias (índice 0 = total, 1..3 = fases)
    public final long[] potenciaActiva = new long[4];
    public final long[] potenciaReactiva = new long[4];
    public final int[] factorPotenciaMilesimas = new int[4];
    public final boolean[] exportada = new boolean[4];     // activa exportada
    public final boolean[] cuadrante34 = new boolean[4];   // reactiva en Q3/Q4
    public final boolean[] potenciaInvalida = new boolean[4];

    // Energías: 0 activa importada, 1 activa exportada, 2..5 reactiva Q1..Q4
    public final long[] energia = new long[6];
    public final boolean[] energiaInvalida = new boolean[6];

    public double relacionTension() {
        return tensionSecundariaDv == 0 ? 0 : (double) tensionPrimariaDv / tensionSecundariaDv;
    }

    public double relacionCorriente() {
        return corrienteSecundariaDa == 0 ? 0 : (double) corrientePrimariaDa / corrienteSecundariaDa;
    }

    public double tensionSecundariaV(int fase) {
        return tensionDv[fase] / 10.0;
    }

    public double corrienteSecundariaA(int fase) {
        return corrienteDa[fase] / 10.0;
    }

    public double tensionPrimariaV(int fase) {
        return tensionSecundariaV(fase) * relacionTension();
    }

    public double corrientePrimariaA(int fase) {
        return corrienteSecundariaA(fase) * relacionCorriente();
    }

    /**
     * Factor de potencia. En las fases es el campo del contador (milésimas). El
     * del total se calcula a partir de P y Q totales (0 si ambas son 0): el
     * software funcional muestra 0,000 en el total aunque el campo del total
     * del contador valga lo mismo que el de las fases.
     */
    public double factorPotencia(int indice) {
        if (indice == 0) {
            double p = potenciaActiva[0];
            double q = potenciaReactiva[0];
            double s = Math.sqrt(p * p + q * q);
            return s == 0 ? 0 : p / s;
        }
        return factorPotenciaMilesimas[indice] / 1000.0;
    }

    /** Activa con signo: negativa si es exportada (y distinta de 0). */
    public long potenciaActivaConSigno(int indice) {
        return exportada[indice] ? -potenciaActiva[indice] : potenciaActiva[indice];
    }

    /** Reactiva con signo: negativa si está en Q3/Q4 (y distinta de 0). */
    public long potenciaReactivaConSigno(int indice) {
        return cuadrante34[indice] ? -potenciaReactiva[indice] : potenciaReactiva[indice];
    }
}