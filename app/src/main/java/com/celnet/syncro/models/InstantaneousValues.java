package com.celnet.syncro.models;

import android.os.Parcel;
import android.os.Parcelable;

import android.os.Parcel;
import android.os.Parcelable;

public class InstantaneousValues implements Parcelable {

    public String serial;
    public String fechaHora;

    public String deciVolts;
    public String volRelation;

    public String vFase1;
    public String vFase2;
    public String vFase3;

    public String deciAmps;
    public String aRelation;

    public String aFase1;
    public String aFase2;
    public String aFase3;

    public String pActivaPlus;
    public String pActivaMinus;
    public String pReactivaPlus;
    public String pReactivaMinus;

    public String factorPotencia;

    // 🔹 Constructor completo
    public InstantaneousValues(String serial, String fechaHora,
                               String deciVolts, String volRelation,
                               String vFase1, String vFase2, String vFase3,
                               String deciAmps, String aRelation,
                               String aFase1, String aFase2, String aFase3,
                               String pActivaPlus, String pActivaMinus,
                               String pReactivaPlus, String pReactivaMinus,
                               String factorPotencia) {

        this.serial = serial;
        this.fechaHora = fechaHora;

        this.deciVolts = deciVolts;
        this.volRelation = volRelation;

        this.vFase1 = vFase1;
        this.vFase2 = vFase2;
        this.vFase3 = vFase3;

        this.deciAmps = deciAmps;
        this.aRelation = aRelation;

        this.aFase1 = aFase1;
        this.aFase2 = aFase2;
        this.aFase3 = aFase3;

        this.pActivaPlus = pActivaPlus;
        this.pActivaMinus = pActivaMinus;
        this.pReactivaPlus = pReactivaPlus;
        this.pReactivaMinus = pReactivaMinus;

        this.factorPotencia = factorPotencia;
    }

    // 🔹 Constructor vacío
    public InstantaneousValues() {
        this.serial = null;
        this.fechaHora = null;

        this.deciVolts = null;
        this.volRelation = null;

        this.vFase1 = null;
        this.vFase2 = null;
        this.vFase3 = null;

        this.deciAmps = null;
        this.aRelation = null;

        this.aFase1 = null;
        this.aFase2 = null;
        this.aFase3 = null;

        this.pActivaPlus = null;
        this.pActivaMinus = null;
        this.pReactivaPlus = null;
        this.pReactivaMinus = null;

        this.factorPotencia = null;
    }

    // 🔹 Constructor desde Parcel
    protected InstantaneousValues(Parcel in) {
        serial = in.readString();
        fechaHora = in.readString();

        deciVolts = in.readString();
        volRelation = in.readString();

        vFase1 = in.readString();
        vFase2 = in.readString();
        vFase3 = in.readString();

        deciAmps = in.readString();
        aRelation = in.readString();

        aFase1 = in.readString();
        aFase2 = in.readString();
        aFase3 = in.readString();

        pActivaPlus = in.readString();
        pActivaMinus = in.readString();
        pReactivaPlus = in.readString();
        pReactivaMinus = in.readString();

        factorPotencia = in.readString();
    }

    // 🔹 Creator
    public static final Creator<InstantaneousValues> CREATOR = new Creator<InstantaneousValues>() {
        @Override
        public InstantaneousValues createFromParcel(Parcel in) {
            return new InstantaneousValues(in);
        }

        @Override
        public InstantaneousValues[] newArray(int size) {
            return new InstantaneousValues[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serial);
        dest.writeString(fechaHora);

        dest.writeString(deciVolts);
        dest.writeString(volRelation);

        dest.writeString(vFase1);
        dest.writeString(vFase2);
        dest.writeString(vFase3);

        dest.writeString(deciAmps);
        dest.writeString(aRelation);

        dest.writeString(aFase1);
        dest.writeString(aFase2);
        dest.writeString(aFase3);

        dest.writeString(pActivaPlus);
        dest.writeString(pActivaMinus);
        dest.writeString(pReactivaPlus);
        dest.writeString(pReactivaMinus);

        dest.writeString(factorPotencia);
    }
}
