package com.example.syncro.licenses;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LicenseManager — versión corregida
 *
 * Política de caché:
 *   - La caché local es válida máximo CACHE_TTL_MS (24h).
 *   - Pasadas 24h, SIEMPRE se revalida contra Firebase.
 *   - Si Firebase no es accesible (sin red), se bloquea el acceso.
 *     Cambia BLOCK_ON_NETWORK_ERROR a false si prefieres política permisiva.
 *
 * Flujo verify():
 *   1. Sin licencia en caché                          → INVALID_CODE
 *   2. MAC no coincide con la sonda conectada         → INVALID_MAC
 *   3. Fecha expirada en caché local                  → EXPIRED
 *   4. Caché fresca (< 24h) y código en caché válido  → VALID (sin Firebase)
 *   5. Caché antigua (> 24h)                          → consulta Firebase:
 *        · Documento no existe (código cambiado)      → INVALID_CODE
 *        · valid=false                                → DISABLED
 *        · MAC no coincide                            → INVALID_MAC
 *        · Expirado en Firebase                       → EXPIRED
 *        · Todo OK → actualiza caché y timestamp      → VALID
 *        · Sin red                                    → NETWORK_ERROR (bloquea)
 */
public class LicenseManager {

    private static final String PREFS_NAME     = "syncro_license";
    private static final String KEY_CODE       = "license_code";
    private static final String KEY_MAC        = "license_mac";
    private static final String KEY_EXPIRES    = "license_expires";
    private static final String KEY_CUSTOMER   = "license_customer";
    private static final String KEY_LAST_CHECK = "license_last_check";

    // Caché válida 24 horas
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L;

    // true  → sin red después de 24h bloquea el acceso
    // false → sin red después de 24h deja pasar (permisivo)
    private static final boolean BLOCK_ON_NETWORK_ERROR = true;

    public enum LicenseStatus {
        VALID,
        INVALID_CODE,   // Código no existe en Firestore (puede haber sido cambiado)
        INVALID_MAC,    // Código existe pero la MAC no coincide
        EXPIRED,        // Licencia caducada
        DISABLED,       // valid=false (revocada manualmente)
        NETWORK_ERROR   // No se pudo contactar con Firebase
    }

