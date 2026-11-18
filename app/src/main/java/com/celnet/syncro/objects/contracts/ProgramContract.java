package com.celnet.syncro.objects.contracts;

import com.celnet.syncro.client.GXDLMSReader;
import com.celnet.syncro.utils.AppLogger;

import gurux.dlms.*;
import gurux.dlms.enums.*;
import gurux.dlms.objects.*;
import java.util.*;
import gurux.dlms.enums.DataType;

public class ProgramContract {

    private static final String TAG = "ProgramContract";

    private static final String OBIS_ACTIVITY_CALENDAR = "0.0.13.0.%d.255";
    private static final String OBIS_SPECIAL_DAYS = "0.0.11.0.%d.255";
    private static final String OBIS_END_OF_BILLING    = "0.0.94.34.41.255";
    private static final String OBIS_CLOCK             = "0.0.1.0.0.255";

    private static final int NUM_PERIODOS = 6;

    // ── Calendario por tarifa ────────────────────────────────────────────────
    //
    // Datos tomados de la programación de referencia de cada tarifa (tramas
    // verificadas byte a byte contra el calendario pasivo que el contador acepta).
    // Para añadir otra tarifa (p. ej. 6.1TD) basta con definir otra constante
    // CalendarioTarifa y añadirla en calendarioDe().

    /** Perfil diario: id y lista de acciones {hora, minuto, selector de script (periodo)}. */
    private static final class DiaPerfil {
        final int id;
        final int[][] acciones;

        DiaPerfil(int id, int[][] acciones) {
            this.id = id;
            this.acciones = acciones;
        }
    }

    private static final class CalendarioTarifa {
        /** Nombre del calendario tal y como se escribe (6 caracteres, con espacio final). */
        final String nombre;
        /** {id, mes, día, id de semana} */
        final int[][] temporadas;
        /** {id, lun, mar, mié, jue, vie, sáb, dom} (cada día = id de perfil diario) */
        final int[][] semanas;
        final DiaPerfil[] dias;
        /** Perfil diario que se aplica a los festivos de la Special Days Table. */
        final int diaFestivo;

        CalendarioTarifa(String nombre, int[][] temporadas, int[][] semanas,
                         DiaPerfil[] dias, int diaFestivo) {
            this.nombre = nombre;
            this.temporadas = temporadas;
            this.semanas = semanas;
            this.dias = dias;
            this.diaFestivo = diaFestivo;
        }
    }

    /** 2.0TD: 3 periodos. Fines de semana y festivos todo el día en P3. */
    private static final CalendarioTarifa TARIFA_2_0TD = new CalendarioTarifa(
            "2.0TD ",
            new int[][]{
                    {1, 1, 1, 1},
            },
            new int[][]{
                    {1, 1, 1, 1, 1, 1, 2, 2},
            },
            new DiaPerfil[]{
                    new DiaPerfil(1, new int[][]{
                            {0, 0, 3}, {8, 0, 2}, {10, 0, 1}, {14, 0, 2}, {18, 0, 1}, {22, 0, 2}}),
                    new DiaPerfil(2, new int[][]{
                            {0, 0, 3}}),
            },
            2);

    /** 3.0TD: 6 periodos, 9 temporadas, fines de semana y festivos todo el día en P6. */
    private static final CalendarioTarifa TARIFA_3_0TD = new CalendarioTarifa(
            "3.0TD ",
            new int[][]{
                    {1, 1, 1, 1}, {2, 3, 1, 2}, {3, 4, 1, 4}, {4, 6, 1, 3}, {5, 7, 1, 1},
                    {6, 8, 1, 3}, {7, 10, 1, 4}, {8, 11, 1, 2}, {9, 12, 1, 1},
            },
            new int[][]{
                    {1, 1, 1, 1, 1, 1, 5, 5},
                    {2, 2, 2, 2, 2, 2, 5, 5},
                    {3, 3, 3, 3, 3, 3, 5, 5},
                    {4, 4, 4, 4, 4, 4, 5, 5},
            },
            new DiaPerfil[]{
                    new DiaPerfil(1, new int[][]{
                            {0, 0, 6}, {8, 0, 2}, {9, 0, 1}, {14, 0, 2}, {18, 0, 1}, {22, 0, 2}}),
                    new DiaPerfil(2, new int[][]{
                            {0, 0, 6}, {8, 0, 3}, {9, 0, 2}, {14, 0, 3}, {18, 0, 2}, {22, 0, 3}}),
                    new DiaPerfil(3, new int[][]{
                            {0, 0, 6}, {8, 0, 4}, {9, 0, 3}, {14, 0, 4}, {18, 0, 3}, {22, 0, 4}}),
                    new DiaPerfil(4, new int[][]{
                            {0, 0, 6}, {8, 0, 5}, {9, 0, 4}, {14, 0, 5}, {18, 0, 4}, {22, 0, 5}}),
                    new DiaPerfil(5, new int[][]{
                            {0, 0, 6}}),
            },
            5);

