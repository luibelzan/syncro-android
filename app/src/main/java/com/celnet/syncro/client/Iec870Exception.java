package com.celnet.syncro.client;

/** Error de protocolo o de comunicación IEC 60870-5-102. */
public class Iec870Exception extends Exception {
    public Iec870Exception(String message) {
        super(message);
    }

    public Iec870Exception(String message, Throwable cause) {
        super(message, cause);
    }
}