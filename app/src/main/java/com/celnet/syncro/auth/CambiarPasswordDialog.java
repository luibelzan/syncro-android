package com.celnet.syncro.auth;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.celnet.syncro.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

/** Diálogo para que el usuario con sesión cambie su propia contraseña. */
public final class CambiarPasswordDialog {

    private CambiarPasswordDialog() {
    }

    /**
     * @param obligatorio si es true no se puede cancelar (primer acceso o contraseña restablecida)
     * @param alTerminar  se ejecuta en el hilo principal cuando la contraseña se ha guardado
     */
    public static void mostrar(Activity actividad, boolean obligatorio, Runnable alTerminar) {
        AuthManager auth = AuthManager.get(actividad);
        View vista = LayoutInflater.from(actividad).inflate(R.layout.dialog_cambiar_password, null);
        TextView txtMensaje = vista.findViewById(R.id.txtMensaje);
        EditText etNueva = vista.findViewById(R.id.etNueva);
        EditText etRepetir = vista.findViewById(R.id.etRepetir);
        TextView txtError = vista.findViewById(R.id.txtError);

        txtMensaje.setText(obligatorio
                ? "Por seguridad debes elegir una contraseña nueva antes de continuar."
                : "Elige tu nueva contraseña.");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(actividad)
                .setTitle("Cambiar contraseña")
                .setView(vista)
                .setCancelable(!obligatorio)
                .setPositiveButton("Guardar", null); // se sobrescribe abajo para no cerrar si hay error
        if (!obligatorio) {
            builder.setNegativeButton("Cancelar", null);
        }

        AlertDialog dialogo = builder.create();
        dialogo.show();

        Button guardar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
        guardar.setOnClickListener(v -> {
            char[] nueva = etNueva.getText().toString().toCharArray();
            String repetida = etRepetir.getText().toString();
            if (!new String(nueva).equals(repetida)) {
                txtError.setText("Las contraseñas no coinciden");
                return;
            }
            txtError.setText("");
            guardar.setEnabled(false);
            auth.io().execute(() -> {
                String error = auth.cambiarPropiaPassword(nueva);
                Arrays.fill(nueva, '\0');
                actividad.runOnUiThread(() -> {
                    if (error == null) {
                        dialogo.dismiss();
                        Toast.makeText(actividad, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                        if (alTerminar != null) {
                            alTerminar.run();
                        }
                    } else {
                        txtError.setText(error);
                        guardar.setEnabled(true);
                    }
                });
            });
        });
    }
}