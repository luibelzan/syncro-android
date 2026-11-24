package com.celnet.syncro.utils;

import android.text.InputFilter;
import android.widget.EditText;

/**
 * Filtros y validación de los campos de configuración FTP/SFTP (DLMS e IEC-870).
 *
 * - Los filtros se aplican al teclear, al pegar y también a setText(), así que una
 *   configuración antigua con saltos de línea se limpia sola al abrir la pantalla.
 * - La contraseña solo pierde caracteres de control/invisibles; nunca se recorta ni se
 *   cambia el resto, para no alterar una contraseña válida.
 */
public final class FtpConfigInput {

    public static final int MAX_HOST = 253;
    public static final int MAX_PORT = 5;
    public static final int MAX_FOLDER = 255;
    public static final int MAX_USER = 128;
    public static final int MAX_PASS = 128;

    private FtpConfigInput() {}

    // ---------------------------------------------------------------- limpieza (lógica pura)

    /** Caracteres de control, saltos de línea/párrafo, tabuladores y caracteres invisibles. */
    static boolean esControlOInvisible(char c) {
        return Character.isISOControl(c)
                || c == '\u2028' || c == '\u2029'
                || Character.getType(c) == Character.FORMAT          // ZWSP, BOM, marcas bidi...
                || Character.getType(c) == Character.LINE_SEPARATOR
                || Character.getType(c) == Character.PARAGRAPH_SEPARATOR;
    }

    static boolean esEspacio(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    /** Servidor: letras, dígitos y . - _ : [ ] % (nombres DNS, IPv4 e IPv6). */
    static boolean hostValido(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '.' || c == '-' || c == '_' || c == ':' || c == '[' || c == ']' || c == '%';
    }

    /** Carpeta: sin control, sin \ ? * " < > | y sin espacios raros (el espacio normal sí). */
    static boolean carpetaValida(char c) {
        if (esControlOInvisible(c)) return false;
        if (c == ' ') return true;
        if (esEspacio(c)) return false;
        return "\\?*\"<>|".indexOf(c) < 0;
    }

    /** Usuario: cualquier carácter visible, sin espacios ni control. */
    static boolean usuarioValido(char c) {
        return !esControlOInvisible(c) && !esEspacio(c);
    }

    /** Contraseña: todo salvo control/invisibles (se admiten espacios, que pueden ser legítimos). */
    static boolean passValido(char c) {
        return !esControlOInvisible(c);
    }

    private interface Regla { boolean ok(char c); }

    private static String filtrar(CharSequence s, Regla r) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (r.ok(c)) sb.append(c);
        }
        return sb.toString();
    }

    public static String limpiarHost(CharSequence s) { return filtrar(s, FtpConfigInput::hostValido); }
    public static String limpiarPuerto(CharSequence s) { return filtrar(s, c -> c >= '0' && c <= '9'); }
    public static String limpiarCarpeta(CharSequence s) { return filtrar(s, FtpConfigInput::carpetaValida); }
    public static String limpiarUsuario(CharSequence s) { return filtrar(s, FtpConfigInput::usuarioValido); }
    public static String limpiarPass(CharSequence s) { return filtrar(s, FtpConfigInput::passValido); }

    // ---------------------------------------------------------------- filtros de EditText

    private static InputFilter filtro(final Regla r) {
        return (source, start, end, dest, dstart, dend) -> {
            StringBuilder sb = null;
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (r.ok(c)) {
                    if (sb != null) sb.append(c);
                } else if (sb == null) {
                    sb = new StringBuilder(end - start);
                    sb.append(source, start, i);
                }
            }
            return sb == null ? null : sb.toString();   // null = sin cambios
        };
    }

    private static void poner(EditText e, Regla r, int max) {
        e.setFilters(new InputFilter[]{filtro(r), new InputFilter.LengthFilter(max)});
    }

    /** Aplica los filtros a los cinco campos de texto. Llamar ANTES de cargar la configuración. */
    public static void aplicarFiltros(EditText host, EditText puerto, EditText carpeta,
                                      EditText usuario, EditText pass) {
        poner(host, FtpConfigInput::hostValido, MAX_HOST);
        poner(puerto, c -> c >= '0' && c <= '9', MAX_PORT);
        poner(carpeta, FtpConfigInput::carpetaValida, MAX_FOLDER);
        poner(usuario, FtpConfigInput::usuarioValido, MAX_USER);
        poner(pass, FtpConfigInput::passValido, MAX_PASS);
    }

    // ---------------------------------------------------------------- validación

    private static String texto(EditText e) { return e.getText() == null ? "" : e.getText().toString(); }

    private static String error(EditText campo, String msg) {
        campo.setError(msg);
        campo.requestFocus();
        return msg;
    }

    /**
     * Valida y normaliza los campos. Devuelve null si todo es correcto (y deja los campos ya
     * normalizados) o el mensaje del primer error (mostrado también en el propio campo).
     *
     * @param protocolo      texto elegido en el desplegable
     * @param permitidos     valores válidos del desplegable (R.array.protocolo_array)
     */
    public static String validar(String protocolo, String[] permitidos, EditText host,
                                 EditText puerto, EditText carpeta, EditText usuario,
                                 EditText pass) {
        boolean protoOk = false;
        if (protocolo != null) {
            for (String p : permitidos) {
                if (p.equals(protocolo.trim())) { protoOk = true; break; }
            }
        }
        if (!protoOk) return "Seleccione un protocolo válido";

        String h = limpiarHost(texto(host));
        if (h.isEmpty()) return error(host, "Indique el servidor");
        if (h.startsWith(".") || h.startsWith("-") || h.endsWith(".") || h.contains(".."))
            return error(host, "Servidor no válido");
        host.setText(h);

        String p = limpiarPuerto(texto(puerto));
        int n;
        try { n = Integer.parseInt(p); } catch (NumberFormatException ex) { n = -1; }
        if (n < 1 || n > 65535) return error(puerto, "Puerto no válido (1-65535)");
        puerto.setText(String.valueOf(n));

        String c = limpiarCarpeta(texto(carpeta)).trim();
        if (c.isEmpty()) c = "/";
        carpeta.setText(c);

        String u = limpiarUsuario(texto(usuario));
        if (u.isEmpty()) return error(usuario, "Indique el usuario");
        usuario.setText(u);

        pass.setText(limpiarPass(texto(pass)));   // sin trim: solo se quitan caracteres de control
        return null;
    }
}