    public interface LicenseCallback {
        void onResult(LicenseStatus status, String customer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activación — primera vez que el usuario introduce el código
    // ─────────────────────────────────────────────────────────────────────────

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
                        saveToPrefs(context,
                                licenseCode,
                                deviceMac,
                                doc.getString("expires"),
                                doc.getString("customer"));

                        // Rellenar activatedAt si está vacío o no existe el campo.
                        // Usamos set() con merge=true en lugar de update() para que
                        // funcione tanto si el campo existe como si no fue creado.
                        if (isEmpty(doc.getString("activatedAt"))) {
                            String today = todayString();
                            java.util.Map<String, Object> patch = new java.util.HashMap<>();
                            patch.put("activatedAt", today);
                            db.collection("licenses")
                                    .document(licenseCode.trim().toUpperCase())
                                    .set(patch, com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener(aVoid ->
                                            android.util.Log.d("LicenseManager",
                                                    "activatedAt actualizado: " + today))
                                    .addOnFailureListener(e ->
                                            android.util.Log.e("LicenseManager",
                                                    "Error al actualizar activatedAt: " + e.getMessage()));
                        }
                    }

                    callback.onResult(status, doc.getString("customer"));
                })
                .addOnFailureListener(e ->
                        callback.onResult(LicenseStatus.NETWORK_ERROR, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verificación — llamar cada vez que se abre la app o se conecta la sonda
    // ─────────────────────────────────────────────────────────────────────────

    public static void verify(Context context,
                              String deviceMac,
                              LicenseCallback callback) {

        SharedPreferences prefs    = prefs(context);
        String cachedCode          = prefs.getString(KEY_CODE, null);
        String cachedMac           = prefs.getString(KEY_MAC, null);
        String cachedExpires       = prefs.getString(KEY_EXPIRES, null);
        String cachedCustomer      = prefs.getString(KEY_CUSTOMER, null);
        long   lastCheck           = prefs.getLong(KEY_LAST_CHECK, 0);

        // 1. Sin licencia guardada
        if (cachedCode == null || cachedMac == null) {
            callback.onResult(LicenseStatus.INVALID_CODE, null);
            return;
        }

        // 2. La sonda no coincide con la licenciada
        if (!cachedMac.equalsIgnoreCase(deviceMac)) {
            callback.onResult(LicenseStatus.INVALID_MAC, null);
            return;
        }

        // 3. Expiración local
        if (isExpired(cachedExpires)) {
            callback.onResult(LicenseStatus.EXPIRED, cachedCustomer);
            return;
        }

        long now = System.currentTimeMillis();
        boolean cacheIsFresh = (now - lastCheck) < CACHE_TTL_MS;

        // 4. Caché fresca → válida sin consultar Firebase
        if (cacheIsFresh) {
            callback.onResult(LicenseStatus.VALID, cachedCustomer);
            return;
        }

        // 5. Caché antigua → revalidar SIEMPRE contra Firebase
        revalidateOnline(context, cachedCode, deviceMac, cachedCustomer, callback);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revalidación online — se llama cuando la caché tiene más de 24h
    // ─────────────────────────────────────────────────────────────────────────

    private static void revalidateOnline(Context context,
                                         String cachedCode,
                                         String deviceMac,
                                         String cachedCustomer,
                                         LicenseCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("licenses")
                .document(cachedCode)
                .get()
                .addOnSuccessListener(doc -> {
                    LicenseStatus status = evaluateDocument(doc, deviceMac);

                    if (status == LicenseStatus.VALID) {
                        // Actualizar caché con datos frescos de Firebase y resetear timestamp
                        saveToPrefs(context,
                                cachedCode,
                                deviceMac,
                                doc.getString("expires"),
                                doc.getString("customer"));
                    } else {
                        // Licencia ya no válida → borrar caché para forzar pantalla de activación
                        clearLicense(context);
                    }

                    callback.onResult(status,
                            status == LicenseStatus.VALID
                                    ? doc.getString("customer")
                                    : cachedCustomer);
                })
                .addOnFailureListener(e -> {
                    if (BLOCK_ON_NETWORK_ERROR) {
                        // Sin red → bloquear
                        callback.onResult(LicenseStatus.NETWORK_ERROR, cachedCustomer);
                    } else {
                        // Sin red → dejar pasar pero NO actualizar el timestamp
                        // (así volverá a intentar en el próximo arranque)
                        callback.onResult(LicenseStatus.VALID, cachedCustomer);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades públicas
    // ─────────────────────────────────────────────────────────────────────────

    /** Elimina la licencia guardada (para tests o soporte). */
    public static void clearLicense(Context context) {
        prefs(context).edit().clear().apply();
    }

    /** MAC licenciada en caché, o null si no hay licencia. */
    public static String getCachedMac(Context context) {
        return prefs(context).getString(KEY_MAC, null);
    }

    /** true si hay alguna licencia guardada en caché. */
    public static boolean hasLicense(Context context) {
        return prefs(context).getString(KEY_CODE, null) != null;
    }

    /** Fuerza revalidación en el próximo verify() poniendo lastCheck a 0. */
    public static void invalidateCache(Context context) {
        prefs(context).edit().putLong(KEY_LAST_CHECK, 0).apply();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

    private static LicenseStatus evaluateDocument(DocumentSnapshot doc, String deviceMac) {
        if (!doc.exists())                          return LicenseStatus.INVALID_CODE;

        Boolean valid = doc.getBoolean("valid");
        if (valid == null || !valid)                return LicenseStatus.DISABLED;

        String licensedMac = doc.getString("mac");
        if (isEmpty(licensedMac) ||
                !licensedMac.equalsIgnoreCase(deviceMac)) return LicenseStatus.INVALID_MAC;

        if (isExpired(doc.getString("expires")))    return LicenseStatus.EXPIRED;

        return LicenseStatus.VALID;
    }

    private static void saveToPrefs(Context context, String code, String mac,
                                    String expires, String customer) {
        prefs(context).edit()
                .putString(KEY_CODE,      code.trim().toUpperCase())
                .putString(KEY_MAC,       mac)
                .putString(KEY_EXPIRES,   expires  != null ? expires  : "")
                .putString(KEY_CUSTOMER,  customer != null ? customer : "")
                .putLong  (KEY_LAST_CHECK, System.currentTimeMillis())  // ← resetea el reloj
                .apply();
    }

    private static boolean isExpired(String expiresStr) {
        if (isEmpty(expiresStr)) return false;
        try {
            Date expDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(expiresStr);
            return expDate != null && expDate.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}