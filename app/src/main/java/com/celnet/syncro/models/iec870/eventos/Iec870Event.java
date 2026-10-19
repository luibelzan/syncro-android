package com.celnet.syncro.models.iec870.eventos;

/** Un evento IEC-870 (ASDU 1): grupo (SPA), código (SPQ), inicio/fin (SPI) y fecha. */
public class Iec870Event {

    /** Fecha en el mismo formato que los eventos DLMS: "d/M/yy H:mm:ss" (p. ej. "15/10/26 10:44:04"). */
    public final String fechaHora;
    public final boolean verano;
    public final int spa;
    public final int spq;
    public final int spi;
    public final String descripcion;

    public Iec870Event(String fechaHora, boolean verano, int spa, int spq, int spi,
                       String descripcion) {
        this.fechaHora = fechaHora;
        this.verano = verano;
        this.spa = spa;
        this.spq = spq;
        this.spi = spi;
        this.descripcion = descripcion;
    }
}