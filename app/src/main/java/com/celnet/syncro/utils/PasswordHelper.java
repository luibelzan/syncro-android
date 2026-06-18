package com.celnet.syncro.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.SecureRandom;
import android.util.Base64;

public class PasswordHelper {

    private static final String PREFS_NAME = "syncro_admin_prefs";
    private static final String KEY_HASH   = "admin_password_hash";
    private static final String KEY_SALT   = "admin_password_salt";

    // Contraseña por defecto si nunca se ha configurado una
    private static final String DEFAULT_PASSWORD = "C3ln3t#AppL0g!92XvQ";

    // ---- Hash ----

    private static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP); // 👈 NO_WRAP
    }

    private static String sha256(String input, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes("UTF-8"));
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            return Base64.encodeToString(hash, Base64.NO_WRAP); // 👈 NO_WRAP
        } catch (Exception e) {
            AppLogger.e("PasswordHelper", "Error al hashear: " + e.getMessage());
            return null;
        }
    }

    // ---- API pública ----

    /**
     * Inicializa la contraseña por defecto si nunca se ha guardado una.
     * Llama a este método en el Application o MainActivity al arrancar.
     */
    public static void initDefaultPasswordIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_HASH)) {
            savePassword(context, DEFAULT_PASSWORD);
            AppLogger.i("PasswordHelper", "Contraseña admin por defecto inicializada.");
        }
    }

    public static void savePassword(Context context, String newPassword) {
        String salt = generateSalt();
        String hash = sha256(newPassword, salt);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HASH, hash)
                .putString(KEY_SALT, salt)
                .apply();
    }

    public static boolean checkPassword(Context context, String input) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedHash = prefs.getString(KEY_HASH, null);
        String storedSalt = prefs.getString(KEY_SALT, null);
        if (storedHash == null || storedSalt == null) return false;
        String inputHash = sha256(input, storedSalt);
        return storedHash.equals(inputHash);
    }
}