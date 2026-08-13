package com.celnet.syncro.models.curvas;

import android.os.Parcel;
import android.os.Parcelable;

public class CurvaFila implements Parcelable {

    public String fechaHora;
    public String bc;
    public String ai;
    public String ae;
    public String r1;
    public String r2;
    public String r3;
    public String r4;

    public CurvaFila(String fechaHora, String bc, String ai,
                     String ae, String r1, String r2, String r3, String r4) {
        this.fechaHora = fechaHora;
        this.bc = bc;
        this.ai = ai;
        this.ae = ae;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
    }

    // Constructor para crear desde Parcel
    protected CurvaFila(Parcel in) {
        fechaHora = in.readString();
        bc = in.readString();
        ai = in.readString();
        ae = in.readString();
        r1 = in.readString();
        r2 = in.readString();
        r3 = in.readString();
        r4 = in.readString();
    }

    // Parcelable.Creator necesario
    public static final Creator<CurvaFila> CREATOR = new Creator<CurvaFila>() {
        @Override
        public CurvaFila createFromParcel(Parcel in) {
            return new CurvaFila(in);
        }

        @Override
        public CurvaFila[] newArray(int size) {
            return new CurvaFila[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // Normalmente 0
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fechaHora);
        dest.writeString(bc);
        dest.writeString(ai);
        dest.writeString(ae);
        dest.writeString(r1);
        dest.writeString(r2);
        dest.writeString(r3);
        dest.writeString(r4);
    }
}
