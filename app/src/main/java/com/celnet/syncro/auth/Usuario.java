package com.celnet.syncro.auth;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

/**
 * Usuario local. El id es un UUID (no autoincremental) para poder sincronizar con
 * un servidor más adelante sin choques entre dispositivos.
 */
@Entity(tableName = "usuarios", indices = {@Index(value = {"nombreUsuario"}, unique = true)})
public class Usuario {

    @PrimaryKey
    @NonNull
    public String id = UUID.randomUUID().toString();

    /** Único sin distinguir mayúsculas de minúsculas. */
    @NonNull
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    public String nombreUsuario = "";

    @NonNull public String hash = "";
    @NonNull public String sal = "";
    public int iteraciones;
    @NonNull public String algoritmo = "";

    /** Nombre del enum {@link Rol}. */
    @NonNull public String rol = Rol.LECTURA.name();

    /** Los usuarios se desactivan, no se borran. */
    public boolean activo = true;

    public boolean debeCambiarPassword;

    public long creadoEn;
    public String creadoPor;
    public long ultimoAcceso;

    public int intentosFallidos;
    public long bloqueadoHasta;

    public Rol rolEnum() {
        return Rol.deNombre(rol);
    }
}