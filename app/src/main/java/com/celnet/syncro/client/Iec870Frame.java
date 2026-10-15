package com.celnet.syncro.client;

/**
 * Trama IEC 60870-5-102 (formato FT1.2): fija (0x10 ... 0x16) o variable
 * (0x68 L L 0x68 ... CS 0x16). Incluye construcción y análisis.
 */
public class Iec870Frame {

    public static final int START_FIXED = 0x10;
    public static final int START_VARIABLE = 0x68;
    public static final int END = 0x16;

    // Funciones de enlace (campo C, 4 bits)
    public static final int CF_REPOSITION = 0;   // reposición del enlace (primario)
    public static final int CF_USER_DATA = 3;    // datos de usuario (primario)
    public static final int CF_LINK_STATE = 9;   // solicitud de estado del enlace
    public static final int CF_REQUEST_CLASS2 = 11; // solicitud de datos de clase 2

    // Causas de transmisión
    public static final int CAUSE_REQUESTED = 0x05;
    public static final int CAUSE_ACTIVATION = 0x06;
    public static final int CAUSE_ACTIVATION_CONFIRM = 0x07;
    public static final int CAUSE_ACTIVATION_END = 0x0A;
    public static final int CAUSE_TYPE_UNAVAILABLE = 0x0E;
    public static final int CAUSE_DIRECTION_UNKNOWN = 0x10;
    public static final int CAUSE_OBJECT_UNAVAILABLE = 0x11;
    public static final int CAUSE_PERIOD_UNAVAILABLE = 0x12;

    public final byte[] raw;
    public final boolean variable;

    public final int control;
    public final int linkAddress;
    public final int functionCode;

    // Solo tramas variables
    public final int type;
    public final int qualifier;     // cualificador de estructura variable (nº de objetos)
    public final boolean negative;  // bit P/N de la causa
    public final int cause;
    public final int measuringPoint;
    public final int register;
    public final byte[] data;

    private Iec870Frame(byte[] raw, boolean variable, int control, int linkAddress,
                        int type, int qualifier, boolean negative, int cause,
                        int measuringPoint, int register, byte[] data) {
        this.raw = raw;
        this.variable = variable;
        this.control = control;
        this.linkAddress = linkAddress;
        this.functionCode = control & 0x0F;
        this.type = type;
        this.qualifier = qualifier;
        this.negative = negative;
        this.cause = cause;
        this.measuringPoint = measuringPoint;
        this.register = register;
        this.data = data;
    }

    // ------------------------------------------------------------------
    // Construcción
    // ------------------------------------------------------------------

    /** Byte de control: RES(7) PRM(6) FCB(5) FCV(4) CF(3..0). Siempre PRM=1, RES=0. */
    public static int controlByte(int fcb, boolean fcv, int cf) {
        return 0x40 | ((fcb & 1) << 5) | (fcv ? 0x10 : 0) | (cf & 0x0F);
    }

    public static byte[] buildFixed(int control, int linkAddress) {
        int lo = linkAddress & 0xFF;
        int hi = (linkAddress >> 8) & 0xFF;
        return new byte[]{
                (byte) START_FIXED, (byte) control, (byte) lo, (byte) hi,
                (byte) ((control + lo + hi) & 0xFF), (byte) END
        };
    }

    public static byte[] buildVariable(int control, int linkAddress, int type, int qualifier,
                                       int cause, int measuringPoint, int register,
                                       byte[] data) {
        int len = 9 + data.length;
        byte[] f = new byte[len + 6];
        f[0] = (byte) START_VARIABLE;
        f[1] = (byte) len;
        f[2] = (byte) len;
        f[3] = (byte) START_VARIABLE;
        f[4] = (byte) control;
        f[5] = (byte) (linkAddress & 0xFF);
        f[6] = (byte) ((linkAddress >> 8) & 0xFF);
        f[7] = (byte) type;
        f[8] = (byte) qualifier;
        f[9] = (byte) cause;
        f[10] = (byte) (measuringPoint & 0xFF);
        f[11] = (byte) ((measuringPoint >> 8) & 0xFF);
        f[12] = (byte) register;
        System.arraycopy(data, 0, f, 13, data.length);
        int cs = 0;
        for (int i = 4; i < len + 4; i++) cs += f[i] & 0xFF;
        f[len + 4] = (byte) (cs & 0xFF);
        f[len + 5] = (byte) END;
        return f;
    }

    // ------------------------------------------------------------------
    // Análisis
    // ------------------------------------------------------------------

    public static Iec870Frame parseFixed(byte[] b) throws Iec870Exception {
        if (b.length != 6 || (b[0] & 0xFF) != START_FIXED || (b[5] & 0xFF) != END) {
            throw new Iec870Exception("Trama fija mal formada: " + hex(b));
        }
        int cs = ((b[1] & 0xFF) + (b[2] & 0xFF) + (b[3] & 0xFF)) & 0xFF;
        if (cs != (b[4] & 0xFF)) {
            throw new Iec870Exception("Checksum incorrecto en trama fija: " + hex(b));
        }
        int addr = (b[2] & 0xFF) | ((b[3] & 0xFF) << 8);
        return new Iec870Frame(b, false, b[1] & 0xFF, addr, 0, 0, false, 0, 0, 0, new byte[0]);
    }

    public static Iec870Frame parseVariable(byte[] b) throws Iec870Exception {
        if (b.length < 15 || (b[0] & 0xFF) != START_VARIABLE || (b[3] & 0xFF) != START_VARIABLE
                || b[1] != b[2] || (b[b.length - 1] & 0xFF) != END) {
            throw new Iec870Exception("Trama variable mal formada: " + hex(b));
        }
        int len = b[1] & 0xFF;
        if (b.length != len + 6) {
            throw new Iec870Exception("Longitud incoherente en trama variable: " + hex(b));
        }
        int cs = 0;
        for (int i = 4; i < len + 4; i++) cs += b[i] & 0xFF;
        if ((cs & 0xFF) != (b[len + 4] & 0xFF)) {
            throw new Iec870Exception("Checksum incorrecto en trama variable: " + hex(b));
        }
        int addr = (b[5] & 0xFF) | ((b[6] & 0xFF) << 8);
        int causeByte = b[9] & 0xFF;
        int pm = (b[10] & 0xFF) | ((b[11] & 0xFF) << 8);
        byte[] data = new byte[len - 9];
        System.arraycopy(b, 13, data, 0, data.length);
        return new Iec870Frame(b, true, b[4] & 0xFF, addr, b[7] & 0xFF, b[8] & 0xFF,
                (causeByte & 0x40) != 0, causeByte & 0x3F, pm, b[12] & 0xFF, data);
    }

    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X ", x & 0xFF));
        return sb.toString().trim();
    }
}