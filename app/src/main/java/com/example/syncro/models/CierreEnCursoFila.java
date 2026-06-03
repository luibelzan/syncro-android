package com.example.syncro.models;

import android.os.Parcel;
import android.os.Parcelable;

public class CierreEnCursoFila implements Parcelable {

    public String fecha;
    public int    contrato;

    // Índices 0-5 = periodos 1-6, índice 6 = Total
    public long[] aPlus     = new long[7];  // Activa Importada [kWh]
    public long[] aMinus    = new long[7];  // Activa Exportada [kWh]
    public long[] qi        = new long[7];  // Reactiva QI [kvarh]
    public long[] qii       = new long[7];  // Reactiva QII [kvarh]
    public long[] qiii      = new long[7];  // Reactiva QIII [kvarh]
    public long[] qiv       = new long[7];  // Reactiva QIV [kvarh]
    public long[] maxDemand = new long[7];  // Maxímetro [W]
    public String[] maxDates = new String[7]; // Fecha/hora maxímetro

    public CierreEnCursoFila(String fecha, int contrato,
                             long[] aPlus, long[] aMinus,
                             long[] qi, long[] qii, long[] qiii, long[] qiv,
                             long[] maxDemand, String[] maxDates) {
        this.fecha      = fecha;
        this.contrato   = contrato;
        this.aPlus      = aPlus;
        this.aMinus     = aMinus;
        this.qi         = qi;
        this.qii        = qii;
        this.qiii       = qiii;
        this.qiv        = qiv;
        this.maxDemand  = maxDemand;
        this.maxDates   = maxDates;
    }

    // ── Parcelable ───────────────────────────────────────────────────────────

    protected CierreEnCursoFila(Parcel in) {
        fecha    = in.readString();
        contrato = in.readInt();
        in.readLongArray(aPlus     = new long[7]);
        in.readLongArray(aMinus    = new long[7]);
        in.readLongArray(qi        = new long[7]);
        in.readLongArray(qii       = new long[7]);
        in.readLongArray(qiii      = new long[7]);
        in.readLongArray(qiv       = new long[7]);
        in.readLongArray(maxDemand = new long[7]);
        maxDates = in.createStringArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fecha);
        dest.writeInt(contrato);
        dest.writeLongArray(aPlus);
        dest.writeLongArray(aMinus);
        dest.writeLongArray(qi);
        dest.writeLongArray(qii);
        dest.writeLongArray(qiii);
        dest.writeLongArray(qiv);
        dest.writeLongArray(maxDemand);
        dest.writeStringArray(maxDates);
    }

    @Override public int describeContents() { return 0; }

    public static final Creator<CierreEnCursoFila> CREATOR = new Creator<>() {
        @Override public CierreEnCursoFila createFromParcel(Parcel in) { return new CierreEnCursoFila(in); }
        @Override public CierreEnCursoFila[] newArray(int size)        { return new CierreEnCursoFila[size]; }
    };
}
