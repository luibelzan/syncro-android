package com.celnet.syncro.models;

import java.io.File;

public class ReportFile {

    public enum EstadoEnvio {
        PENDIENTE,
        SUBIENDO,
        EXITO,
        ERROR
    }

    /** Protocolo del contador del que procede el reporte. */
    public enum Origen {
        DLMS,
        IEC
    }

    private File file;
    private Origen origen;
    private boolean seleccionado;
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    private String mensajeError;

    public ReportFile(File file) {
        this(file, Origen.DLMS);
    }

    public ReportFile(File file, Origen origen) {
        this.file = file;
        this.origen = origen;
    }

    public File getFile() {
        return file;
    }

    public Origen getOrigen() {
        return origen;
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