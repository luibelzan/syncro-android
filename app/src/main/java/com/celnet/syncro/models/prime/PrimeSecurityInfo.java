package com.celnet.syncro.models.prime;

import android.os.Parcel;
import android.os.Parcelable;

public class PrimeSecurityInfo implements Parcelable {

    private ConstellationCoding constellationCoding;
    private String sarSize;
    private boolean arqEnabled;
    private String dualStackVersion;
    private int dualStackVersionCode;

    public PrimeSecurityInfo() {
    }

    protected PrimeSecurityInfo(Parcel in) {
        boolean[] bits = in.createBooleanArray();
        constellationCoding = ConstellationCoding.fromBitArray(bits);
        sarSize = in.readString();
        arqEnabled = in.readByte() != 0;
        dualStackVersion = in.readString();
        dualStackVersionCode = in.readInt();
    }

    public static final Creator<PrimeSecurityInfo> CREATOR = new Creator<PrimeSecurityInfo>() {
        @Override
        public PrimeSecurityInfo createFromParcel(Parcel in) {
            return new PrimeSecurityInfo(in);
        }

        @Override
        public PrimeSecurityInfo[] newArray(int size) {
            return new PrimeSecurityInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Se serializa el array de bits en vez de ConstellationCoding
        // directamente, para no depender de que esa clase implemente
        // Parcelable. Se reconstruye con fromBitArray() al deserializar.
        dest.writeBooleanArray(constellationCoding.toBitArray());
        dest.writeString(sarSize);
        dest.writeByte((byte) (arqEnabled ? 1 : 0));
        dest.writeString(dualStackVersion);
        dest.writeInt(dualStackVersionCode);
    }

    public ConstellationCoding getConstellationCoding() { return constellationCoding; }
    public void setConstellationCoding(ConstellationCoding constellationCoding) { this.constellationCoding = constellationCoding; }

    public String getSarSize() { return sarSize; }
    public void setSarSize(String sarSize) { this.sarSize = sarSize; }

    public boolean isArqEnabled() { return arqEnabled; }
    public void setArqEnabled(boolean arqEnabled) { this.arqEnabled = arqEnabled; }

    public String getDualStackVersion() { return dualStackVersion; }
    public void setDualStackVersion(String dualStackVersion) { this.dualStackVersion = dualStackVersion; }

    public int getDualStackVersionCode() { return dualStackVersionCode; }
    public void setDualStackVersionCode(int dualStackVersionCode) { this.dualStackVersionCode = dualStackVersionCode; }

    @Override
    public String toString() {
        return "------------------------------\n" +
                "Prime 1.4 Constellation Coding :\n" +
                constellationCoding.toString() + "\n" +
                "Prime 1.4 SARSize : " + sarSize + "\n" +
                "Prime 1.4 ARQ " + (arqEnabled ? "Enabled" : "Disabled") + "\n" +
                "Prime 1.4 Dualstackversion : " + dualStackVersion;
    }
}