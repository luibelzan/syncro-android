package com.celnet.syncro.models.iec870.params;

/** Configuración de un puerto serie / óptico del equipo (códigos del protocolo). */
public class Iec870PortConfig {

    /** Códigos de velocidad 1..12 (0 = no disponible, 255 = otra). */
    public static final int[] BAUDS = {300, 600, 1200, 2400, 4800, 9600, 14400, 19200,
            28800, 38400, 57600, 115200};
    public static final String[] PARIDADES = {"No", "Par", "Impar"};

    public int velocidadCodigo;   // 1..12
    public int paramsCodigo;      // 1..12 (7N1 .. 8O2)
    public int modo;              // solo puerto serie 1
    public String cadena = "";    // solo puerto serie 1

    public Iec870PortConfig() {
    }

    public Iec870PortConfig(int velocidadCodigo, int paramsCodigo) {
        this.velocidadCodigo = velocidadCodigo;
        this.paramsCodigo = paramsCodigo;
    }

    /** Baudios o 0 si el código no es conocido. */
    public int baudios() {
        return velocidadCodigo >= 1 && velocidadCodigo <= BAUDS.length ? BAUDS[velocidadCodigo - 1] : 0;
    }

    public static int codigoVelocidad(int baudios) {
        for (int i = 0; i < BAUDS.length; i++) {
            if (BAUDS[i] == baudios) return i + 1;
        }
        return 0;
    }

    /** Bits de datos (7/8) o 0 si el código no es conocido. */
    public int bitsDatos() {
        return paramsCodigo >= 1 && paramsCodigo <= 12 ? (paramsCodigo <= 6 ? 7 : 8) : 0;
    }

    /** Índice en {@link #PARIDADES} (0 No, 1 Par, 2 Impar) o -1. */
    public int paridad() {
        return paramsCodigo >= 1 && paramsCodigo <= 12 ? (paramsCodigo - 1) % 3 : -1;
    }

    /** Bits de stop (1/2) o 0 si el código no es conocido. */
    public int bitsStop() {
        return paramsCodigo >= 1 && paramsCodigo <= 12 ? (((paramsCodigo - 1) % 6) < 3 ? 1 : 2) : 0;
    }

    /** Código 1..12 a partir de bits de datos (7/8), índice de paridad (0..2) y stop (1/2). */
    public static int codigoParams(int bits, int paridad, int stop) {
        return 1 + (bits == 8 ? 6 : 0) + (stop == 2 ? 3 : 0) + paridad;
    }

    /** "8 No 1" como lo muestra el software funcional. */
    public String textoParams() {
        int p = paridad();
        return p < 0 ? "?" : bitsDatos() + " " + PARIDADES[p] + " " + bitsStop();
    }
}