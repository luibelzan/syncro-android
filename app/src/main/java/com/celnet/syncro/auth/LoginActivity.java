package com.celnet.syncro.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.MainActivity;
import com.celnet.syncro.R;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    /** Intent de la pantalla protegida que pidió el login (lo rellena AuthManager.exigirSesion). */
    public static final String EXTRA_DESTINO = "auth_destino";

    /** Destino por defecto si el login se abre sin una pantalla de origen. */
    private static final Class<?> DESTINO = MainActivity.class;

    private AuthManager auth;
    private EditText etUsuario;
    private EditText etPassword;
    private TextView txtError;
    private Button btnEntrar;
    private View progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        auth = AuthManager.get(this);
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        txtError = findViewById(R.id.txtError);
        btnEntrar = findViewById(R.id.btnEntrar);
        progreso = findViewById(R.id.progreso);

        btnEntrar.setOnClickListener(v -> entrar());
        etPassword.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_DONE) {
                entrar();
                return true;
            }
            return false;
        });
    }

    private void entrar() {
        String usuario = etUsuario.getText().toString().trim();
        char[] password = etPassword.getText().toString().toCharArray();
        if (usuario.isEmpty() || password.length == 0) {
            txtError.setText("Introduce usuario y contraseña");
            return;
        }
        txtError.setText("");
        setCargando(true);

        auth.io().execute(() -> {
            AuthManager.ResultadoLogin r = auth.login(usuario, password);
            Arrays.fill(password, '\0');
            runOnUiThread(() -> mostrarResultado(r));
        });
    }

    private void mostrarResultado(AuthManager.ResultadoLogin r) {
        setCargando(false);
        switch (r.estado) {
            case OK:
                etPassword.setText("");
                if (r.usuario.debeCambiarPassword) {
                    CambiarPasswordDialog.mostrar(this, true, this::irAlInicio);
                } else {
                    irAlInicio();
                }
                break;
            case BLOQUEADO:
                long minutos = Math.max(1,
                        (r.bloqueadoHasta - System.currentTimeMillis() + 59_999) / 60_000);
                txtError.setText("Demasiados intentos. Vuelve a probar en " + minutos + " min.");
                break;
            case DESACTIVADO:
                txtError.setText("Este usuario está desactivado. Consulta con el administrador.");
                break;
            default:
                txtError.setText(r.intentosRestantes > 0 && r.intentosRestantes < AuthManager.MAX_INTENTOS
                        ? "Usuario o contraseña incorrectos (" + r.intentosRestantes + " intentos restantes)"
                        : "Usuario o contraseña incorrectos");
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void irAlInicio() {
        Intent destino = getIntent().getParcelableExtra(EXTRA_DESTINO);
        if (destino == null) {
            destino = new Intent(this, DESTINO);
        }
        startActivity(destino);
        finish();
    }

    private void setCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!cargando);
        etUsuario.setEnabled(!cargando);
        etPassword.setEnabled(!cargando);
    }
}