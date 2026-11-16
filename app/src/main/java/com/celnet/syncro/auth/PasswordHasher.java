package com.celnet.syncro.auth;

import android.os.Build;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2 y sal aleatoria por usuario. La contraseña nunca se
 * guarda; solo el resultado. Se almacena también el algoritmo y las iteraciones usadas
 * para poder subirlas en el futuro sin invalidar los usuarios existentes.
 */
final class PasswordHasher {

    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    static final class Credencial {
        String hash;
        String sal;
        int iteraciones;
        String algoritmo;
    }

    private PasswordHasher() {
    }

    static Credencial crear(char[] password) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);

        Credencial c = new Credencial();
        // PBKDF2WithHmacSHA256 existe desde Android 8 (API 26).
        c.algoritmo = Build.VERSION.SDK_INT >= 26 ? "PBKDF2WithHmacSHA256" : "PBKDF2WithHmacSHA1";
        c.iteraciones = ITERACIONES;
        c.sal = Base64.encodeToString(sal, Base64.NO_WRAP);
        try {
            c.hash = Base64.encodeToString(
                    derivar(password, sal, c.iteraciones, c.algoritmo), Base64.NO_WRAP);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
        return c;
    }

    static boolean verificar(char[] password, Usuario usuario) {
        try {
            byte[] esperado = Base64.decode(usuario.hash, Base64.NO_WRAP);
            byte[] sal = Base64.decode(usuario.sal, Base64.NO_WRAP);
            byte[] obtenido = derivar(password, sal, usuario.iteraciones, usuario.algoritmo);
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derivar(char[] password, byte[] sal, int iteraciones, String algoritmo)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, LONGITUD_BITS);
        try {
            return SecretKeyFactory.getInstance(algoritmo).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}