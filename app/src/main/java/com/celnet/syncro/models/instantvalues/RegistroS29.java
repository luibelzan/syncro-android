package com.celnet.syncro.models.instantvalues;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Representa una fila ya parseada y escalada del buffer S29
 * (Instantaneous Values Profile, OBIS 1-0:99.1.7.255).
 *
 * Los valores numéricos llegan ya formateados como String (mismo criterio
 * que CurvaFila) para que el Parcelable sea trivial y el adapter no tenga
 * que reformatear en cada bind.
 */
public class RegistroS29 implements Parcelable {

    public String fechaHora;

    public String voltageL1, voltageL2, voltageL3;
    public String currentL1, currentL2, currentL3;
    public String currentSum, neutralCurrent, differentialCurrent;

    public String activePowerTotal, activePowerL1, activePowerL2, activePowerL3;
    public String reactivePowerTotal, reactivePowerL1, reactivePowerL2, reactivePowerL3;
    public String powerFactorTotal, powerFactorL1, powerFactorL2, powerFactorL3;

    public String phaseSequence;
    public String angleU1, angleU2, angleU3;
    public String angleI1, angleI2, angleI3;
    public String angleIN, angleIdif;

    public RegistroS29(String fechaHora,
                       String voltageL1, String voltageL2, String voltageL3,
                       String currentL1, String currentL2, String currentL3,
                       String currentSum, String neutralCurrent, String differentialCurrent,
                       String activePowerTotal, String activePowerL1, String activePowerL2, String activePowerL3,
                       String reactivePowerTotal, String reactivePowerL1, String reactivePowerL2, String reactivePowerL3,
                       String powerFactorTotal, String powerFactorL1, String powerFactorL2, String powerFactorL3,
                       String phaseSequence,
                       String angleU1, String angleU2, String angleU3,
                       String angleI1, String angleI2, String angleI3,
                       String angleIN, String angleIdif) {
        this.fechaHora = fechaHora;
        this.voltageL1 = voltageL1;
        this.voltageL2 = voltageL2;
        this.voltageL3 = voltageL3;
        this.currentL1 = currentL1;
        this.currentL2 = currentL2;
        this.currentL3 = currentL3;
        this.currentSum = currentSum;
        this.neutralCurrent = neutralCurrent;
        this.differentialCurrent = differentialCurrent;
        this.activePowerTotal = activePowerTotal;
        this.activePowerL1 = activePowerL1;
        this.activePowerL2 = activePowerL2;
        this.activePowerL3 = activePowerL3;
        this.reactivePowerTotal = reactivePowerTotal;
        this.reactivePowerL1 = reactivePowerL1;
        this.reactivePowerL2 = reactivePowerL2;
        this.reactivePowerL3 = reactivePowerL3;
        this.powerFactorTotal = powerFactorTotal;
        this.powerFactorL1 = powerFactorL1;
        this.powerFactorL2 = powerFactorL2;
        this.powerFactorL3 = powerFactorL3;
        this.phaseSequence = phaseSequence;
        this.angleU1 = angleU1;
        this.angleU2 = angleU2;
        this.angleU3 = angleU3;
        this.angleI1 = angleI1;
        this.angleI2 = angleI2;
        this.angleI3 = angleI3;
        this.angleIN = angleIN;
        this.angleIdif = angleIdif;
    }

    // Constructor para crear desde Parcel
    protected RegistroS29(Parcel in) {
        fechaHora = in.readString();
        voltageL1 = in.readString();
        voltageL2 = in.readString();
        voltageL3 = in.readString();
        currentL1 = in.readString();
        currentL2 = in.readString();
        currentL3 = in.readString();
        currentSum = in.readString();
        neutralCurrent = in.readString();
        differentialCurrent = in.readString();
        activePowerTotal = in.readString();
        activePowerL1 = in.readString();
        activePowerL2 = in.readString();
        activePowerL3 = in.readString();
        reactivePowerTotal = in.readString();
        reactivePowerL1 = in.readString();
        reactivePowerL2 = in.readString();
        reactivePowerL3 = in.readString();
        powerFactorTotal = in.readString();
        powerFactorL1 = in.readString();
        powerFactorL2 = in.readString();
        powerFactorL3 = in.readString();
        phaseSequence = in.readString();
        angleU1 = in.readString();
        angleU2 = in.readString();
        angleU3 = in.readString();
        angleI1 = in.readString();
        angleI2 = in.readString();
        angleI3 = in.readString();
        angleIN = in.readString();
        angleIdif = in.readString();
    }

