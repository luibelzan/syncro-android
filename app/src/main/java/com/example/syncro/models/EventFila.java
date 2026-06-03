package com.example.syncro.models;

import android.os.Parcel;
import android.os.Parcelable;

public class EventFila implements Parcelable {

    public String fh;

    public Integer id;

    public String description;

    public Integer cod;

    public EventFila(String fh, Integer id, String description, Integer cod) {
        this.fh = fh;
        this.id = id;
        this.description = description;
        this.cod = cod;
    }

    protected EventFila(Parcel in) {
        fh = in.readString();
        id = in.readInt();
        description = in.readString();
        cod = in.readInt();
    }

    public static final Creator<EventFila> CREATOR = new Creator<EventFila>() {
        @Override
        public EventFila createFromParcel(Parcel in) {
            return new EventFila(in);
        }

        @Override
        public EventFila[] newArray(int size) {
            return new EventFila[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fh);
        dest.writeInt(id);
        dest.writeString(description);
        dest.writeInt(cod);
    }
}
