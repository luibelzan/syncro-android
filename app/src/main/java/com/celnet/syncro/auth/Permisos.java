package com.celnet.syncro.auth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tabla de qué perfil necesita cada pantalla. Es el único sitio a editar si hay que
 * mover una pantalla de nivel. Se identifica por el nombre simple de la clase.
 *
 * Las pantallas que mezclan lectura y escritura (por ejemplo "Leer actual" y
 * "Programar" en la misma pantalla) NO van aquí: se dejan accesibles y el botón que
 * escribe se protege con AuthManager.exigirEscritura(this).
 */
public final class Permisos {

    public enum Nivel {
        /** No requiere sesión: pantallas anteriores al login. */
        PUBLICA,
        /** Cualquier usuario con sesión. */
        LECTURA,
        /** Gestor o administrador. */
        ESCRITURA,
        /** Solo administrador. */
        CONFIGURACION
    }

    private static final Set<String> PUBLICAS = set(
            "LoginActivity",
            "BluetoothScanActivity",
            "LicenseCheckActivity");

    /** Menú de configuración de la aplicación y todo lo que cuelga de él. */
    private static final Set<String> CONFIGURACION = set(
            "ConfigActivity",
            "FtpActivity",
            "StgActivity",
            "ConfigContadorActivity",
            "IecConfigActivity",
            "IecFtpActivity",
            "IecStgActivity",
            "IecConfigContadorActivity",
            "AppLogActivity",
            "UsuariosActivity");

    /** Pantallas cuyo único propósito es modificar el contador. REVISAR esta lista. */
    private static final Set<String> ESCRITURA = set(
            "ProgramarContratoActivity",
            "SyncDateActivity",
            "IcpExecuteActivity",
            "CambioPasswordActivity");

    private Permisos() {
    }

    public static Nivel nivelDe(String nombreClase) {
        if (PUBLICAS.contains(nombreClase)) {
            return Nivel.PUBLICA;
        }
        if (CONFIGURACION.contains(nombreClase)) {
            return Nivel.CONFIGURACION;
        }
        if (ESCRITURA.contains(nombreClase)) {
            return Nivel.ESCRITURA;
        }
        return Nivel.LECTURA;
    }

    private static Set<String> set(String... nombres) {
        return new HashSet<>(Arrays.asList(nombres));
    }
}