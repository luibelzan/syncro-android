package com.example.syncro.models;

import android.os.Parcel;
import android.os.Parcelable;

public class CierreMensualFila implements Parcelable {

    public String fechaInicio;
    public String fechaFin;
    public int contrato;
    public int periodo;
    public String maxAIi;
    public String fechaMaxAIi;
    public String activeImportAbs;
    public String activeExportAbs;
    public String r1;
    public String r2;
    public String r3;
    public String r4;
    public String activeImportInc;
    public String activeExportInc;
    public String r1Inc;
    public String r2Inc;
    public String r3Inc;
    public String r4Inc;

    public CierreMensualFila(String fechaInicio, String fechaFin, int contrato, int periodo, String maxAIi, String fechaMaxAIi,
                             String activeImportAbs, String activeExportAbs, String r1, String r2, String r3, String r4,
                             String activeImportInc, String activeExportInc, String r1Inc, String r2Inc, String r3Inc, String r4Inc) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.contrato = contrato;
        this.periodo = periodo;
        this.maxAIi = maxAIi;
        this.fechaMaxAIi = fechaMaxAIi;
        this.activeImportAbs = activeImportAbs;
        this.activeExportAbs = activeExportAbs;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
        this.activeImportInc = activeImportInc;
        this.activeExportInc = activeExportInc;
        this.r1Inc = r1Inc;
        this.r2Inc = r2Inc;
        this.r3Inc = r3Inc;
        this.r4Inc = r4Inc;
    }

    protected CierreMensualFila(Parcel in) {
        fechaInicio = in.readString();
        fechaFin = in.readString();
        contrato = in.readInt();     // ✅ mejor
        periodo = in.readInt();      // ✅ mejor
        maxAIi = in.readString();
        fechaMaxAIi = in.readString();
        activeImportAbs = in.readString();
        activeExportAbs = in.readString();
        r1 = in.readString();
        r2 = in.readString();
        r3 = in.readString();
        r4 = in.readString();
        activeImportInc = in.readString();
        activeExportInc = in.readString();
        r1Inc = in.readString();
        r2Inc = in.readString();
        r3Inc = in.readString();
        r4Inc = in.readString();
    }

    public static final Creator<CierreMensualFila> CREATOR = new Creator<CierreMensualFila>() {
        @Override
        public CierreMensualFila createFromParcel(Parcel source) {
            return new CierreMensualFila(source);
        }

        @Override
        public CierreMensualFila[] newArray(int size) {
            return new CierreMensualFila[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fechaInicio);
        dest.writeString(fechaFin);
        dest.writeInt(contrato);     // ✅ importante
        dest.writeInt(periodo);      // ✅ importante
        dest.writeString(maxAIi);
        dest.writeString(fechaMaxAIi);
        dest.writeString(activeImportAbs);
        dest.writeString(activeExportAbs);
        dest.writeString(r1);
        dest.writeString(r2);
        dest.writeString(r3);
        dest.writeString(r4);
        dest.writeString(activeImportInc);
        dest.writeString(activeExportInc);
        dest.writeString(r1Inc);
        dest.writeString(r2Inc);
        dest.writeString(r3Inc);
        dest.writeString(r4Inc);
    }
}
