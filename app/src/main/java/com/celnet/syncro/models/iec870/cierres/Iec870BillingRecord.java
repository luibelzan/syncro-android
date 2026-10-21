package com.celnet.syncro.models.iec870.cierres;

import java.io.Serializable;

/**
 * Registro de tarificación (valores memorizados, ASDU 136) de una tarifa de un
 * contrato. Las energías son lecturas absolutas (kWh / kVArh) e incrementos del
 * periodo; cada valor lleva su byte de calidad tal como llega del contador.
 */
public class Iec870BillingRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public int contrato;     // 1..3
    public int tarifa;       // 0 = total, 1..6

    public long activaAbs;
    public long activaInc;
    public int activaCal;

    public long reactivaIndAbs;
    public long reactivaIndInc;
    public int reactivaIndCal;

    public long reactivaCapAbs;
    public long reactivaCapInc;
    public int reactivaCapCal;

    public long reservado7;
    public int reservado7Cal;
    public long reservado8;
    public int reservado8Cal;

    public long maximetro;
    /** "yyyy/MM/dd HH:mm:00" + letra S/W (SU=0 -> S, SU=1 -> W, como el software funcional). */
    public String maximetroFecha = "";
    public int maximetroCal;

    public long exceso;
    public int excesoCal;

    /** Inicio y fin del periodo de tarificación, mismo formato que maximetroFecha. */
    public String inicio = "";
    public String fin = "";
}