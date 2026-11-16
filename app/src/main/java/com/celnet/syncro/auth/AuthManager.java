package com.celnet.syncro.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Punto único de acceso a usuarios, sesión y permisos.
 *
 * Todos los métodos de instancia acceden a la base de datos: llamarlos siempre fuera del
 * hilo principal (por ejemplo con {@link #io()}). Los métodos estáticos de permisos no
 * tocan la base de datos y se pueden llamar desde cualquier hilo.
 */
public final class AuthManager {

    public static final String ADMIN_POR_DEFECTO = "admin";
    public static final String PASSWORD_INICIAL = "admin";

    static final int MAX_INTENTOS = 5;
    static final long BLOQUEO_MS = 5L * 60 * 1000;
    static final int LONGITUD_MIN_PASSWORD = 6;
    private static final Pattern PATRON_USUARIO = Pattern.compile("[A-Za-z0-9._-]{3,30}");

    public enum Estado { OK, CREDENCIALES_INVALIDAS, BLOQUEADO, DESACTIVADO }

    public static final class ResultadoLogin {
        public Estado estado;
        public Usuario usuario;
        public long bloqueadoHasta;
        public int intentosRestantes;
    }

    private static volatile AuthManager instancia;

    private final UsuarioDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile Usuario actual;

    private AuthManager(Context contexto) {
        this.dao = AuthDatabase.get(contexto).usuarioDao();
    }

    public static AuthManager get(Context contexto) {
        if (instancia == null) {
            synchronized (AuthManager.class) {
                if (instancia == null) {
                    instancia = new AuthManager(contexto);
                }
            }
        }
        return instancia;
    }

    /** Hilo de trabajo para llamar a los métodos que usan la base de datos. */
    public ExecutorService io() {
        return io;
    }

    // ------------------------------------------------------------------ sesión

    public ResultadoLogin login(String nombre, char[] password) {
        asegurarAdmin();
        ResultadoLogin r = new ResultadoLogin();

        Usuario u = (nombre == null) ? null : dao.porNombre(nombre.trim());
        if (u == null) {
            r.estado = Estado.CREDENCIALES_INVALIDAS;
            return r;
        }

        long ahora = System.currentTimeMillis();
        if (u.bloqueadoHasta > ahora) {
            r.estado = Estado.BLOQUEADO;
            r.bloqueadoHasta = u.bloqueadoHasta;
            return r;
        }

        if (!PasswordHasher.verificar(password, u)) {
            u.intentosFallidos++;
            if (u.intentosFallidos >= MAX_INTENTOS) {
                u.bloqueadoHasta = ahora + BLOQUEO_MS;
                u.intentosFallidos = 0;
                r.estado = Estado.BLOQUEADO;
                r.bloqueadoHasta = u.bloqueadoHasta;
            } else {
                r.estado = Estado.CREDENCIALES_INVALIDAS;
                r.intentosRestantes = MAX_INTENTOS - u.intentosFallidos;
            }
            dao.actualizar(u);
            return r;
        }

        if (!u.activo) {
            r.estado = Estado.DESACTIVADO;
            return r;
        }

        u.intentosFallidos = 0;
        u.bloqueadoHasta = 0;
        u.ultimoAcceso = ahora;
        dao.actualizar(u);
        actual = u;
        r.estado = Estado.OK;
        r.usuario = u;
        return r;
    }

    public void logout() {
        actual = null;
    }

    /** Crea el administrador predefinido si todavía no hay ningún usuario. */
    private synchronized void asegurarAdmin() {
        if (dao.contar() > 0) {
            return;
        }
        Usuario admin = nuevo(ADMIN_POR_DEFECTO, PASSWORD_INICIAL.toCharArray(), Rol.ADMIN, "sistema");
        admin.debeCambiarPassword = true;
        dao.insertar(admin);
    }

    // ------------------------------------------------------------ contraseñas

    /** Cambia la contraseña del usuario con sesión. Devuelve un mensaje de error o null. */
    public String cambiarPropiaPassword(char[] nueva) {
        Usuario ses = actual;
        if (ses == null) {
            return "Sesión no iniciada";
        }
        Usuario u = dao.porId(ses.id);
        if (u == null) {
            return "Usuario no encontrado";
        }
        String error = validarPassword(nueva, u.nombreUsuario);
        if (error != null) {
            return error;
        }
        aplicarPassword(u, nueva);
        u.debeCambiarPassword = false;
        dao.actualizar(u);
        actual = u;
        return null;
    }

    /**
     * Solo administradores. Deja una contraseña temporal y obliga al usuario a cambiarla
     * en su próximo acceso. También levanta un bloqueo por intentos fallidos.
     */
    public String restablecerPassword(String idUsuario, char[] temporal) {
        String error = requiereAdmin();
        if (error != null) {
            return error;
        }
        Usuario u = dao.porId(idUsuario);
        if (u == null) {
            return "Usuario no encontrado";
        }
        error = validarPassword(temporal, u.nombreUsuario);
        if (error != null) {
            return error;
        }
        aplicarPassword(u, temporal);
        u.debeCambiarPassword = true;
        u.intentosFallidos = 0;
        u.bloqueadoHasta = 0;
        dao.actualizar(u);
        return null;
    }

    // --------------------------------------------------------- gestión (admin)

    public List<Usuario> listar() {
        return dao.todos();
    }

    public String crearUsuario(String nombre, char[] passwordInicial, Rol rol) {
        String error = requiereAdmin();
        if (error != null) {
            return error;
        }
        String n = nombre == null ? "" : nombre.trim();
        if (!PATRON_USUARIO.matcher(n).matches()) {
            return "El usuario debe tener entre 3 y 30 caracteres: letras, números, punto, guion o guion bajo";
        }
        error = validarPassword(passwordInicial, n);
        if (error != null) {
            return error;
        }
        Usuario u = nuevo(n, passwordInicial, rol, actual.nombreUsuario);
        u.debeCambiarPassword = true;
        try {
            dao.insertar(u);
        } catch (SQLiteConstraintException e) {
            return "Ya existe un usuario con ese nombre";
        }
        return null;
    }

    public String cambiarRol(String idUsuario, Rol rol) {
        String error = requiereAdmin();
        if (error != null) {
            return error;
        }
        Usuario u = dao.porId(idUsuario);
        if (u == null) {
            return "Usuario no encontrado";
        }
        if (u.rolEnum() == Rol.ADMIN && u.activo && rol != Rol.ADMIN
                && dao.contarAdminsActivos() <= 1) {
            return "Debe quedar al menos un administrador activo";
        }
        u.rol = rol.name();
        dao.actualizar(u);
        if (actual != null && actual.id.equals(u.id)) {
            actual = u;
        }
        return null;
    }

    public String setActivo(String idUsuario, boolean activo) {
        String error = requiereAdmin();
        if (error != null) {
            return error;
        }
        Usuario u = dao.porId(idUsuario);
        if (u == null) {
            return "Usuario no encontrado";
        }
        if (!activo) {
            if (actual != null && actual.id.equals(u.id)) {
                return "No puedes desactivar tu propio usuario";
            }
            if (u.rolEnum() == Rol.ADMIN && u.activo && dao.contarAdminsActivos() <= 1) {
                return "Debe quedar al menos un administrador activo";
            }
        }
        u.activo = activo;
        if (activo) {
            u.intentosFallidos = 0;
            u.bloqueadoHasta = 0;
        }
        dao.actualizar(u);
        return null;
    }

    // ------------------------------------------------------------- permisos

    public static Usuario usuarioActual() {
        AuthManager m = instancia;
        return m == null ? null : m.actual;
    }

    public static boolean haySesion() {
        return usuarioActual() != null;
    }

    public static boolean puedeEscribir() {
        Usuario u = usuarioActual();
        return u != null && u.rolEnum().puedeEscribir();
    }

    public static boolean puedeGestionarUsuarios() {
        Usuario u = usuarioActual();
        return u != null && u.rolEnum().puedeGestionarUsuarios();
    }

    /**
     * Para el principio de onCreate de la pantalla que debe quedar protegida. Si no hay
     * sesión, abre el login y devuelve false: la pantalla debe hacer return. Tras iniciar
     * sesión, el login vuelve a abrir esta misma pantalla con sus extras originales, así
     * que no hace falta modificar quien la lanza.
     */
    public static boolean exigirSesion(Activity actividad) {
        if (haySesion()) {
            return true;
        }
        Intent login = new Intent(actividad, LoginActivity.class);
        login.putExtra(LoginActivity.EXTRA_DESTINO, new Intent(actividad.getIntent()));
        actividad.startActivity(login);
        actividad.finish();
        return false;
    }

    /**
     * Para el comienzo de cualquier acción que modifique el contador. Si el perfil no lo
     * permite muestra un aviso y devuelve false: la acción no debe ejecutarse.
     */
    public static boolean exigirEscritura(Context contexto) {
        if (puedeEscribir()) {
            return true;
        }
        Toast.makeText(contexto, "Tu perfil no permite modificar el contador",
                Toast.LENGTH_LONG).show();
        return false;
    }

    // -------------------------------------------------------------- internos

    private String requiereAdmin() {
        Usuario u = actual;
        if (u == null) {
            return "Sesión no iniciada";
        }
        if (!u.rolEnum().puedeGestionarUsuarios()) {
            return "Solo un administrador puede hacer esto";
        }
        return null;
    }

    private static String validarPassword(char[] password, String nombreUsuario) {
        if (password == null || password.length < LONGITUD_MIN_PASSWORD) {
            return "La contraseña debe tener al menos " + LONGITUD_MIN_PASSWORD + " caracteres";
        }
        String p = new String(password);
        if (p.equals(PASSWORD_INICIAL)) {
            return "Elige una contraseña distinta de la inicial";
        }
        if (p.equalsIgnoreCase(nombreUsuario)) {
            return "La contraseña no puede ser igual al nombre de usuario";
        }
        return null;
    }

    private static Usuario nuevo(String nombre, char[] password, Rol rol, String creadoPor) {
        Usuario u = new Usuario();
        u.nombreUsuario = nombre;
        u.rol = rol.name();
        u.creadoEn = System.currentTimeMillis();
        u.creadoPor = creadoPor;
        aplicarPassword(u, password);
        return u;
    }

    private static void aplicarPassword(Usuario u, char[] password) {
        PasswordHasher.Credencial c = PasswordHasher.crear(password);
        u.hash = c.hash;
        u.sal = c.sal;
        u.iteraciones = c.iteraciones;
        u.algoritmo = c.algoritmo;
    }
}