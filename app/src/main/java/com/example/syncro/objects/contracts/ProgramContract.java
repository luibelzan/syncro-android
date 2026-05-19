package com.example.syncro.objects.contracts;

import com.example.syncro.client.GXDLMSReader;

import gurux.dlms.*;
import gurux.dlms.enums.*;
import gurux.dlms.objects.*;
import java.text.SimpleDateFormat;
import java.util.*;
import gurux.dlms.enums.DataType;

/**
 * Clase para programar contratos tarifarios en contadores DLMS/COSEM.
 * Basada en el protocolo observado para tarifa 2.0TD (España).
 */
public class ProgramContract {

    // ─── Constantes OBIS ────────────────────────────────────────────────────

    /** Calendario pasivo del contrato N → 0.0.13.0.<contract>.255 */
    private static final String OBIS_ACTIVITY_CALENDAR = "0.0.13.0.%d.255";

    /** Tabla de días especiales → 0.0.11.0.0.255 */
    private static final String OBIS_SPECIAL_DAYS      = "0.0.11.0.0.255";

    /** Cierre de facturación del contrato N → 0.0.94.34.<contract>.255 */
    private static final String OBIS_END_OF_BILLING    = "0.0.94.34.%d.255";

    /** Límites de potencia (demand limiter) del contrato N → 0.0.17.0.<contract>.255 */
    private static final String OBIS_THRESHOLD         = "0.0.17.0.%d.255";

    // Número de períodos tarifarios soportados
    private static final int NUM_PERIODOS = 6;

    // ─── Método principal ────────────────────────────────────────────────────

