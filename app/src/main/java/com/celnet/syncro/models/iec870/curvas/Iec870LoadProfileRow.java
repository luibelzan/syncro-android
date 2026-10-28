package com.celnet.syncro.models.iec870.curvas;

import java.io.Serializable;

/**
 * Un registro de curva de carga IEC-870: un instante y sus 6 totales integrados
 * (incrementos de energía).
 */
public class Iec870LoadProfileRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** "yyyy/MM/dd HH:mm:ss" + 'S' (horario de verano) o 'W' (invierno). */
    public final String fechaHora;
    public final boolean verano;

    public final long ai;  // dirección 1: activa importada
    public final long ae;  // dirección 2: activa exportada
    public final long r1;  // dirección 3: reactiva cuadrante 1
    public final long r2;  // dirección 4: reactiva cuadrante 2
    public final long r3;  // dirección 5: reactiva cuadrante 3
    public final long r4;  // dirección 6: reactiva cuadrante 4

    /** Byte de cualidad tal cual lo envía el contador (el de la dirección 1). */
    public final int calidad;
    /** Bit IV (bit 7) activo en cualquiera de los 6 totales. */
    public final boolean invalido;

    /** Byte de cualidad de cada total, por dirección 1..6 (índice 0 sin uso). */
    public final int[] calidades = new int[7];

    public Iec870LoadProfileRow(String fechaHora, boolean verano,
                                long ai, long ae, long r1, long r2, long r3, long r4,
                                int calidad, boolean invalido, int[] calidades) {
        this(fechaHora, verano, ai, ae, r1, r2, r3, r4, calidad, invalido);
        System.arraycopy(calidades, 0, this.calidades, 0, Math.min(calidades.length, 7));
    }

    public Iec870LoadProfileRow(String fechaHora, boolean verano,
                                long ai, long ae, long r1, long r2, long r3, long r4,
                                int calidad, boolean invalido) {
        this.fechaHora = fechaHora;
        this.verano = verano;
        this.ai = ai;
        this.ae = ae;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
        this.calidad = calidad;
        this.invalido = invalido;
        java.util.Arrays.fill(this.calidades, calidad);
    }
}