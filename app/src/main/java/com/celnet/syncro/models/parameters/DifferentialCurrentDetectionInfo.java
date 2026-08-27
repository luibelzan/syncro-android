package com.celnet.syncro.models.parameters;

import android.os.Parcel;
import android.os.Parcelable;

public class DifferentialCurrentDetectionInfo implements Parcelable {

    // % de variación entre I fase e I neutro que dispara la detección
    private double umbralVariacionPorcentaje;

    // Segundos que debe mantenerse la variación para confirmar la detección
    private int umbralTiempoSegundos;

    // Valor mínimo de corriente de fase (A) a superar para considerar la detección
    private double umbralCorrienteMinimaAmperios;

    // Corriente diferencial medida actualmente (solo lectura)
    private double corrienteDiferencialActualAmperios;

    public DifferentialCurrentDetectionInfo() {
    }

    protected DifferentialCurrentDetectionInfo(Parcel in) {
        umbralVariacionPorcentaje = in.readDouble();
        umbralTiempoSegundos = in.readInt();
        umbralCorrienteMinimaAmperios = in.readDouble();
        corrienteDiferencialActualAmperios = in.readDouble();
    }

    public static final Creator<DifferentialCurrentDetectionInfo> CREATOR =
            new Creator<DifferentialCurrentDetectionInfo>() {
                @Override
                public DifferentialCurrentDetectionInfo createFromParcel(Parcel in) {
                    return new DifferentialCurrentDetectionInfo(in);
                }

                @Override
                public DifferentialCurrentDetectionInfo[] newArray(int size) {
                    return new DifferentialCurrentDetectionInfo[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(umbralVariacionPorcentaje);
        dest.writeInt(umbralTiempoSegundos);
        dest.writeDouble(umbralCorrienteMinimaAmperios);
        dest.writeDouble(corrienteDiferencialActualAmperios);
    }

    public double getUmbralVariacionPorcentaje() { return umbralVariacionPorcentaje; }
    public void setUmbralVariacionPorcentaje(double v) { this.umbralVariacionPorcentaje = v; }

    public int getUmbralTiempoSegundos() { return umbralTiempoSegundos; }
    public void setUmbralTiempoSegundos(int v) { this.umbralTiempoSegundos = v; }

    public double getUmbralCorrienteMinimaAmperios() { return umbralCorrienteMinimaAmperios; }
    public void setUmbralCorrienteMinimaAmperios(double v) { this.umbralCorrienteMinimaAmperios = v; }

    public double getCorrienteDiferencialActualAmperios() { return corrienteDiferencialActualAmperios; }
    public void setCorrienteDiferencialActualAmperios(double v) { this.corrienteDiferencialActualAmperios = v; }

    @Override
    public String toString() {
        return "------------------------------\n" +
                "Umbral variación corriente neutro : " + String.format("%.2f", umbralVariacionPorcentaje) + " %\n" +
                "Umbral tiempo detección           : " + umbralTiempoSegundos + " s\n" +
                "Umbral corriente mínima            : " + String.format("%.1f", umbralCorrienteMinimaAmperios) + " A\n" +
                "Corriente diferencial actual       : " + String.format("%.1f", corrienteDiferencialActualAmperios) + " A";
    }
}