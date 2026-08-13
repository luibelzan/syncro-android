package com.celnet.syncro.models.prime;

public class PrimeSecurityData {

    // Máscara de 16 bits con la constelación/codificación (bit 0 = LSB)
    public int constellationCodingRaw;

    // Tamaño SAR (código numérico devuelto por el medidor)
    public int sarSizeRaw;

    // ARQ habilitado/deshabilitado
    public boolean arqEnabled;

    // Versión de Dual Stack Prime (1, 2 o 3)
    public int dualStackVersionRaw;

    private static final String[] BIT_LABELS = {
            "DBPSK", "RES", "DQPSK", "D8PSK", "RES", "DBPSK_C", "DQPSK_C", "D8PSK_C",
            "RES", "RES", "RES", "RES", "R_DBPSK", "R_DQPSK", "RES", "RES"
    };

    /**
     * Genera el informe de texto con el mismo formato que se ve en el log de lectura.
     */
    public String generarReporte() {
        StringBuilder sb = new StringBuilder();

        sb.append("Prime 1.4 Constellation Coding :\n");
        for (int i = 0; i < BIT_LABELS.length; i++) {
            int bit = (constellationCodingRaw >> i) & 1;
            sb.append(" ")
                    .append(String.format("%-8s", BIT_LABELS[i]))
                    .append(": ")
                    .append(bit)
                    .append("\n");
        }

        sb.append("Prime 1.4 SARSize : ").append(formatearSarSize(sarSizeRaw)).append("\n");

        sb.append("Prime 1.4 ARQ ").append(arqEnabled ? "Enabled" : "Disabled").append("\n");

        sb.append("Prime 1.4 Dualstackversion : ").append(formatearDualStackVersion(dualStackVersionRaw));

        return sb.toString();
    }

    private String formatearSarSize(int codigo) {
        // TODO: completa el resto de códigos si el fabricante documenta más valores.
        switch (codigo) {
            case 0:
                return "(0) Not mandated by BN";
            default:
                return "(" + codigo + ") Código no reconocido";
        }
    }

    private String formatearDualStackVersion(int codigo) {
        switch (codigo) {
            case 1:
                return "(1) Communications in Prime 1.3.6 mode";
            case 2:
                return "(2) Communications in Prime 1.4 mode";
            case 3:
                return "(3) Dynamic communications 1.3.6 or 1.4";
            default:
                return "(" + codigo + ") Código no reconocido";
        }
    }
}