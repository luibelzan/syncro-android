package com.example.syncro.models;

import android.os.Parcel;
import android.os.Parcelable;

public class CurvaFila implements Parcelable {

    public String fechaHora;
    public String energiaActiva;
    public String energiaReactiva;
    public String potenciaActiva;
    public String potenciaReactiva;

    public CurvaFila(String fechaHora, String energiaActiva, String energiaReactiva,
                     String potenciaActiva, String potenciaReactiva) {
        this.fechaHora = fechaHora;
        this.energiaActiva = energiaActiva;
        this.energiaReactiva = energiaReactiva;
        this.potenciaActiva = potenciaActiva;
        this.potenciaReactiva = potenciaReactiva;
    }

    // Constructor para crear desde Parcel
    protected CurvaFila(Parcel in) {
        fechaHora = in.readString();
        energiaActiva = in.readString();
        energiaReactiva = in.readString();
        potenciaActiva = in.readString();
        potenciaReactiva = in.readString();
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
        dest.writeString(energiaActiva);
        dest.writeString(energiaReactiva);
        dest.writeString(potenciaActiva);
        dest.writeString(potenciaReactiva);
    }
}
