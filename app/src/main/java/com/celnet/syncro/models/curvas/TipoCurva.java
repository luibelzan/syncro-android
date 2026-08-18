package com.celnet.syncro.models.curvas;

public enum TipoCurva {
    INCREMENTAL_S02("1.0.99.1.0.255", "Curva de carga incremental (S02)"),
    VOLTAGE_S44("1.0.99.1.4.255", "Tensiones máx/media/mín (S44)"),
    CURRENT_S45("1.0.99.1.5.255", "Corrientes máx/media/mín (S45)"),
    ENERGY_PHASE_S43("1.0.99.1.6.255", "Energías por fase (S43)");

    public final String obis;
    public final String etiqueta;

    TipoCurva(String obis, String etiqueta) {
        this.obis = obis;
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}