package com.celnet.syncro.models.parameters;

public class ControlModeResult {

    public boolean success;
    public String estadoInicial;
    public String estadoFinal;
    public String modoControl;
    public String mensaje;

    public ControlModeResult(boolean success, String estadoInicial, String estadoFinal,
                             String modoControl, String mensaje) {
        this.success = success;
        this.estadoInicial = estadoInicial;
        this.estadoFinal = estadoFinal;
        this.modoControl = modoControl;
        this.mensaje = mensaje;
    }
}