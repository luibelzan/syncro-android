package com.celnet.syncro.models;

import java.io.File;

public class ReportFile {

    public enum EstadoEnvio {
        PENDIENTE,
        SUBIENDO,
        EXITO,
        ERROR
    }

    private File file;
    private boolean seleccionado;
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    private String mensajeError;

    public ReportFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
    }

    public EstadoEnvio getEstado() {
        return estado;
    }

    public void setEstado(EstadoEnvio estado) {
        this.estado = estado;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }
}