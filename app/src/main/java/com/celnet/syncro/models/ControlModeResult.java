package com.celnet.syncro.models;

public class ControlModeResult {

    public boolean success;
    public String estadoInicial;
    public String estadoFinal;
    public String mensaje;

    public ControlModeResult(boolean success, String estadoInicial, String estadoFinal, String mensaje) {
        this.success = success;
        this.estadoInicial = estadoInicial;
        this.estadoFinal = estadoFinal;
        this.mensaje = mensaje;
    }
}
