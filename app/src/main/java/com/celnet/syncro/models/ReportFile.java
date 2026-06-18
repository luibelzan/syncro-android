package com.celnet.syncro.models;

import java.io.File;

public class ReportFile {

    private File file;
    private boolean seleccionado;

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
}