    /**
     * Programa un contrato tarifario en el contador.
     *
     * @param reader      Sesión DLMS activa (autenticada).
     * @param contract    Número de contrato (obligatorio, ej: 1, 2…).
     * @param tarifa      Nombre de la tarifa, ej: "2.0TD" (null = no se escribe).
     * @param thresholds  Límites de potencia W para P1…P6; null o array vacío = no se escriben.
     *                    Si el array tiene menos de 6 elementos, los restantes se ignoran.
     * @param activacion  Fecha/hora de activación del calendario pasivo (null = FFFF/FF/FF 00:00:00).
     * @param cierreMes   Día del mes para el cierre de facturación en formato "YYYY/MM/DD"
     *                    (usar "FFFF/MM/DD" para repetición anual). Null = no se escribe.
     */
    public static void programarContrato(
            GXDLMSReader reader,
            int          contract,
            String       tarifa,
            long[]       thresholds,
            Date         activacion,
            String       cierreMes
    ) {
        System.out.println("=== Programando contrato " + contract
                + (tarifa != null ? " tarifa " + tarifa : "") + " ===");

        try {
            if (tarifa != null) {
                escribirCalendarioPasivo(reader, contract, tarifa, activacion);
            }

            // Días especiales: opcional, no todos los contadores lo soportan
            try {
                escribirDiasEspeciales(reader);
            } catch (Exception e) {
                System.out.println("Special Days Table no soportada, omitiendo: " + e.getMessage());
            }

            if (cierreMes != null) {
                escribirCierreFacturacion(reader, contract, cierreMes);
            }

            if (thresholds != null && thresholds.length > 0) {
                escribirUmbralesPotencia(reader, contract, thresholds);
            }

            System.out.println("=== Contrato " + contract + " programado correctamente ===");

        } catch (Exception e) {
            System.err.println("Error al programar contrato " + contract + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Paso 1: Calendario pasivo ───────────────────────────────────────────

    private static void escribirCalendarioPasivo(
            GXDLMSReader reader,
            int          contract,
            String       tarifa,
            Date         activacion
    ) throws Exception {

        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);

        // Atributo 6 — nombre del calendario pasivo
        System.out.println("Updating calendar_name_passive");
        cal.setCalendarNamePassive(tarifa);
        reader.writeObject(cal, 6);

        System.out.println("Skipping season/week/day profiles (precargados en contador)");

        // Atributo 10 — activate_passive_calendar_time
        System.out.println("Writing passive calendar activation time");
        if (activacion != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(activacion);

            int year  = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day   = c.get(Calendar.DAY_OF_MONTH);
            int hour  = c.get(Calendar.HOUR_OF_DAY);
            int min   = c.get(Calendar.MINUTE);
            int sec   = c.get(Calendar.SECOND);

            // Construir datetime como GXDateTime y asignarlo directamente
            // al campo interno de GXDLMSActivityCalendar mediante una subclase
            GXDateTime activacionDT = new GXDateTime(
                    year, month, day, hour, min, sec, -1
            );
            // deviation = not specified, status = wildcard
            activacionDT.setSkip(EnumSet.of(
                    DateTimeSkips.DEVIATION,
                    DateTimeSkips.STATUS
            ));

            // Forzar el valor en atributo 10 usando updateValue del cliente
            // igual que hace el software funcional — class-id 20, attr 10
            reader.getClient().updateValue(cal, 10, activacionDT);
            reader.writeObject(cal, 10);

            System.out.println("Passive Contract Updated !");
        } else {
            System.out.println("Passive Contract Updated ! (activation automatic)");
        }
    }

    /**
     * Construye los perfiles de día para la tarifa 2.0TD española.
     * Día 1 (L-V): P1 08:00, P2 10:00, P1 14:00, P2 18:00, P3 22:00
     * Día 2 (S-D/festivos): tarifa valle todo el día (P3)
     */
    private static GXDLMSDayProfile[] buildDayProfiles2_0TD() throws Exception {

        // Acciones de cambio de período: apuntan al script 0.0.0.0.0.0 (no-op)
        // En contadores reales el OBIS del script selector puede variar
        String scriptObis = "0.0.0.0.0.0";

        GXDLMSDayProfile day1 = new GXDLMSDayProfile();
        day1.setDayId(1);
        day1.setDaySchedules(new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 3),  // P3 - Valle
                makeDayAction("08:00:00", scriptObis, 1),  // P1 - Punta
                makeDayAction("10:00:00", scriptObis, 2),  // P2 - Llano
                makeDayAction("14:00:00", scriptObis, 1),  // P1 - Punta
                makeDayAction("18:00:00", scriptObis, 2),  // P2 - Llano
                makeDayAction("22:00:00", scriptObis, 3),  // P3 - Valle
        });

        GXDLMSDayProfile day2 = new GXDLMSDayProfile();
        day2.setDayId(2);
        day2.setDaySchedules(new GXDLMSDayProfileAction[]{
                makeDayAction("00:00:00", scriptObis, 3),  // P3 - Valle todo el día
        });

        return new GXDLMSDayProfile[]{ day1, day2 };
    }

    private static GXDLMSDayProfileAction makeDayAction(
            String time, String scriptObis, int scriptSelector
    ) throws Exception {
        GXDLMSDayProfileAction action = new GXDLMSDayProfileAction();
        action.setStartTime(new GXTime(time));
        action.setScriptLogicalName(scriptObis);
        action.setScriptSelector(scriptSelector);
        return action;
    }

    // ─── Paso 2: Días especiales ─────────────────────────────────────────────

    /**
     * Escribe la tabla de días especiales (festivos nacionales recurrentes).
     * Basado en la secuencia del log: 9 festivos nacionales España.
     */
    private static void escribirDiasEspeciales(GXDLMSReader reader) throws Exception {
        System.out.println("Updating Special Days Table...");

        GXDLMSSpecialDaysTable sdt = new GXDLMSSpecialDaysTable(OBIS_SPECIAL_DAYS);

        // Festivos nacionales España (FFFF = cualquier año → recurrente)
        // Formato: índice, fecha (GXDateTime), día-tipo
        GXDLMSSpecialDay[] specialDays = {
                makeSpecialDay(1, -1,  1,  1, 2), // Año Nuevo        01-ene
                makeSpecialDay(2, -1,  1,  6, 2), // Reyes            06-ene
                makeSpecialDay(3, -1,  5,  1, 2), // Día del Trabajo  01-may
                makeSpecialDay(4, -1,  8, 15, 2), // Asunción         15-ago
                makeSpecialDay(5, -1, 10, 12, 2), // Fiesta Nacional  12-oct
                makeSpecialDay(6, -1, 11,  1, 2), // Todos los Santos 01-nov
                makeSpecialDay(7, -1, 12,  6, 2), // Constitución     06-dic
                makeSpecialDay(8, -1, 12,  8, 2), // Inmaculada       08-dic
                makeSpecialDay(9, -1, 12, 25, 2), // Navidad          25-dic
        };

        sdt.setEntries(specialDays);
        reader.writeObject(sdt, 2); // attribute 2 = Entries
    }

