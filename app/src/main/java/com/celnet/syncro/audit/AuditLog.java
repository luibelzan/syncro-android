package com.celnet.syncro.audit;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Registro de actividad de los usuarios, guardado en local en ficheros JSON (uno por día).
 *
 * Cada inicio de sesión abre un registro con el usuario, la fecha y la lista de acciones:
 *
 * <pre>
 * [
 *   {
 *     "usuario": "jperez",
 *     "fecha": "2026-11-19 12:29:03",
 *     "acciones": [
 *       {"hora": "12:29:03", "accion": "INICIO DE SESION"},
 *       {"hora": "12:31:40", "accion": "LECTURA DE CURVA DE CARGA CONTADOR ZIV0000123"},
 *       {"hora": "12:35:02", "accion": "CORTE DE ICP CONTADOR ZIV0000123"}
 *     ]
 *   }
 * ]
 * </pre>
 *
 * Los intentos de acceso fallidos se guardan como registros de una sola acción.
 *
 * Cada acción se escribe en cuanto ocurre (no al cerrar sesión), así que no se pierde nada
 * si la app se cierra. El fichero se amplía sin reescribirlo entero. Registrar nunca lanza
 * excepciones ni bloquea al llamante: un fallo de disco no puede romper una operación con
 * el contador.
 */
public final class AuditLog {

    private static final String DIR = "auditoria";

    // Estructura fija del final del fichero, para poder ampliarlo sin leerlo entero.
    private static final String FIN_SESION = "\n    ]\n  }";
    private static final String FIN_FICHERO = "\n]\n";

    private static volatile AuditLog instancia;

    public static AuditLog get(Context contexto) {
        if (instancia == null) {
            synchronized (AuditLog.class) {
                if (instancia == null) {
                    instancia = new AuditLog(new File(contexto.getApplicationContext().getFilesDir(), DIR));
                }
            }
        }
        return instancia;
    }

    /** Atajo para las pantallas: {@code AuditLog.log(this, "CORTE DE ICP", serie);} */
    public static void log(Context contexto, String accion, String contador) {
        get(contexto).registrar(accion, contador);
    }

    /** Atajo para acciones que no son sobre un contador concreto. */
    public static void log(Context contexto, String accion) {
        get(contexto).registrar(accion);
    }

    /** Solo para pruebas fuera de Android. */
    public static AuditLog paraDirectorio(File dir) {
        return new AuditLog(dir);
    }

