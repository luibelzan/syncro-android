package com.celnet.syncro.models.iec870.params;

/** Información de configuración del equipo (ASDU 141 / 142). */
public class Iec870DeviceInfo {

    public int codigoFabricante;
    public String modelo = "";
    public int firmware;
    public String serie = "";
    /** Octeto 8: mes (bits 0-3) y año-2000 (bits 4-7) de la versión del protocolo, "MM/yyyy". */
    public String fechaProtocolo = "";
    /** TimeA de los octetos 9-13, "dd/MM/yyyy" (significado según la librería: fecha IEC). */
    public String fechaVersion = "";
    public int bateria;

    public final Iec870PortConfig puerto1 = new Iec870PortConfig();
    public final Iec870PortConfig puerto2 = new Iec870PortConfig();
    public final Iec870PortConfig puertoOptico = new Iec870PortConfig();

    public final Iec870TrafoRatio trafo = new Iec870TrafoRatio();
    public final int[] periodoIntegracion = new int[3];
    public final boolean[] contratoActivo = new boolean[3];
}