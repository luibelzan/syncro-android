package com.celnet.syncro.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celnet.syncro.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Gestión de usuarios. Solo accesible para administradores. */
public class UsuariosActivity extends AppCompatActivity {

    private interface Operacion {
        /** Devuelve un mensaje de error o null si todo fue bien. */
        String ejecutar();
    }

    private AuthManager auth;
    private UsuarioAdapter adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthManager.exigirSesion(this)) {
            return;
        }
        if (!AuthManager.puedeGestionarUsuarios()) {
            Toast.makeText(this, "Solo un administrador puede gestionar usuarios",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_usuarios);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        auth = AuthManager.get(this);

        RecyclerView lista = findViewById(R.id.listaUsuarios);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new UsuarioAdapter(this::mostrarAcciones);
        lista.setAdapter(adaptador);

        ExtendedFloatingActionButton btnNuevo = findViewById(R.id.btnNuevoUsuario);
        btnNuevo.setOnClickListener(v -> mostrarNuevoUsuario());

        recargar();
    }

    private void recargar() {
        auth.io().execute(() -> {
            List<Usuario> usuarios = auth.listar();
            runOnUiThread(() -> adaptador.setItems(usuarios));
        });
    }

    /** Ejecuta una operación del AuthManager fuera del hilo principal y avisa del resultado. */
    private void ejecutar(Operacion operacion, String mensajeOk) {
        auth.io().execute(() -> {
            String error = operacion.ejecutar();
            runOnUiThread(() -> {
                Toast.makeText(this, error == null ? mensajeOk : error, Toast.LENGTH_LONG).show();
                recargar();
            });
        });
    }

    // ---------------------------------------------------------------- acciones

    private void mostrarAcciones(Usuario u) {
        String[] acciones = {"Cambiar perfil", "Restablecer contraseña",
                u.activo ? "Desactivar usuario" : "Activar usuario"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(u.nombreUsuario)
                .setItems(acciones, (d, cual) -> {
                    if (cual == 0) {
                        mostrarCambiarRol(u);
                    } else if (cual == 1) {
                        mostrarRestablecer(u);
                    } else {
                        ejecutar(() -> auth.setActivo(u.id, !u.activo),
                                u.activo ? "Usuario desactivado" : "Usuario activado");
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void mostrarCambiarRol(Usuario u) {
        Rol[] roles = Rol.values();
        String[] etiquetas = new String[roles.length];
        int seleccion = 0;
        for (int i = 0; i < roles.length; i++) {
            etiquetas[i] = roles[i].etiqueta();
            if (roles[i] == u.rolEnum()) {
                seleccion = i;
            }
        }
        final int[] elegido = {seleccion};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Perfil de " + u.nombreUsuario)
                .setSingleChoiceItems(etiquetas, seleccion, (d, cual) -> elegido[0] = cual)
                .setPositiveButton("Guardar", (d, w) ->
                        ejecutar(() -> auth.cambiarRol(u.id, roles[elegido[0]]), "Perfil actualizado"))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarRestablecer(Usuario u) {
        EditText campo = new EditText(this);
        campo.setHint("Contraseña temporal");
        campo.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int margen = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout contenedor = new android.widget.FrameLayout(this);
        contenedor.setPadding(margen, margen / 2, margen, 0);
        contenedor.addView(campo);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Restablecer contraseña de " + u.nombreUsuario)
                .setMessage("El usuario tendrá que cambiarla en su próximo acceso.")
                .setView(contenedor)
                .setPositiveButton("Restablecer", (d, w) -> {
                    char[] temporal = campo.getText().toString().toCharArray();
                    ejecutar(() -> {
                        String error = auth.restablecerPassword(u.id, temporal);
                        Arrays.fill(temporal, '\0');
                        return error;
                    }, "Contraseña restablecida");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarNuevoUsuario() {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_usuario_nuevo, null);
        EditText etNombre = vista.findViewById(R.id.etUsuarioNuevo);
        EditText etPassword = vista.findViewById(R.id.etPasswordNuevo);
        RadioButton rbEscritura = vista.findViewById(R.id.rbEscritura);
        RadioButton rbAdmin = vista.findViewById(R.id.rbAdmin);
        TextView txtError = vista.findViewById(R.id.txtErrorNuevo);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Nuevo usuario")
                .setView(vista)
                .setPositiveButton("Crear", null) // se sobrescribe para no cerrar si hay error
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();

        Button crear = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        crear.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString();
            char[] password = etPassword.getText().toString().toCharArray();
            Rol rol = rbAdmin.isChecked() ? Rol.ADMIN
                    : rbEscritura.isChecked() ? Rol.ESCRITURA : Rol.LECTURA;

            crear.setEnabled(false);
            auth.io().execute(() -> {
                String error = auth.crearUsuario(nombre, password, rol);
                Arrays.fill(password, '\0');
                runOnUiThread(() -> {
                    if (error == null) {
                        dialogo.dismiss();
                        Toast.makeText(this, "Usuario creado. Deberá cambiar la contraseña al entrar.",
                                Toast.LENGTH_LONG).show();
                        recargar();
                    } else {
                        txtError.setText(error);
                        crear.setEnabled(true);
                    }
                });
            });
        });
    }

    // ---------------------------------------------------------------- adaptador

    private static class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.Fila> {

        interface AlPulsar {
            void pulsado(Usuario u);
        }

        private final List<Usuario> items = new ArrayList<>();
        private final AlPulsar listener;

        UsuarioAdapter(AlPulsar listener) {
            this.listener = listener;
        }

        void setItems(List<Usuario> nuevos) {
            items.clear();
            items.addAll(nuevos);
            notifyDataSetChanged();
        }

        @Override
        public Fila onCreateViewHolder(ViewGroup padre, int tipo) {
            View v = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_usuario, padre, false);
            return new Fila(v);
        }

        @Override
        public void onBindViewHolder(Fila fila, int posicion) {
            Usuario u = items.get(posicion);
            fila.txtNombre.setText(u.nombreUsuario);
            StringBuilder detalle = new StringBuilder(u.rolEnum().etiqueta());
            if (!u.activo) {
                detalle.append(" · Desactivado");
            }
            if (u.debeCambiarPassword) {
                detalle.append(" · Pendiente de cambiar contraseña");
            }
            fila.txtDetalle.setText(detalle);
            fila.itemView.setAlpha(u.activo ? 1f : 0.5f);
            fila.itemView.setOnClickListener(v -> listener.pulsado(u));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Fila extends RecyclerView.ViewHolder {
            final TextView txtNombre;
            final TextView txtDetalle;

            Fila(View v) {
                super(v);
                txtNombre = v.findViewById(R.id.txtNombre);
                txtDetalle = v.findViewById(R.id.txtDetalle);
            }
        }
    }
}