    // Parcelable.Creator necesario
    public static final Creator<RegistroS29> CREATOR = new Creator<RegistroS29>() {
        @Override
        public RegistroS29 createFromParcel(Parcel in) {
            return new RegistroS29(in);
        }

        @Override
        public RegistroS29[] newArray(int size) {
            return new RegistroS29[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // Normalmente 0
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fechaHora);
        dest.writeString(voltageL1);
        dest.writeString(voltageL2);
        dest.writeString(voltageL3);
        dest.writeString(currentL1);
        dest.writeString(currentL2);
        dest.writeString(currentL3);
        dest.writeString(currentSum);
        dest.writeString(neutralCurrent);
        dest.writeString(differentialCurrent);
        dest.writeString(activePowerTotal);
        dest.writeString(activePowerL1);
        dest.writeString(activePowerL2);
        dest.writeString(activePowerL3);
        dest.writeString(reactivePowerTotal);
        dest.writeString(reactivePowerL1);
        dest.writeString(reactivePowerL2);
        dest.writeString(reactivePowerL3);
        dest.writeString(powerFactorTotal);
        dest.writeString(powerFactorL1);
        dest.writeString(powerFactorL2);
        dest.writeString(powerFactorL3);
        dest.writeString(phaseSequence);
        dest.writeString(angleU1);
        dest.writeString(angleU2);
        dest.writeString(angleU3);
        dest.writeString(angleI1);
        dest.writeString(angleI2);
        dest.writeString(angleI3);
        dest.writeString(angleIN);
        dest.writeString(angleIdif);
    }

    /**
     * Cabeceras de columna, en el mismo orden exacto que {@link #toRowValues()}.
     * Usar SIEMPRE ambas listas juntas para que las columnas se alineen.
     */
    public static final String[] CABECERAS = {
            "Fecha",
            "Instantaneous Voltage Phase 1", "Instantaneous Voltage Phase 2", "Instantaneous Voltage Phase 3",
            "Instantaneous Current Phase 1", "Instantaneous Current Phase 2", "Instantaneous Current Phase 3",
            "Current Sum three phases", "Neutral current", "Differential current",
            "Active Power (total)", "Active Power Phase 1", "Active Power Phase 2", "Active Power Phase 3",
            "Reactive Power (total)", "Reactive Power Phase 1", "Reactive Power Phase 2", "Reactive Power Phase 3",
            "Power Factor (total)", "Power Factor Phase 1", "Power Factor Phase 2", "Power Factor Phase 3",
            "Phase sequence",
            "Angle U1", "Angle U2", "Angle U3",
            "Angle I1", "Angle I2", "Angle I3",
            "Angle IN", "Angle Idif"
    };

    /** Todas las columnas ya formateadas, en el mismo orden que {@link #CABECERAS}. */
    public String[] toRowValues() {
        return new String[]{
                fechaHora,
                voltageL1, voltageL2, voltageL3,
                currentL1, currentL2, currentL3,
                currentSum, neutralCurrent, differentialCurrent,
                activePowerTotal, activePowerL1, activePowerL2, activePowerL3,
                reactivePowerTotal, reactivePowerL1, reactivePowerL2, reactivePowerL3,
                powerFactorTotal, powerFactorL1, powerFactorL2, powerFactorL3,
                phaseSequence,
                angleU1, angleU2, angleU3,
                angleI1, angleI2, angleI3,
                angleIN, angleIdif
        };
    }
}