    private static GXDLMSSpecialDay makeSpecialDay(
            int index, int year, int month, int day, int dayType
    ) {
        GXDLMSSpecialDay sd = new GXDLMSSpecialDay();
        sd.setIndex(index);
        GXDate date = new GXDate(year, month, day);
        // Asegurar que el día de semana es wildcard (0xFF)
        date.setSkip(EnumSet.of(DateTimeSkips.YEAR, DateTimeSkips.DAY_OF_WEEK));
        sd.setDate(date);
        sd.setDayId(dayType);
        return sd;
    }

    // ─── Paso 3: Cierre de facturación ───────────────────────────────────────

    /**
     * Escribe la fecha de cierre de facturación (end_of_billing).
     *
     * @param cierreMes Fecha en formato "YYYY/MM/DD". Usar "FFFF/MM/DD" para
     *                  repetición anual (ej: "FFFF/05/16" = día 16 de cada mayo).
     */
    private static void escribirCierreFacturacion(
            GXDLMSReader reader,
            int          contract,
            String       cierreMes
    ) throws Exception {
        System.out.println("Writing passive end of billing " + contract);

        String obis = String.format(OBIS_END_OF_BILLING, contract);
        GXDLMSData endOfBilling = new GXDLMSData(obis);

        // Construir GXDateTime respetando wildcards (FFFF para el año)
        String[] parts = cierreMes.split("/");
        int year  = parts[0].equalsIgnoreCase("FFFF") ? 0xFFFF : Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day   = Integer.parseInt(parts[2]);

        Calendar cal = Calendar.getInstance();
        cal.set(year == 0xFFFF ? 2000 : year, month - 1, day, 0, 0, 0);
        GXDateTime dt = new GXDateTime(cal.getTime());

        // Aplicar skip de año si es FFFF
        if (year == 0xFFFF) {
            Set<DateTimeSkips> skips = dt.getSkip();
            skips.add(DateTimeSkips.YEAR);
            dt.setSkip(EnumSet.copyOf(skips));
        }

        // El valor es un octet-string de 12 bytes (formato DLMS DateTime)
        endOfBilling.setValue(dt);
        reader.writeObject(endOfBilling, 2); // attribute 2 = Value
    }

    // ─── Paso 4: Umbrales de potencia ────────────────────────────────────────

    /**
     * Escribe los límites de potencia para los períodos P1…P6.
     *
     * @param thresholds Array con los vatios para cada período [P1, P2, P3, P4, P5, P6].
     *                   Si tiene menos de 6 elementos, los períodos sobrantes no se tocan.
     */
    private static void escribirUmbralesPotencia(
            GXDLMSReader reader,
            int          contract,
            long[]       thresholds
    ) throws Exception {

        int numPeriodos = Math.min(thresholds.length, NUM_PERIODOS);

        for (int p = 0; p < numPeriodos; p++) {
            int periodoNum = p + 1;
            long valorW    = thresholds[p];

            // Campo B siempre = 1, campo E identifica período (0x0B..0x10)
            // El contrato NO va en el OBIS de threshold, va implícito en la asociación
            String obisThreshold = String.format(
                    "0.1.94.34.%d.255", 0x0A + periodoNum
            );

            System.out.println("Writing new threshold " + periodoNum + "    -> " + valorW);

            GXDLMSRegister reg = new GXDLMSRegister(obisThreshold);
            reg.setDataType(2, DataType.UINT32);
            reg.setValue(valorW);
            reader.writeObject(reg, 2);
        }

        System.out.println("Pasive Thresholds updated");
    }
}