    private final File dir;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audit-log");
        t.setDaemon(true);
        return t;
    });

    // Solo se tocan desde el hilo "io".
    private File ficheroSesion;
    private String usuarioSesion;

    /** El fichero no tiene la estructura esperada (editado a mano o dañado). */
    private static final class FormatoInesperado extends IOException {
        FormatoInesperado(String m) { super(m); }
    }

    private AuditLog(File dir) {
        this.dir = dir;
    }

    // ------------------------------------------------------------------ API

    /** Abre un registro nuevo para el usuario que acaba de iniciar sesión. */
    public void iniciarSesion(String usuario) {
        final Date ahora = new Date();
        final String u = usuario;
        io.execute(() -> {
            try {
                ficheroSesion = ficheroDelDia(ahora);
                usuarioSesion = u;
                try {
                    anadirRegistro(ficheroSesion, u, ahora, "INICIO DE SESION", false);
                } catch (FormatoInesperado e) {
                    apartar(ficheroSesion);
                    anadirRegistro(ficheroSesion, u, ahora, "INICIO DE SESION", false);
                }
            } catch (Exception e) {
                ficheroSesion = null;
            }
        });
    }

    public void cerrarSesion() {
        registrar("CIERRE DE SESION");
        io.execute(() -> ficheroSesion = null);
    }

    /** Anota una acción en la sesión actual. */
    public void registrar(String accion) {
        final Date ahora = new Date();
        final String a = accion;
        io.execute(() -> {
            try {
                if (ficheroSesion == null) return; // sin sesión no hay a quién atribuirla
                try {
                    anadirAccion(ficheroSesion, ahora, a);
                } catch (FormatoInesperado e) {
                    // Se aparta el fichero dañado y se continúa en uno nuevo para no perder lo siguiente.
                    apartar(ficheroSesion);
                    anadirRegistro(ficheroSesion, usuarioSesion, ahora, a, false);
                }
            } catch (Exception ignorada) {
                // el registro nunca debe romper la operación
            }
        });
    }

    /** Anota una acción sobre un contador: "ACCION CONTADOR serie". */
    public void registrar(String accion, String contador) {
        registrar(contador == null || contador.trim().isEmpty()
                ? accion
                : accion + " CONTADOR " + contador.trim());
    }

    /** Registro de una sola acción que no pertenece a una sesión (p. ej. un acceso fallido). */
    public void registrarSinSesion(String usuario, String accion) {
        final Date ahora = new Date();
        final String u = usuario;
        final String a = accion;
        io.execute(() -> {
            try {
                File f = ficheroDelDia(ahora);
                try {
                    anadirRegistro(f, u, ahora, a, true);
                } catch (FormatoInesperado e) {
                    apartar(f);
                    anadirRegistro(f, u, ahora, a, true);
                }
            } catch (Exception ignorada) {
            }
        });
    }

    /** Ficheros de registro, del más antiguo al más reciente. */
    public File[] ficheros() {
        File[] f = dir.listFiles((d, n) -> n.startsWith("audit_") && n.endsWith(".json"));
        if (f == null) return new File[0];
        java.util.Arrays.sort(f);
        return f;
    }

    /** Espera a que se escriba lo pendiente (pruebas y cierre ordenado). */
    public void esperar() throws Exception {
        io.submit(() -> { }).get();
    }

    // ------------------------------------------------------------ escritura

    private File ficheroDelDia(Date d) {
        return new File(dir, "audit_" + formato("yyyy-MM-dd", d) + ".json");
    }

    private void anadirRegistro(File f, String usuario, Date ahora, String accion, boolean cerrado)
            throws IOException {
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("No se pudo crear " + dir);

        StringBuilder reg = new StringBuilder();
        reg.append("  {\n")
                .append("    \"usuario\": ").append(cadena(usuario)).append(",\n")
                .append("    \"fecha\": ").append(cadena(formato("yyyy-MM-dd HH:mm:ss", ahora))).append(",\n")
                .append("    \"acciones\": [\n")
                .append("      ").append(accionJson(ahora, accion))
                .append(FIN_SESION);

        try (RandomAccessFile raf = abrir(f)) {
            if (raf.length() == 0) {
                escribir(raf, "[\n" + reg + FIN_FICHERO);
            } else {
                // quita "\n]\n" y añade el registro nuevo
                if (raf.length() < FIN_FICHERO.length()
                        || !colaCoincide(raf, raf.length() - FIN_FICHERO.length(), FIN_FICHERO)) {
                    throw new FormatoInesperado("Fichero de registro con formato inesperado: " + f);
                }
                raf.setLength(raf.length() - FIN_FICHERO.length());
                raf.seek(raf.length());
                escribir(raf, ",\n" + reg + FIN_FICHERO);
            }
        }
    }

    private void anadirAccion(File f, Date ahora, String accion) throws IOException {
        String cola = FIN_SESION + FIN_FICHERO;
        try (RandomAccessFile raf = abrir(f)) {
            long fin = raf.length() - cola.length();
            if (fin < 0 || !colaCoincide(raf, fin, cola)) {
                throw new FormatoInesperado("Fichero de registro con formato inesperado: " + f);
            }
            raf.setLength(fin);
            raf.seek(fin);
            escribir(raf, ",\n      " + accionJson(ahora, accion) + cola);
        }
    }

    /** Renombra un fichero dañado para conservarlo y poder empezar uno nuevo con el mismo nombre. */
    private static void apartar(File f) {
        File destino = new File(f.getParentFile(), f.getName() + ".danado-" + System.currentTimeMillis());
        //noinspection ResultOfMethodCallIgnored
        f.renameTo(destino);
    }

    private static boolean colaCoincide(RandomAccessFile raf, long pos, String cola) throws IOException {
        byte[] esperado = cola.getBytes(StandardCharsets.UTF_8);
        byte[] real = new byte[esperado.length];
        raf.seek(pos);
        raf.readFully(real);
        return java.util.Arrays.equals(esperado, real);
    }

    private static RandomAccessFile abrir(File f) throws IOException {
        return new RandomAccessFile(f, "rw");
    }

    private static void escribir(RandomAccessFile raf, String texto) throws IOException {
        raf.write(texto.getBytes(StandardCharsets.UTF_8));
        raf.getFD().sync();
    }

    private static String accionJson(Date ahora, String accion) {
        return "{\"hora\": " + cadena(formato("HH:mm:ss", ahora))
                + ", \"accion\": " + cadena(accion) + "}";
    }

    private static String formato(String patron, Date d) {
        return new SimpleDateFormat(patron, Locale.ROOT).format(d);
    }

    /** Cadena JSON con comillas y escapes. */
    static String cadena(String s) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
