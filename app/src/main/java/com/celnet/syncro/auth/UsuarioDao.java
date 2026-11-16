package com.celnet.syncro.auth;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UsuarioDao {

    /** Lanza SQLiteConstraintException si el nombre de usuario ya existe. */
    @Insert
    long insertar(Usuario usuario);

    @Update
    void actualizar(Usuario usuario);

    /** La comparación no distingue mayúsculas (la columna usa COLLATE NOCASE). */
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombre LIMIT 1")
    Usuario porNombre(String nombre);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    Usuario porId(String id);

    @Query("SELECT * FROM usuarios ORDER BY nombreUsuario")
    List<Usuario> todos();

    @Query("SELECT COUNT(*) FROM usuarios")
    int contar();

    @Query("SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = 1")
    int contarAdminsActivos();
}