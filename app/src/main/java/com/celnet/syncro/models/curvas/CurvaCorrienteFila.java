package com.celnet.syncro.models.curvas;

import android.os.Parcel;
import android.os.Parcelable;

public class CurvaCorrienteFila implements Parcelable {
    public String fechaHora;
    public String status;
    public String maxL1, maxL2, maxL3, maxN;
    public String avL1, avL2, avL3, avN;
    public String minL1, minL2, minL3, minN;

    public CurvaCorrienteFila(String fechaHora, String status,
                              String maxL1, String maxL2, String maxL3, String maxN,
                              String avL1, String avL2, String avL3, String avN,
                              String minL1, String minL2, String minL3, String minN) {
        this.fechaHora = fechaHora;
        this.status = status;
        this.maxL1 = maxL1; this.maxL2 = maxL2; this.maxL3 = maxL3; this.maxN = maxN;
        this.avL1 = avL1; this.avL2 = avL2; this.avL3 = avL3; this.avN = avN;
        this.minL1 = minL1; this.minL2 = minL2; this.minL3 = minL3; this.minN = minN;
    }

    protected CurvaCorrienteFila(Parcel in) {
        fechaHora = in.readString();
        status = in.readString();
        maxL1 = in.readString(); maxL2 = in.readString(); maxL3 = in.readString(); maxN = in.readString();
        avL1 = in.readString(); avL2 = in.readString(); avL3 = in.readString(); avN = in.readString();
        minL1 = in.readString(); minL2 = in.readString(); minL3 = in.readString(); minN = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fechaHora);
        dest.writeString(status);
        dest.writeString(maxL1); dest.writeString(maxL2); dest.writeString(maxL3); dest.writeString(maxN);
        dest.writeString(avL1); dest.writeString(avL2); dest.writeString(avL3); dest.writeString(avN);
        dest.writeString(minL1); dest.writeString(minL2); dest.writeString(minL3); dest.writeString(minN);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<CurvaCorrienteFila> CREATOR = new Creator<CurvaCorrienteFila>() {
        @Override
        public CurvaCorrienteFila createFromParcel(Parcel in) { return new CurvaCorrienteFila(in); }
        @Override
        public CurvaCorrienteFila[] newArray(int size) { return new CurvaCorrienteFila[size]; }
    };
}