package com.celnet.syncro.models.events;

import android.os.Parcel;
import android.os.Parcelable;

public class EventFila implements Parcelable {

    public String fh;
    public Integer id;
    public String description;
    public Integer cod;

    // Datos adicionales opcionales (D1/D2 en el XML S09), presentes solo en
    // algunos logs (p.ej. Finished Power Quality Event Log). Null si el
    // evento no lleva datos adicionales.
    public String d1;
    public String d2;

    public EventFila(String fh, Integer id, String description, Integer cod) {
        this(fh, id, description, cod, null, null);
    }

    public EventFila(String fh, Integer id, String description, Integer cod, String d1, String d2) {
        this.fh = fh;
        this.id = id;
        this.description = description;
        this.cod = cod;
        this.d1 = d1;
        this.d2 = d2;
    }

    protected EventFila(Parcel in) {
        fh = in.readString();
        id = in.readInt();
        description = in.readString();
        cod = in.readInt();
        d1 = in.readString();
        d2 = in.readString();
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
        dest.writeString(d1);
        dest.writeString(d2);
    }
}