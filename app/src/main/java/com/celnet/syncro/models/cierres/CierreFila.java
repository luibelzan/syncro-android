package com.celnet.syncro.models.cierres;

import android.os.Parcel;
import android.os.Parcelable;

public class CierreFila implements Parcelable {

    public String fecha;
    public int contrato;
    public int periodo;
    public String activeImport;
    public String activeExport;
    public String r1;
    public String r2;
    public String r3;
    public String r4;

    public CierreFila(String fecha, int contrato, int periodo,
                      String activaImport, String activaExport,
                      String r1, String r2, String r3, String r4) {

        this.fecha = fecha;
        this.contrato = contrato;
        this.periodo = periodo;
        this.activeImport = activaImport;
        this.activeExport = activaExport;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
    }

    protected CierreFila(Parcel in) {
        fecha = in.readString();
        periodo = in.readInt();
        contrato = in.readInt();
        activeImport = in.readString();
        activeExport = in.readString();
        r1 = in.readString();
        r2 = in.readString();
        r3 = in.readString();
        r4 = in.readString();
    }

    public static final Creator<CierreFila> CREATOR = new Creator<CierreFila>() {
        @Override
        public CierreFila createFromParcel(Parcel in) {
            return new CierreFila(in);
        }

        @Override
        public CierreFila[] newArray(int size) {
            return new CierreFila[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fecha);
        dest.writeInt(periodo);
        dest.writeInt(contrato);
        dest.writeString(activeImport);
        dest.writeString(activeExport);
        dest.writeString(r1);
        dest.writeString(r2);
        dest.writeString(r3);
        dest.writeString(r4);
    }

}
