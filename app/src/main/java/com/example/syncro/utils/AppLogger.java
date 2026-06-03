package com.example.syncro.utils;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Singleton que captura y almacena los logs de la app en memoria.
 * Úsalo en lugar de Log.d/i/w/e directamente.
 *
 * Ejemplo de uso:
 *   AppLogger.i("MainActivity", "Usuario logueado correctamente");
 *   AppLogger.e("SyncService", "Error de red: " + e.getMessage());
 */
public class AppLogger {

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public static class LogEntry {
        public final Level level;
        public final String tag;
        public final String message;
        public final String timestamp;
        public final long timeMillis;

        public LogEntry(Level level, String tag, String message) {
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.timeMillis = System.currentTimeMillis();
            this.timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                    .format(new Date(this.timeMillis));
        }
    }

    // ---- Configuración ----
    private static final int MAX_ENTRIES = 500; // Máximo de entradas en memoria

    // ---- Singleton ----
    private static AppLogger instance;
    private final List<LogEntry> entries = new ArrayList<>();
    private final List<OnNewLogListener> listeners = new ArrayList<>();

    public interface OnNewLogListener {
        void onNewLog(LogEntry entry);
    }

    private AppLogger() {}

    public static synchronized AppLogger getInstance() {
        if (instance == null) instance = new AppLogger();
        return instance;
    }

    // ---- Métodos estáticos de conveniencia ----

    public static void d(String tag, String message) {
        getInstance().addEntry(Level.DEBUG, tag, message);
        Log.d(tag, message); // También manda al Logcat nativo
    }

    public static void i(String tag, String message) {
        getInstance().addEntry(Level.INFO, tag, message);
        Log.i(tag, message);
    }

    public static void w(String tag, String message) {
        getInstance().addEntry(Level.WARN, tag, message);
        Log.w(tag, message);
    }

    public static void e(String tag, String message) {
        getInstance().addEntry(Level.ERROR, tag, message);
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        String full = message + "\n" + Log.getStackTraceString(throwable);
        getInstance().addEntry(Level.ERROR, tag, full);
        Log.e(tag, message, throwable);
    }

    // ---- Gestión interna ----

    private synchronized void addEntry(Level level, String tag, String message) {
        if (entries.size() >= MAX_ENTRIES) {
            entries.remove(0); // FIFO: elimina el más antiguo
        }
        LogEntry entry = new LogEntry(level, tag, message);
        entries.add(entry);
        for (OnNewLogListener listener : listeners) {
            listener.onNewLog(entry);
        }
    }

    public synchronized List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized List<LogEntry> getEntriesFiltered(Level level, String search) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry e : entries) {
            if (level != null && e.level != level) continue;
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase(Locale.getDefault());
                if (!e.message.toLowerCase(Locale.getDefault()).contains(q)
                        && !e.tag.toLowerCase(Locale.getDefault()).contains(q)) {
                    continue;
                }
            }
            result.add(e);
        }
        return result;
    }

    public synchronized void clear() {
        entries.clear();
    }

    public void addListener(OnNewLogListener listener) {
        listeners.add(listener);
    }

    public void removeListener(OnNewLogListener listener) {
        listeners.remove(listener);
    }
}