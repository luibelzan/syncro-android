package com.celnet.syncro.auth;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos local de usuarios.
 *
 * IMPORTANTE: al cambiar el esquema, subir "version" y escribir una Migration.
 * No usar fallbackToDestructiveMigration(): borraría todos los usuarios.
 */
@Database(entities = {Usuario.class}, version = 1, exportSchema = false)
public abstract class AuthDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();

    private static volatile AuthDatabase instancia;

    static AuthDatabase get(Context contexto) {
        if (instancia == null) {
            synchronized (AuthDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                            contexto.getApplicationContext(),
                            AuthDatabase.class,
                            "syncro_auth.db").build();
                }
            }
        }
        return instancia;
    }
}