package com.celnet.syncro.models.instantvalues;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valores instantáneos S21 ya separados por secciones, para mostrarlos en tarjetas. Se obtienen
 * del texto que genera {@link InstantaneousValuesReader#leerValores}; los números se conservan
 * tal como los formateó el lector (con el formato decimal del dispositivo), sin reinterpretarlos.
 */
public final class ValoresS21 {

    public String timestamp = "N/A";
    /** Por fase 1..3: {tensión [V], corriente [A], FP}. */
    public final String[][] fases = new String[3][];
    public String fpTotal = "-";
    /** {primario, secundario, relación} de tensión [dV] y de corriente [dA]. */
    public String[] relacionTension;
    public String[] relacionCorriente;
    /** Filas fase 1, 2, 3 y total: {P+ [kW], P- [kW], Q+ [kvar], Q- [kvar]}. */
    public final String[][] potencias = new String[4][];
    /** Activa importada, activa exportada, reactiva Q1..Q4. */
    public final String[] energias = new String[6];

    private static final Pattern NUM = Pattern.compile("-?\\d[\\d.,]*");
    private static final Pattern FASE = Pattern.compile("^Fase\\s+(\\d)\\s*:(.*)$");
    private static final Pattern TOTAL = Pattern.compile("^Total\\s*:(.*)$");
    private static final Pattern REL = Pattern.compile("\\(\\s*([^/]+?)\\s*/\\s*([^)]+?)\\s*\\)\\s*=\\s*(\\S+)");

    private ValoresS21() {}

    /** @return los valores separados, o null si el texto no tiene la estructura esperada. */
    public static ValoresS21 parsear(String texto) {
        if (texto == null) return null;
        ValoresS21 v = new ValoresS21();
        int bloque = 0;          // 1 = tensión/corriente/FP, 2 = potencias
        int relacion = 0;        // 0 = tensión, 1 = corriente
        int energia = 0;

        for (String cruda : texto.split("\\r?\\n")) {
            String l = cruda.trim();
            if (l.isEmpty() || l.startsWith("---")) continue;

            if (l.startsWith("Timestamp")) {
                v.timestamp = l.substring(l.indexOf(':') + 1).trim();
            } else if (l.startsWith("Valores")) {
                bloque = l.contains("P+") ? 2 : 1;
            } else if (l.startsWith("FP")) {
                String[] n = numeros(l.substring(l.lastIndexOf(':') + 1));
                if (n.length > 0) v.fpTotal = n[0];
            } else if (l.startsWith("Relaci")) {
                relacion = l.contains("Corriente") ? 1 : 0;
            } else if (l.startsWith("Prim/sec")) {
                Matcher m = REL.matcher(l);
                if (m.find()) {
                    String[] r = {m.group(1), m.group(2), m.group(3)};
                    if (relacion == 0) v.relacionTension = r; else v.relacionCorriente = r;
                }
            } else if (l.startsWith("Activa") || l.startsWith("Reactiva")) {
                String[] n = numeros(l.substring(l.indexOf(':') + 1));
                if (energia < 6 && n.length > 0) v.energias[energia++] = n[0];
            } else {
                Matcher mf = FASE.matcher(l);
                Matcher mt = TOTAL.matcher(l);
                if (mf.matches()) {
                    int f = Integer.parseInt(mf.group(1)) - 1;
                    String[] n = numeros(mf.group(2));
                    if (f < 0 || f > 2) return null;
                    if (bloque == 1 && n.length == 3) v.fases[f] = n;
                    else if (bloque == 2 && n.length == 4) v.potencias[f] = n;
                } else if (mt.matches()) {
                    String[] n = numeros(mt.group(1));
                    if (n.length == 4) v.potencias[3] = n;
                }
            }
        }

        for (String[] f : v.fases) if (f == null) return null;
        for (String[] p : v.potencias) if (p == null) return null;
        if (v.relacionTension == null || v.relacionCorriente == null || energia != 6) return null;
        return v;
    }

    private static String[] numeros(String s) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        Matcher m = NUM.matcher(s);
        while (m.find()) {
            String t = m.group();
            while (t.endsWith(".") || t.endsWith(",")) t = t.substring(0, t.length() - 1);
            out.add(t);
        }
        return out.toArray(new String[0]);
    }
}