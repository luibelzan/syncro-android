package com.example.syncro.models;

import android.os.Parcel;
import android.os.Parcelable;

public class MeterInfo implements Parcelable{

    public String serial;

    public String equipo;

    public String tipo;

    public String firmware;

    public MeterInfo(String serial, String equipo, String tipo, String firmware) {
        this.serial = serial;
        this.equipo = equipo;
        this.tipo = tipo;
        this.firmware = firmware;
    }

    public MeterInfo(Parcel in) {
        serial = in.readString();
        equipo = in.readString();
        tipo = in.readString();
        firmware = in.readString();
    }

    public MeterInfo() {
        this.serial = null;
        this.equipo = null;
        this.tipo = null;
        this.firmware = null;
    }

    // Parcelable.Creator necesario
    public static final Parcelable.Creator<MeterInfo> CREATOR = new Parcelable.Creator<MeterInfo>() {
        @Override
        public MeterInfo createFromParcel(Parcel in) {
            return new MeterInfo(in);
        }

        @Override
        public MeterInfo[] newArray(int size) {
            return new MeterInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // Normalmente 0
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serial);
        dest.writeString(equipo);
        dest.writeString(tipo);
        dest.writeString(firmware);
    }
}
