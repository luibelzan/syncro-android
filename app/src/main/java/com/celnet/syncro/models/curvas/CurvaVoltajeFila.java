package com.celnet.syncro.models.curvas;

import android.os.Parcel;
import android.os.Parcelable;

public class CurvaVoltajeFila implements Parcelable {
    public String fechaHora;
    public String status;
    public String maxL1, maxL2, maxL3;
    public String avL1, avL2, avL3;
    public String minL1, minL2, minL3;

    public CurvaVoltajeFila(String fechaHora, String status,
                            String maxL1, String maxL2, String maxL3,
                            String avL1, String avL2, String avL3,
                            String minL1, String minL2, String minL3) {
        this.fechaHora = fechaHora;
        this.status = status;
        this.maxL1 = maxL1; this.maxL2 = maxL2; this.maxL3 = maxL3;
        this.avL1 = avL1; this.avL2 = avL2; this.avL3 = avL3;
        this.minL1 = minL1; this.minL2 = minL2; this.minL3 = minL3;
    }

    protected CurvaVoltajeFila(Parcel in) {
        fechaHora = in.readString();
        status = in.readString();
        maxL1 = in.readString(); maxL2 = in.readString(); maxL3 = in.readString();
        avL1 = in.readString(); avL2 = in.readString(); avL3 = in.readString();
        minL1 = in.readString(); minL2 = in.readString(); minL3 = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fechaHora);
        dest.writeString(status);
        dest.writeString(maxL1); dest.writeString(maxL2); dest.writeString(maxL3);
        dest.writeString(avL1); dest.writeString(avL2); dest.writeString(avL3);
        dest.writeString(minL1); dest.writeString(minL2); dest.writeString(minL3);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<CurvaVoltajeFila> CREATOR = new Creator<CurvaVoltajeFila>() {
        @Override
        public CurvaVoltajeFila createFromParcel(Parcel in) { return new CurvaVoltajeFila(in); }
        @Override
        public CurvaVoltajeFila[] newArray(int size) { return new CurvaVoltajeFila[size]; }
    };
}