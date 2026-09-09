package com.celnet.syncro.utils;

/**
 * Lista centralizada de marcas de sonda soportadas, identificadas por el
 * nombre que anuncian por Bluetooth. Añadir una marca nueva aquí actualiza
 * automáticamente el escaneo (BluetoothScanActivity) y la conexión DLMS
 * (DLMSConnection) sin tener que tocar cada sitio por separado.
 *
 * Cada marca tiene un ID INTERNO (usado para el matching contra el nombre
 * real del dispositivo Bluetooth) y una ETIQUETA VISIBLE (lo que se muestra
 * al usuario en el desplegable de MainActivity). Normalmente coinciden,
 * pero por temas de privacidad/marca comercial a veces hace falta mostrar
 * algo distinto (p. ej. "Otros" en vez de "Bigrid") sin que eso afecte al
 * filtrado real, que sigue usando el ID interno.
 *
 * ⚠️ Esto SOLO cubre el emparejamiento/descubrimiento por nombre. El
 * protocolo de comandos propio de cada sonda (batería, MAC, etc., ver
 * EstadoSondaActivity) es independiente y debe implementarse por marca.
 */
public final class ProbeBrands {

    private ProbeBrands() {}

    public static class Marca {
        public final String idInterno;      // usado para el matching real (nombre Bluetooth)
        public final String etiquetaVisible; // lo que ve el usuario en el desplegable

        public Marca(String idInterno, String etiquetaVisible) {
            this.idInterno = idInterno;
            this.etiquetaVisible = etiquetaVisible;
        }
    }

    public static final Marca[] MARCAS_SOPORTADAS = {
            new Marca("tespro", "TesPro"),
            new Marca("bigrid", "Otros") // por privacidad no se muestra "Bigrid" en la UI
    };

    /** true si el nombre del dispositivo Bluetooth corresponde a alguna marca soportada. */
    public static boolean coincideNombreSonda(String nombreDispositivo) {
        if (nombreDispositivo == null) return false;
        String nombreLower = nombreDispositivo.toLowerCase();
        for (Marca marca : MARCAS_SOPORTADAS) {
            if (nombreLower.contains(marca.idInterno)) return true;
        }
        return false;
    }

    /** Etiquetas visibles, en el mismo orden que MARCAS_SOPORTADAS, para rellenar el desplegable. */
    public static String[] etiquetasVisibles() {
        String[] etiquetas = new String[MARCAS_SOPORTADAS.length];
        for (int i = 0; i < MARCAS_SOPORTADAS.length; i++) {
            etiquetas[i] = MARCAS_SOPORTADAS[i].etiquetaVisible;
        }
        return etiquetas;
    }

    /**
     * Traduce la etiqueta elegida en el desplegable a su ID interno real
     * (p. ej. "Otros" -> "bigrid"), que es lo que hay que guardar en
     * ConnectionConfig para que el filtrado por nombre siga funcionando.
     * Si no encuentra la etiqueta, devuelve la propia etiqueta tal cual
     * (fallback defensivo, no debería ocurrir en uso normal).
     */
    public static String idInternoParaEtiqueta(String etiquetaVisible) {
        if (etiquetaVisible == null) return null;
        for (Marca marca : MARCAS_SOPORTADAS) {
            if (marca.etiquetaVisible.equalsIgnoreCase(etiquetaVisible)) {
                return marca.idInterno;
            }
        }
        return etiquetaVisible;
    }
}