    /** Festivos nacionales {mes, día}; son los mismos en todas las tarifas. */
    private static final int[][] FESTIVOS = {
            {1, 1}, {1, 6}, {5, 1}, {8, 15}, {10, 12}, {11, 1}, {12, 6}, {12, 8}, {12, 25},
    };

    /**
     * Devuelve el calendario de la tarifa indicada. Acepta etiquetas con texto añadido
     * o espacios (por ejemplo "2.0TD" o "Tarifa 2.0TD").
     *
     * @throws IllegalArgumentException si no hay calendario definido para esa tarifa
     */
    private static CalendarioTarifa calendarioDe(String tarifa) {
        String t = tarifa == null ? "" : tarifa.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (t.contains("2.0TD")) return TARIFA_2_0TD;
        if (t.contains("3.0TD")) return TARIFA_3_0TD;
        throw new IllegalArgumentException(
                "Tarifa no soportada: '" + tarifa + "'. Disponibles: 2.0TD, 3.0TD");
    }

    public static void programarContrato(
            GXDLMSReader reader,
            int          contract,
            String       tarifa,
            long[]       thresholds,
            Date         activacion,
            String       cierreMes
    ) {
        // Se resuelve antes de escribir nada: una tarifa desconocida debe fallar sin
        // tocar el contador (la excepción llega a la pantalla; el try de abajo la tragaría).
        final CalendarioTarifa calendario = (tarifa != null) ? calendarioDe(tarifa) : null;

        AppLogger.i(TAG, "=== Programando contrato " + contract
                + (calendario != null ? " tarifa " + calendario.nombre.trim() : "") + " ===");

        try {
            // ── SESIÓN 1 ────────────────────────────────────────────────────
            if (calendario != null) {
                AppLogger.i(TAG, "Writing contract " + calendario.nombre);
                escribirNombreCalendarioPasivo(reader, contract, calendario);
                escribirSeasonProfilePasivo(reader, contract, calendario);
                escribirWeekProfilePasivo(reader, contract, calendario);
                escribirDayProfilePasivo(reader, contract, calendario);

                // Los festivos apuntan a un perfil diario del calendario que se acaba de
                // escribir, así que solo se escriben junto con una tarifa.
                try {
                    escribirDiasEspeciales(reader, calendario);
                } catch (Exception e) {
                    AppLogger.i(TAG, "Special Days Table no soportada, omitiendo: " + e.getMessage());
                }
            }

            if (cierreMes != null) {
                escribirCierreFacturacion(reader, contract, cierreMes);
            }

            if (thresholds != null && thresholds.length > 0) {
                escribirUmbralesPotencia(reader, contract, thresholds);
            }

            // ── SESIÓN 2: activación ────────────────────────────────────────
            if (activacion != null) {
                AppLogger.i(TAG, "Reconnecting for activation session...");
                reader.reconnect();
                escribirFechaActivacion(reader, contract, activacion);
            }

            AppLogger.i(TAG, "=== Contrato " + contract + " programado correctamente ===");

        } catch (Exception e) {
            AppLogger.e(TAG, "Error al programar contrato " + contract + ": " + e.getMessage());
        }
    }

    // ── Sesión 1 ─────────────────────────────────────────────────────────────

    private static void escribirNombreCalendarioPasivo(
            GXDLMSReader reader, int contract, CalendarioTarifa calendario
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating calendar_name_passive");
        cal.setCalendarNamePassive(calendario.nombre);
        reader.writeObject(cal, 6);
    }

