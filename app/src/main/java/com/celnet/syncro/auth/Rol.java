package com.celnet.syncro.auth;

/** Perfiles de usuario. Cada perfil incluye los permisos de los anteriores. */
public enum Rol {
    ADMIN("Administrador"),
    ESCRITURA("Escritura"),
    LECTURA("Lectura");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean puedeLeer() {
        return true;
    }

    /** Programar parámetros y cualquier operación que modifique el contador. */
    public boolean puedeEscribir() {
        return this == ADMIN || this == ESCRITURA;
    }

    public boolean puedeGestionarUsuarios() {
        return this == ADMIN;
    }

    /** Si el valor guardado no se reconoce, se aplica el perfil con menos permisos. */
    public static Rol deNombre(String nombre) {
        try {
            return valueOf(nombre);
        } catch (Exception e) {
            return LECTURA;
        }
    }
}