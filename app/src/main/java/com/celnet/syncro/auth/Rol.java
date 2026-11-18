package com.celnet.syncro.auth;

/**
 * Perfiles de usuario.
 *
 * ADMIN: todo, incluida la configuración de la aplicación y la gestión de usuarios.
 * GESTOR: todo excepto configurar los parámetros de la aplicación.
 * LECTOR: solo funciones de lectura y envío de reportes.
 */
public enum Rol {
    ADMIN("Administrador"),
    GESTOR("Gestor"),
    LECTOR("Lector");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Todos los perfiles pueden leer. */
    public boolean puedeLeer() {
        return true;
    }

    /** Todos los perfiles pueden enviar reportes. */
    public boolean puedeEnviarReportes() {
        return true;
    }

    /** Programar el contador o cualquier operación que lo modifique. */
    public boolean puedeEscribir() {
        return this == ADMIN || this == GESTOR;
    }

    /** Menú de configuración de la aplicación (FTP, STG, contador, log...). */
    public boolean puedeConfigurar() {
        return this == ADMIN;
    }

    public boolean puedeGestionarUsuarios() {
        return this == ADMIN;
    }

    /**
     * Si el valor guardado no se reconoce, se aplica el perfil con menos permisos.
     * Se aceptan los nombres antiguos ESCRITURA y LECTURA por si hay bases de datos
     * creadas con la versión anterior.
     */
    public static Rol deNombre(String nombre) {
        if ("ESCRITURA".equals(nombre)) {
            return GESTOR;
        }
        if ("LECTURA".equals(nombre)) {
            return LECTOR;
        }
        try {
            return valueOf(nombre);
        } catch (Exception e) {
            return LECTOR;
        }
    }
}