package com.example.syncro.licenses;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LicenseManager
 *
 * Flujo:
 *  1. El usuario introduce un código de licencia en LicenseCheckActivity.
 *  2. Se consulta Firestore: licenses/{licenseCode}
 *  3. Si el documento existe, valid=true, y la MAC del campo "mac" coincide
 *     con la MAC de la sonda conectada → licencia activada.
 *  4. Se guarda en SharedPreferences para no consultar Firestore en cada arranque
 *     (caché de 24 horas).
 *
 * Estructura Firestore:
 *   Collection: licenses
 *   Document ID: el código de licencia (ej: "SYNC-XXXX-YYYY-ZZZZ")
 *   Campos:
 *     - mac         (String)  "AA:BB:CC:DD:EE:FF"   ← MAC de la sonda autorizada
 *     - valid       (boolean) true/false
 *     - customer    (String)  "Empresa X"
 *     - expires     (String)  "2027-12-31"           ← fecha límite (YYYY-MM-DD)
 *     - activatedAt (String)  "2026-05-22"           ← se rellena al activar
 */
public class LicenseManager {

    private static final String PREFS_NAME      = "syncro_license";
    private static final String KEY_CODE        = "license_code";
    private static final String KEY_MAC         = "license_mac";
    private static final String KEY_EXPIRES     = "license_expires";
    private static final String KEY_CUSTOMER    = "license_customer";
    private static final String KEY_LAST_CHECK  = "license_last_check";

    // Caché válida 24 horas (en milisegundos)
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L;

    public enum LicenseStatus {
        VALID,           // Licencia válida para esta MAC
        INVALID_CODE,    // Código no existe en Firestore
        INVALID_MAC,     // Código existe pero la MAC no coincide
        EXPIRED,         // Licencia caducada
        DISABLED,        // valid=false (revocada manualmente)
        NETWORK_ERROR    // No se pudo contactar con Firebase
    }

    public interface LicenseCallback {
        void onResult(LicenseStatus status, String customer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activación — llamar cuando el usuario introduce el código por primera vez
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valida el código contra Firestore y comprueba que la MAC coincide.
     * Si todo es correcto, guarda la licencia en caché local.
     *
     * @param context     contexto Android
     * @param licenseCode código introducido por el usuario (ej: "SYNC-XXXX-YYYY")
     * @param deviceMac   MAC de la sonda Bluetooth conectada (ej: "AA:BB:CC:DD:EE:FF")
     * @param callback    resultado en el hilo que llama (puede ser background)
     */
    public static void activate(Context context,
                                String licenseCode,
                                String deviceMac,
                                LicenseCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("licenses")
                .document(licenseCode.trim().toUpperCase())
                .get()
                .addOnSuccessListener(doc -> {
                    LicenseStatus status = evaluateDocument(doc, deviceMac);

                    if (status == LicenseStatus.VALID) {
                        // Guardar en caché local
                        saveToPrefs(context, licenseCode, deviceMac,
                                doc.getString("expires"),
                                doc.getString("customer"));

                        // Marcar activatedAt en Firestore si aún no tiene valor
                        if (doc.getString("activatedAt") == null ||
                                doc.getString("activatedAt").isEmpty()) {
                            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(new Date());
                            db.collection("licenses")
                                    .document(licenseCode.trim().toUpperCase())
                                    .update("activatedAt", today);
                        }
                    }

                    callback.onResult(status, doc.getString("customer"));
                })
                .addOnFailureListener(e -> callback.onResult(LicenseStatus.NETWORK_ERROR, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verificación — llamar al arrancar la app o al conectar la sonda
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica la licencia guardada en caché contra la MAC de la sonda.
     * Si la caché tiene menos de 24h, no consulta Firestore.
     * Si la caché ha expirado, revalida online.
     *
     * Diseñado para llamarse desde un hilo de background (usa CountDownLatch).
     *
     * @param context   contexto Android
     * @param deviceMac MAC de la sonda Bluetooth actualmente conectada
     * @param callback  resultado
     */
    public static void verify(Context context,
                              String deviceMac,
                              LicenseCallback callback) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedCode    = prefs.getString(KEY_CODE, null);
        String cachedMac     = prefs.getString(KEY_MAC, null);
        String cachedExpires = prefs.getString(KEY_EXPIRES, null);
        String cachedCustomer = prefs.getString(KEY_CUSTOMER, null);
        long   lastCheck     = prefs.getLong(KEY_LAST_CHECK, 0);

        // Sin licencia guardada → pedir activación
        if (cachedCode == null || cachedMac == null) {
            callback.onResult(LicenseStatus.INVALID_CODE, null);
            return;
        }

        // La MAC de la sonda no coincide con la licenciada
        if (!cachedMac.equalsIgnoreCase(deviceMac)) {
            callback.onResult(LicenseStatus.INVALID_MAC, null);
            return;
        }

        // Comprobar expiración local
        if (isExpired(cachedExpires)) {
            callback.onResult(LicenseStatus.EXPIRED, cachedCustomer);
            return;
        }

        // Caché fresca → válida sin consultar Firestore
        long now = System.currentTimeMillis();
        if ((now - lastCheck) < CACHE_TTL_MS) {
            callback.onResult(LicenseStatus.VALID, cachedCustomer);
            return;
        }

        // Caché antigua → revalidar online
        activate(context, cachedCode, deviceMac, (status, customer) -> {
            if (status == LicenseStatus.NETWORK_ERROR) {
                // Sin red pero caché no muy antigua → dejar pasar (política permisiva)
                // Cambia esto a NETWORK_ERROR si prefieres bloquear sin red
                callback.onResult(LicenseStatus.VALID, cachedCustomer);
            } else {
                callback.onResult(status, customer);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────────────

    /** Elimina la licencia guardada (para pruebas o soporte). */
    public static void clearLicense(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    /** Devuelve la MAC licenciada guardada en caché, o null si no hay licencia. */
    public static String getCachedMac(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MAC, null);
    }

    /** Devuelve true si ya hay una licencia activada en caché. */
    public static boolean hasLicense(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CODE, null) != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

    private static LicenseStatus evaluateDocument(DocumentSnapshot doc, String deviceMac) {
        if (!doc.exists()) return LicenseStatus.INVALID_CODE;

        Boolean valid = doc.getBoolean("valid");
        if (valid == null || !valid) return LicenseStatus.DISABLED;

        String licensedMac = doc.getString("mac");
        if (licensedMac == null || !licensedMac.equalsIgnoreCase(deviceMac)) {
            return LicenseStatus.INVALID_MAC;
        }

        String expires = doc.getString("expires");
        if (isExpired(expires)) return LicenseStatus.EXPIRED;

        return LicenseStatus.VALID;
    }

    private static boolean isExpired(String expiresStr) {
        if (expiresStr == null || expiresStr.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expDate = sdf.parse(expiresStr);
            return expDate != null && expDate.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveToPrefs(Context context, String code, String mac,
                                    String expires, String customer) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CODE, code.trim().toUpperCase())
                .putString(KEY_MAC, mac)
                .putString(KEY_EXPIRES, expires != null ? expires : "")
                .putString(KEY_CUSTOMER, customer != null ? customer : "")
                .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                .apply();
    }
}