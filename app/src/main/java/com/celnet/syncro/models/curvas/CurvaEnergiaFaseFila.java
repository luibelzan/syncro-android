package com.celnet.syncro.models.curvas;

import android.os.Parcel;
import android.os.Parcelable;

public class CurvaEnergiaFaseFila implements Parcelable {
    public String fechaHora;
    public String status;
    public String eaPosR, eaNegR, q1R, q2R, q3R, q4R;
    public String eaPosS, eaNegS, q1S, q2S, q3S, q4S;
    public String eaPosT, eaNegT, q1T, q2T, q3T, q4T;

    public CurvaEnergiaFaseFila(String fechaHora, String status,
                                String eaPosR, String eaNegR, String q1R, String q2R, String q3R, String q4R,
                                String eaPosS, String eaNegS, String q1S, String q2S, String q3S, String q4S,
                                String eaPosT, String eaNegT, String q1T, String q2T, String q3T, String q4T) {
        this.fechaHora = fechaHora;
        this.status = status;
        this.eaPosR = eaPosR; this.eaNegR = eaNegR; this.q1R = q1R; this.q2R = q2R; this.q3R = q3R; this.q4R = q4R;
        this.eaPosS = eaPosS; this.eaNegS = eaNegS; this.q1S = q1S; this.q2S = q2S; this.q3S = q3S; this.q4S = q4S;
        this.eaPosT = eaPosT; this.eaNegT = eaNegT; this.q1T = q1T; this.q2T = q2T; this.q3T = q3T; this.q4T = q4T;
    }

    protected CurvaEnergiaFaseFila(Parcel in) {
        fechaHora = in.readString();
        status = in.readString();
        eaPosR = in.readString(); eaNegR = in.readString(); q1R = in.readString(); q2R = in.readString(); q3R = in.readString(); q4R = in.readString();
        eaPosS = in.readString(); eaNegS = in.readString(); q1S = in.readString(); q2S = in.readString(); q3S = in.readString(); q4S = in.readString();
        eaPosT = in.readString(); eaNegT = in.readString(); q1T = in.readString(); q2T = in.readString(); q3T = in.readString(); q4T = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fechaHora);
        dest.writeString(status);
        dest.writeString(eaPosR); dest.writeString(eaNegR); dest.writeString(q1R); dest.writeString(q2R); dest.writeString(q3R); dest.writeString(q4R);
        dest.writeString(eaPosS); dest.writeString(eaNegS); dest.writeString(q1S); dest.writeString(q2S); dest.writeString(q3S); dest.writeString(q4S);
        dest.writeString(eaPosT); dest.writeString(eaNegT); dest.writeString(q1T); dest.writeString(q2T); dest.writeString(q3T); dest.writeString(q4T);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<CurvaEnergiaFaseFila> CREATOR = new Creator<CurvaEnergiaFaseFila>() {
        @Override
        public CurvaEnergiaFaseFila createFromParcel(Parcel in) { return new CurvaEnergiaFaseFila(in); }
        @Override
        public CurvaEnergiaFaseFila[] newArray(int size) { return new CurvaEnergiaFaseFila[size]; }
    };
}