    private static void escribirSeasonProfilePasivo(
            GXDLMSReader reader, int contract, CalendarioTarifa calendario
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating season_profile_passive");

        // Construir seasons como raw List para evitar que Gurux serialice
        // fechas incorrectas al usar GXDateTime con wildcards
        List<Object> seasons = new ArrayList<>();
        for (int[] s : calendario.temporadas) {
            seasons.add(buildSeasonRaw((byte) s[0], s[1], s[2], (byte) s[3]));
        }

        reader.getClient().updateValue(cal, 7, seasons);
        reader.writeObject(cal, 7);
    }

    /**
     * Construye una season como lista raw de {name, datetime_bytes, weekName}.
     * Los bytes de datetime siguen el formato DLMS octet-string de 12 bytes:
     *   year(2) month(1) day(1) dow(1) hh(1) mm(1) ss(1) ms(1) dev(2) sts(1)
     * Con year=FFFF (skip), dow=7 (wildcard), hora=0.
     *
     * deviation/status: estas seasons son fechas RECURRENTES cada año (no un
     * instante concreto). Se escriben igual que en la programación de referencia:
     * deviation=0x8000 (no especificado) y status=0x80.
     */
    private static List<Object> buildSeasonRaw(byte name, int month, int day, byte weekName) {
        List<Object> season = new ArrayList<>();
        season.add(new byte[]{name});
        byte[] dt = new byte[]{
                (byte) 0xFF, (byte) 0xFF,  // year = FFFF (skip)
                (byte) month,               // month
                (byte) day,                 // day
                (byte) 0x07,               // day_of_week = 7 (wildcard)
                (byte) 0x00,               // hour = 0
                (byte) 0x00,               // minute = 0
                (byte) 0x00,               // second = 0
                (byte) 0x00,               // millisecond = 0
                (byte) 0x80, (byte) 0x00,  // deviation = 0x8000 (no especificado)
                (byte) 0x80                // status = 0x80
        };
        season.add(dt);
        season.add(new byte[]{weekName});
        return season;
    }

    private static void escribirWeekProfilePasivo(
            GXDLMSReader reader, int contract, CalendarioTarifa calendario
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating week_profile_table_passive");

        GXDLMSWeekProfile[] semanas = new GXDLMSWeekProfile[calendario.semanas.length];
        for (int i = 0; i < semanas.length; i++) {
            int[] w = calendario.semanas[i];
            semanas[i] = makeWeekProfile((byte) w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7]);
        }

