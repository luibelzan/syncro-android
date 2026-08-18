package com.celnet.syncro.models.instantvalues;

public enum TipoLecturaInstantanea {
    VALORES_S28("Valores instantáneos (Companion < 1.09)"),
    VALORES_S29("Valores instantáneos (S29)");

    public final String etiqueta;

    TipoLecturaInstantanea(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}