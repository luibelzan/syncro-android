package com.celnet.syncro.models.iec870.params;

import java.io.Serializable;

/**
 * Fechas oficiales de cambio de hora que tiene configuradas el contador (ASDU 185 /
 * 131): el instante en que empieza el horario de verano y el del horario de invierno.
 */
public class Iec870DaylightChange implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Cambio a horario de verano (entrada con SU = 1). */
    public Iec870DateTime inicioVerano;
    /** Cambio a horario de invierno (entrada con SU = 0). */
    public Iec870DateTime inicioInvierno;
}