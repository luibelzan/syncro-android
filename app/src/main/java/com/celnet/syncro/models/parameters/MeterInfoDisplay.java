package com.celnet.syncro.models.parameters;

import java.util.HashMap;
import java.util.Map;

/**
 * Descompone los campos "equipo" y "tipo" de MeterInfo (que llegan concatenados
 * desde el contador) en los valores individuales que se muestran en pantalla,
 * y resuelve el nombre de fabricante a partir de su letra.
 */
public class MeterInfoDisplay {

    public String numeroSerie;
    public String fabricanteLetra;
    public String fabricanteNombre;
    public String modelo;
    public String fabricadoEn;
    public String firmwareDlms;
    public String tipoEquipo;
    public String companion;

    private static final Map<String, String> FABRICANTES = new HashMap<>();
    static {
        FABRICANTES.put("I", "SAGEMCOM");
        FABRICANTES.put("C", "LANDIS&GYR");
        FABRICANTES.put("Z", "ZIV");
        FABRICANTES.put("X", "KAIFA");
        FABRICANTES.put("9", "SOGECAM");
        FABRICANTES.put("Q", "CIRCUTOR");
        FABRICANTES.put("H", "GENERAL ELECTRIC");
        FABRICANTES.put("B", "ITRON");
        FABRICANTES.put("3", "SANXING");
        FABRICANTES.put("J", "ELSTER");
    }

    /**
     * @param serial   res.serialNumber (número de serie, no requiere parseo)
     * @param equipo   MeterInfo.equipo, formato [fabricante(1)][modelo(N)][año(2)], p.ej. "IBT17"
     * @param tipo     MeterInfo.tipo, formato "[tipoEquipo] [companion]", p.ej. "contador DLMS0107"
     * @param firmware MeterInfo.firmware, se muestra tal cual como "Firmware DLMS"
     */
    public static MeterInfoDisplay parse(String serial, String equipo, String tipo, String firmware) {
        MeterInfoDisplay d = new MeterInfoDisplay();
        d.numeroSerie = serial != null ? serial : "-";
        d.firmwareDlms = firmware != null ? firmware : "-";

        // ── equipo: fabricante + modelo + año ────────────────────────────
        if (equipo != null && equipo.length() >= 3) {
            d.fabricanteLetra = equipo.substring(0, 1).toUpperCase();
            d.fabricanteNombre = FABRICANTES.getOrDefault(d.fabricanteLetra, "Desconocido (" + d.fabricanteLetra + ")");
            d.modelo = equipo.substring(1, equipo.length() - 2);
            String sufijoAnio = equipo.substring(equipo.length() - 2);
            d.fabricadoEn = "20" + sufijoAnio;
        } else {
            d.fabricanteLetra = "";
            d.fabricanteNombre = "-";
            d.modelo = "-";
            d.fabricadoEn = "-";
        }

        // ── tipo: "tipoEquipo companion" ──────────────────────────────────
        if (tipo != null && tipo.contains(" ")) {
            String[] partes = tipo.split(" ", 2);
            d.tipoEquipo = partes[0];
            d.companion = partes[1];
        } else {
            d.tipoEquipo = tipo != null ? tipo : "-";
            d.companion = "-";
        }

        return d;
    }

    /**
     * Nombre del recurso drawable esperado para el logo de este fabricante,
     * por ejemplo "logo_sagemcom". El recurso debe existir en res/drawable;
     * si no existe, usa un icono de repuesto (ver IdsActivity).
     */
    public String logoResourceName() {
        return "logo_" + fabricanteNombre
                .toLowerCase()
                .replace("&", "")
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "");
    }
}