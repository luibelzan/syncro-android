package com.celnet.syncro.utils;

import java.util.Locale;

/**
 * Utilidad compartida para formatear valores numéricos que llegan como
 * String desde los readers DLMS, tolerando tanto coma como punto como
 * separador decimal (y detectando cuál es el separador de miles a
 * eliminar en cada caso). Usado tanto en la generación de reportes XML
 * como en la tabla en pantalla, para que ambos coincidan.
 */
public final class NumeroFormatUtils {

    private NumeroFormatUtils() {
    }

    /**
     * Parsea un número que puede venir en formato "1234,56" (coma decimal),
     * "1234.56" (punto decimal), o incluso con separador de miles mezclado
     * (p.ej. "1.234,56" o "1,234.56"). Detecta el separador decimal real
     * como el que aparece más a la derecha del valor, y trata el otro
     * símbolo (si aparece) como separador de miles a eliminar.
     *
     * @param valor      valor crudo tal como llega de la fila (puede ser null)
     * @param decimales  número de decimales deseado en el resultado (0, 1, ...)
     * @return el número formateado con punto como separador decimal, o el
     *         valor original si no se pudo interpretar como número.
     */
    public static String formatearNumero(String valor, int decimales) {
        if (valor == null) {
            return "";
        }

        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return limpio;
        }

        int posComa = limpio.lastIndexOf(',');
        int posPunto = limpio.lastIndexOf('.');

        String normalizado;
        if (posComa > posPunto) {
            // La coma es el separador decimal; el punto (si existe) es de miles.
            normalizado = limpio.replace(".", "").replace(',', '.');
        } else if (posPunto > posComa) {
            // El punto es el separador decimal; la coma (si existe) es de miles.
            normalizado = limpio.replace(",", "");
        } else {
            // No hay ni coma ni punto: valor entero o ya normalizado.
            normalizado = limpio;
        }

        try {
            double numero = Double.parseDouble(normalizado);
            return String.format(Locale.US, "%." + decimales + "f", numero);
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    public static String sinDecimales(String valor) {
        return formatearNumero(valor, 0);
    }

    public static String unDecimal(String valor) {
        return formatearNumero(valor, 1);
    }
}