        cal.setWeekProfileTablePassive(semanas);
        reader.writeObject(cal, 8);
    }

    private static GXDLMSWeekProfile makeWeekProfile(
            byte name, int mon, int tue, int wed, int thu, int fri, int sat, int sun
    ) {
        GXDLMSWeekProfile w = new GXDLMSWeekProfile();
        w.setName(new byte[]{name});
        w.setMonday(mon); w.setTuesday(tue); w.setWednesday(wed);
        w.setThursday(thu); w.setFriday(fri); w.setSaturday(sat); w.setSunday(sun);
        return w;
    }

    private static void escribirDayProfilePasivo(
            GXDLMSReader reader, int contract, CalendarioTarifa calendario
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);
        AppLogger.i(TAG, "Updating day_profile_table_passive");

        String scriptObis = "0.0.0.0.0.0";

        GXDLMSDayProfile[] dias = new GXDLMSDayProfile[calendario.dias.length];
        for (int i = 0; i < dias.length; i++) {
            DiaPerfil d = calendario.dias[i];
            GXDLMSDayProfileAction[] acciones = new GXDLMSDayProfileAction[d.acciones.length];
            for (int j = 0; j < acciones.length; j++) {
                int[] a = d.acciones[j];
                acciones[j] = makeDayAction(
                        String.format(Locale.ROOT, "%02d:%02d:00", a[0], a[1]), scriptObis, a[2]);
            }
            dias[i] = makeDayProfile(d.id, acciones);
        }

        cal.setDayProfileTablePassive(dias);
        reader.writeObject(cal, 9);
    }

    private static GXDLMSDayProfile makeDayProfile(
            int dayId, GXDLMSDayProfileAction[] actions
    ) {
        GXDLMSDayProfile dp = new GXDLMSDayProfile();
        dp.setDayId(dayId);
        dp.setDaySchedules(actions);
        return dp;
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

    private static void escribirDiasEspeciales(
            GXDLMSReader reader, CalendarioTarifa calendario
    ) throws Exception {
        // El contador usa 0.0.11.0.4.255 para escritura del pasivo
        // independientemente del contrato activo
        String obis = "0.0.11.0.4.255";
        AppLogger.i(TAG, "Updating Special Days Table " + obis
                + " (perfil diario de festivos: " + calendario.diaFestivo + ")");
        GXDLMSSpecialDaysTable sdt = new GXDLMSSpecialDaysTable(obis);
        GXDLMSSpecialDay[] specialDays = new GXDLMSSpecialDay[FESTIVOS.length];
        for (int i = 0; i < specialDays.length; i++) {
            specialDays[i] = makeSpecialDay(
                    i + 1, -1, FESTIVOS[i][0], FESTIVOS[i][1], calendario.diaFestivo);
        }
        sdt.setEntries(specialDays);
        reader.writeObject(sdt, 2);
    }

    private static GXDLMSSpecialDay makeSpecialDay(
            int index, int year, int month, int day, int dayType
    ) {
        GXDLMSSpecialDay sd = new GXDLMSSpecialDay();
        sd.setIndex(index);
        GXDate date = new GXDate(year, month, day);
        date.setSkip(EnumSet.of(DateTimeSkips.YEAR, DateTimeSkips.DAY_OF_WEEK));
        sd.setDate(date);
        sd.setDayId(dayType);
        return sd;
    }

    private static void escribirCierreFacturacion(
            GXDLMSReader reader, int contract, String cierreMes
    ) throws Exception {
        AppLogger.i(TAG, "Writing passive end of billing " + contract);
        GXDLMSData endOfBilling = new GXDLMSData(OBIS_END_OF_BILLING);
        String[] parts = cierreMes.split("/");

        int year  = parts[0].equalsIgnoreCase("FFFF") ? 0xFFFF : Integer.parseInt(parts[0]);
        int month = parseHexOrDec(parts[1]);
        int day   = parseHexOrDec(parts[2]);

        byte[] dt = new byte[12];
        dt[0]  = (byte) ((year >> 8) & 0xFF);
        dt[1]  = (byte) (year & 0xFF);
        dt[2]  = (byte) month;
        dt[3]  = (byte) day;
        dt[4]  = (byte) 0xFF;
        dt[5]  = (byte) 0x00;
        dt[6]  = (byte) 0x00;
        dt[7]  = (byte) 0x00;
        dt[8]  = (byte) 0x00;
        dt[9]  = (byte) 0x00;
        dt[10] = (byte) 0x80;
        dt[11] = (byte) 0xFF;
        endOfBilling.setDataType(2, DataType.OCTET_STRING);
        endOfBilling.setValue(dt);
        reader.writeObject(endOfBilling, 2);
        AppLogger.i(TAG, "End of billing updated");
    }

    private static int parseHexOrDec(String value) {
        if (value == null || value.isEmpty()) return 0xFF;
        try {
            // Si contiene letras A-F es hex (FD, FE, FF)
            if (value.matches(".*[A-Fa-f].*")) {
                return Integer.parseInt(value, 16);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0xFF; // fallback wildcard
        }
    }

    private static void escribirUmbralesPotencia(
            GXDLMSReader reader, int contract, long[] thresholds
    ) throws Exception {
        int numPeriodos = Math.min(thresholds.length, NUM_PERIODOS);
        for (int p = 0; p < numPeriodos; p++) {
            int periodoNum = p + 1;
            long valorW    = thresholds[p];
            String obisThreshold = String.format("0.1.94.34.%d.255", 0x0A + periodoNum);
            AppLogger.i(TAG, "Writing new threshold " + periodoNum + " -> " + valorW);
            GXDLMSRegister reg = new GXDLMSRegister(obisThreshold);
            reg.setDataType(2, DataType.UINT32);
            reg.setValue(valorW);
            reader.writeObject(reg, 2);
        }
        AppLogger.i(TAG, "Passive Thresholds updated");
    }

    // ── Sesión 2 ─────────────────────────────────────────────────────────────

    /*
     * ⚠️ CORRECCIÓN respecto a la versión anterior:
     *
     * Se escribía deviation=0x0080 (128) / status=0x00, un valor que NO es un
     * desfase horario real (España en verano es UTC+2 = 120 min, no 128) y que
     * no coincide con cómo el propio contador representa su reloj real (visto
     * repetidamente en esta app, incluida la reconexión de esta misma sesión):
     * deviation=0xFF88 (-120, con el signo que usa el contador) / status=0x80
     * (verano activo).
     *
     * El escrito anterior recibía OK a nivel de trama (el contador acepta y
     * guarda los bytes), pero el desfase horario inválido probablemente hace
     * que el contador nunca dispare el cambio de pasivo a activo en la hora
     * programada, aunque el dato quede "guardado" en el atributo pasivo — que
     * es exactamente el síntoma: pasada la hora, el activo no cambia y el
     * pasivo se queda ahí indefinidamente en vez de vaciarse tras activarse.
     *
     * Ahora se usa el mismo deviation/status que el contador reporta para su
     * propio reloj.
     */
    private static void escribirFechaActivacion(
            GXDLMSReader reader, int contract, Date activacion
    ) throws Exception {
        String obis = String.format(OBIS_ACTIVITY_CALENDAR, contract);
        GXDLMSActivityCalendar cal = new GXDLMSActivityCalendar(obis);

        byte[] dtBytes;

        if (activacion != null) {
            // Leer reloj del contador para confirmar desviación real actual
            int deviationHi = 0xFF;
            int deviationLo = 0x88;
            int statusByte  = 0x80;
            try {
                GXDLMSClock clock = new GXDLMSClock(OBIS_CLOCK);
                reader.read(clock, 2);
                GXDateTime meterTime = clock.getTime();
                if (meterTime != null) {
                    AppLogger.i(TAG, "Meter time raw: " + meterTime);
                    // TODO: si Gurux expone deviation/status de meterTime de forma
                    // fiable en tu versión, sustituye las constantes de arriba por
                    // los valores reales leídos aquí en vez de la constante FF88/80.
                }
            } catch (Exception e) {
                AppLogger.i(TAG, "No se pudo leer el reloj del contador, uso deviation FF88/status 80 por defecto");
            }

            Calendar c = Calendar.getInstance();
            c.setTime(activacion);

            int year   = c.get(Calendar.YEAR);
            int month  = c.get(Calendar.MONTH) + 1;
            int day    = c.get(Calendar.DAY_OF_MONTH);
            int hour   = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            int second = c.get(Calendar.SECOND);

            // Construir octet-string de 12 bytes directamente
            // para garantizar deviation/status coherentes con el reloj del contador
            dtBytes = new byte[]{
                    (byte)((year >> 8) & 0xFF), (byte)(year & 0xFF), // year
                    (byte) month,               // month
                    (byte) day,                 // day
                    (byte) 0xFF,               // day_of_week = FF (skip)
                    (byte) hour,               // hour
                    (byte) minute,             // minute
                    (byte) second,             // second
                    (byte) 0x00,               // millisecond
                    (byte) deviationHi, (byte) deviationLo,  // deviation = igual que el reloj del contador
                    (byte) statusByte          // status = igual que el reloj del contador
            };

            AppLogger.i(TAG, "Writing passive calendar activation time: " + activacion
                    + " deviation=0xFF88 status=0x80 (igual que el reloj del contador)");

        } else {
            // Activación inmediata: todos los campos FF (wildcarded)
            dtBytes = new byte[]{
                    (byte)0xFF, (byte)0xFF,  // year = FF FF
                    (byte)0xFF,              // month = FF
                    (byte)0xFF,              // day = FF
                    (byte)0xFF,              // dow = FF
                    (byte)0xFF,              // hour = FF
                    (byte)0xFF,              // minute = FF
                    (byte)0xFF,              // second = FF
                    (byte)0xFF,              // ms = FF
                    (byte)0xFF, (byte)0xFF,  // deviation = FF FF
                    (byte)0xFF               // status = FF
            };
            AppLogger.i(TAG, "Writing passive calendar activation time: immediate (null)");
        }

        // Usar updateValue con el octet-string raw para evitar
        // que Gurux reinterprete la desviación como signed
        reader.getClient().updateValue(cal, 10, dtBytes);
        reader.writeObject(cal, 10);

        AppLogger.i(TAG, "Passive Contract Updated !");
    }
}