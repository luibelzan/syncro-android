package com.celnet.syncro.models.iec870.eventos;

import java.io.Serializable;

/** Una fila de evento IEC-870: fecha, SPA (grupo), SPQ (código), SPI (inicio/fin) y descripción. */
public class Iec870EventFila implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Formato "d/M/yy H:mm:ss", el mismo que los eventos DLMS. */
    public final String fh;
    public final int spa;
    public final int spq;
    public final int spi;
    public final String descripcion;

    public Iec870EventFila(String fh, int spa, int spq, int spi, String descripcion) {
        this.fh = fh;
        this.spa = spa;
        this.spq = spq;
        this.spi = spi;
        this.descripcion = descripcion;
    }
}