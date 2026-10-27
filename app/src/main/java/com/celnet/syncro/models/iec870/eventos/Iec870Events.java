package com.celnet.syncro.models.iec870.eventos;

import java.util.HashMap;
import java.util.Map;

/**
 * Descripciones de eventos IEC-870 (ASDU 1, "single point con etiqueta de tiempo").
 * Clave: SPA (grupo) + SPQ (código). SPI (0/1) elige el texto de inicio/fin cuando aplica.
 * Tabla tomada de la librería iec870ree (events.py).
 */
public final class Iec870Events {

    /** Registros de eventos conocidos (dirección de registro en la petición ASDU 102). */
    public static final int REG_ARRANQUES_TENSION = 52;
    public static final int REG_SINCRONIZACION_HORA = 53;
    public static final int REG_CAMBIO_PARAMETROS = 54;
    public static final int REG_ERRORES_INTERNOS = 55;
    public static final int REG_INTRUSISMO = 128;
    public static final int REG_COMUNICACIONES = 129;
    public static final int REG_CLAVE_PRIVADA = 130;
    public static final int REG_CONTRATO_1 = 131;
    public static final int REG_CONTRATO_2 = 132;
    public static final int REG_CONTRATO_3 = 133;

    /** Valor especial de la pantalla: leer todos los registros de eventos. */
    public static final int REG_TODOS = -1;

    /** Todos los registros de eventos, en el orden en que se leen con "Todos los eventos". */
    public static final int[] TODOS_LOS_REGISTROS = {
            REG_ARRANQUES_TENSION, REG_SINCRONIZACION_HORA, REG_CAMBIO_PARAMETROS,
            REG_ERRORES_INTERNOS, REG_INTRUSISMO, REG_COMUNICACIONES, REG_CLAVE_PRIVADA,
            REG_CONTRATO_1, REG_CONTRATO_2, REG_CONTRATO_3
    };

    private static final class Entry {
        final String[] spi;
        final String desc;

        Entry(String[] spi, String desc) {
            this.spi = spi;
            this.desc = desc;
        }
    }

    private static final Map<Integer, Entry> TABLE = new HashMap<>();
    private static final Map<Integer, Boolean> GRUPOS = new HashMap<>();

    static {
        put(1, 1, null, "rearranque de sistema (con pérdida de los datos anteriores al arranque)");
        put(1, 2, null, "arranque tras fallo de alimentación (se conservan datos, parámetros y hora). A usar con dirección de registro");
        put(3, 0, null, "rearranque de sistema (con pérdida de los datos anteriores al arranque)");
        put(3, 1, new String[]{"Fin", "Inicio"}, "{} fallo de tensión de medida en fase 1");
        put(3, 2, new String[]{"Fin", "Inicio"}, "{} fallo de tensión de medida en fase 2");
        put(3, 3, new String[]{"Fin", "Inicio"}, "{} fallo de tensión de medida en fase 3");
        put(7, 2, null, "desincronización (el contador del punto de medida asociado se encuentra desincronizado con respecto a su RM con una diferencia de tiempo significativa).");
        put(7, 9, null, "cambio de hora, hora anterior");
        put(7, 11, null, "cambio de hora, hora nueva");
        put(7, 21, null, "Cierre de facturación por comando del Contrato I");
        put(7, 22, null, "Cierre de facturación por comando del Contrato II");
        put(7, 23, null, "Cierre de facturación por comando del Contrato III");
        put(15, 0, null, "cambio de parámetros");
        put(15, 1, null, "Cambio de alguna característica de los puertos de comunicaciones realizada por comando");
        put(15, 21, null, "Cambio de parámetros del Contrato I");
        put(15, 22, null, "Cambio de parámetros del Contrato II");
        put(15, 23, null, "Cambio de parámetros del Contrato III");
        put(15, 24, null, "Cambio de potencia de contrato (activo o latente) I por comando");
        put(15, 25, null, "Cambio de potencia de contrato (activo o latente) II por comando");
        put(15, 26, null, "Cambio de potencia de contrato (activo o latente) III por comando");
        put(15, 27, null, "Cambio de la tabla de días festivos contrato I por comando");
        put(15, 28, null, "Cambio de la tabla de días festivos contrato II por comando");
        put(15, 29, null, "Cambio de la tabla de días festivos contrato III por comando");
        put(16, 0, null, "Cambio de la Clave Privada del registrador");
        put(18, 1, null, "incidencia de intrusismo");
        put(18, 2, new String[]{"Fin", "Inicio"}, "{} establecimiento de comunicaciones con un CM - llamada -");
        put(18, 3, new String[]{"Fin", "Inicio"}, "{} establecimiento de comunicaciones con el TPL");
        put(18, 4, new String[]{"Recuperación", "Pérdida"}, "{} de comunicación con el GPS");
        put(18, 21, null, "Establecimiento de comunicaciones para Contrato I, producido al enviar el primer ASDU con tal información dentro de cada sesión con un punto de medida");
        put(18, 22, null, "Establecimiento de comunicaciones para Contrato II, producido al enviar el primer ASDU con tal información dentro de cada sesión con un punto de medida");
        put(18, 23, null, "Establecimiento de comunicaciones para Contrato III, producido al enviar el primer ASDU con tal información dentro de cada sesión con un punto de medida");
    }

    private static void put(int spa, int spq, String[] spi, String desc) {
        TABLE.put((spa << 8) | spq, new Entry(spi, desc));
        GRUPOS.put(spa, Boolean.TRUE);
    }

    private Iec870Events() {
    }

    public static String descripcion(int spa, int spq, int spi) {
        if (!GRUPOS.containsKey(spa)) {
            return "Código de error interno, dependiente del fabricante";
        }
        Entry e = TABLE.get((spa << 8) | spq);
        if (e == null) {
            return "Código de error desconocido";
        }
        if (e.spi != null) {
            return e.desc.replace("{}", e.spi[spi & 1]);
        }
        return e.desc;
    }
}