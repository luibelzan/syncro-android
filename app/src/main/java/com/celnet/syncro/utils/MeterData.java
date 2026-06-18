package com.celnet.syncro.utils;

import java.time.LocalDateTime;

public class MeterData {

    private LocalDateTime timestamp;
    private InstantaneousValues instantaneousValues;
    private TransformationRatios transformationRatios;
    private PowerValues powerValues;
    private EnergyValues energyValues;

    public MeterData() {
        this.instantaneousValues = new InstantaneousValues();
        this.transformationRatios = new TransformationRatios();
        this.powerValues = new PowerValues();
        this.energyValues = new EnergyValues();
    }

    // ================== GETTERS & SETTERS ==================

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public InstantaneousValues getInstantaneousValues() {
        return instantaneousValues;
    }

    public void setInstantaneousValues(InstantaneousValues instantaneousValues) {
        this.instantaneousValues = instantaneousValues;
    }

    public TransformationRatios getTransformationRatios() {
        return transformationRatios;
    }

    public void setTransformationRatios(TransformationRatios transformationRatios) {
        this.transformationRatios = transformationRatios;
    }

    public PowerValues getPowerValues() {
        return powerValues;
    }

    public void setPowerValues(PowerValues powerValues) {
        this.powerValues = powerValues;
    }

    public EnergyValues getEnergyValues() {
        return energyValues;
    }

    public void setEnergyValues(EnergyValues energyValues) {
        this.energyValues = energyValues;
    }

    // ================== TOSTRING ==================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n------------------------------\n");
        sb.append("Timestamp : ").append(timestamp).append("\n\n");
        sb.append(instantaneousValues.toString());
        sb.append("\n").append(transformationRatios.toString());
        sb.append("\n").append(powerValues.toString());
        sb.append("\n").append(energyValues.toString());
        return sb.toString();
    }

    // ================== INNER CLASSES (public static) ==================

    /**
     * Valores instantáneos de tensión, corriente y factor de potencia
     */
    public static class InstantaneousValues {
        private double voltageL1, voltageL2, voltageL3;
        private double currentL1, currentL2, currentL3;
        private double powerFactorL1, powerFactorL2, powerFactorL3;
        private double powerFactorTotal;

        // Getters
        public double getVoltageL1() { return voltageL1; }
        public double getVoltageL2() { return voltageL2; }
        public double getVoltageL3() { return voltageL3; }
        public double getCurrentL1() { return currentL1; }
        public double getCurrentL2() { return currentL2; }
        public double getCurrentL3() { return currentL3; }
        public double getPowerFactorL1() { return powerFactorL1; }
        public double getPowerFactorL2() { return powerFactorL2; }
        public double getPowerFactorL3() { return powerFactorL3; }
        public double getPowerFactorTotal() { return powerFactorTotal; }

        // Setters
        public void setVoltageL1(double voltageL1) { this.voltageL1 = voltageL1; }
        public void setVoltageL2(double voltageL2) { this.voltageL2 = voltageL2; }
        public void setVoltageL3(double voltageL3) { this.voltageL3 = voltageL3; }
        public void setCurrentL1(double currentL1) { this.currentL1 = currentL1; }
        public void setCurrentL2(double currentL2) { this.currentL2 = currentL2; }
        public void setCurrentL3(double currentL3) { this.currentL3 = currentL3; }
        public void setPowerFactorL1(double powerFactorL1) { this.powerFactorL1 = powerFactorL1; }
        public void setPowerFactorL2(double powerFactorL2) { this.powerFactorL2 = powerFactorL2; }
        public void setPowerFactorL3(double powerFactorL3) { this.powerFactorL3 = powerFactorL3; }
        public void setPowerFactorTotal(double powerFactorTotal) { this.powerFactorTotal = powerFactorTotal; }

        @Override
        public String toString() {
            return String.format(
                    "Valores Tensión       Corriente        FP :\n" +
                            "Fase 1 :   %.1f [V]       %.1f [A]   %.3f\n" +
                            "Fase 2 :   %.1f [V]       %.1f [A]   %.3f\n" +
                            "Fase 3 :   %.1f [V]       %.1f [A]   %.3f\n" +
                            "FP (sum of all phases: +P/S) : %.3f",
                    voltageL1, currentL1, powerFactorL1,
                    voltageL2, currentL2, powerFactorL2,
                    voltageL3, currentL3, powerFactorL3,
                    powerFactorTotal
            );
        }
    }

    /**
     * Relaciones de transformación
     */
    public static class TransformationRatios {
        private int voltagePrimary;
        private int voltageSecondary;
        private int currentPrimary;
        private int currentSecondary;

        // Getters
        public int getVoltagePrimary() { return voltagePrimary; }
        public int getVoltageSecondary() { return voltageSecondary; }
        public int getCurrentPrimary() { return currentPrimary; }
        public int getCurrentSecondary() { return currentSecondary; }

        // Setters
        public void setVoltagePrimary(int voltagePrimary) { this.voltagePrimary = voltagePrimary; }
        public void setVoltageSecondary(int voltageSecondary) { this.voltageSecondary = voltageSecondary; }
        public void setCurrentPrimary(int currentPrimary) { this.currentPrimary = currentPrimary; }
        public void setCurrentSecondary(int currentSecondary) { this.currentSecondary = currentSecondary; }

        public double getVoltageRatio() {
            return voltageSecondary != 0 ? (double) voltagePrimary / voltageSecondary : 0;
        }

        public double getCurrentRatio() {
            return currentSecondary != 0 ? (double) currentPrimary / currentSecondary : 0;
        }

        @Override
        public String toString() {
            return String.format(
                    "Relación de Transformacion de Tensión : [deciVolts]/[deciVolts]\n" +
                            "Prim/sec  (%d / %d) = %.3f\n\n" +
                            "Relación de Transformacion Corriente : [deciAmps]/[deciAmps]\n" +
                            "Prim/sec (%d / %d) = %.3f",
                    voltagePrimary, voltageSecondary, getVoltageRatio(),
                    currentPrimary, currentSecondary, getCurrentRatio()
            );
        }
    }

    /**
     * Valores de potencia instantánea
     */
    public static class PowerValues {
        private double activePowerPlusL1, activePowerMinusL1, reactivePowerPlusL1, reactivePowerMinusL1;
        private double activePowerPlusL2, activePowerMinusL2, reactivePowerPlusL2, reactivePowerMinusL2;
        private double activePowerPlusL3, activePowerMinusL3, reactivePowerPlusL3, reactivePowerMinusL3;
        private double totalActivePowerPlus, totalActivePowerMinus, totalReactivePowerPlus, totalReactivePowerMinus;

        // Getters
        public double getActivePowerPlusL1() { return activePowerPlusL1; }
        public double getActivePowerMinusL1() { return activePowerMinusL1; }
        public double getReactivePowerPlusL1() { return reactivePowerPlusL1; }
        public double getReactivePowerMinusL1() { return reactivePowerMinusL1; }

        public double getActivePowerPlusL2() { return activePowerPlusL2; }
        public double getActivePowerMinusL2() { return activePowerMinusL2; }
        public double getReactivePowerPlusL2() { return reactivePowerPlusL2; }
        public double getReactivePowerMinusL2() { return reactivePowerMinusL2; }

        public double getActivePowerPlusL3() { return activePowerPlusL3; }
        public double getActivePowerMinusL3() { return activePowerMinusL3; }
        public double getReactivePowerPlusL3() { return reactivePowerPlusL3; }
        public double getReactivePowerMinusL3() { return reactivePowerMinusL3; }

        public double getTotalActivePowerPlus() { return totalActivePowerPlus; }
        public double getTotalActivePowerMinus() { return totalActivePowerMinus; }
        public double getTotalReactivePowerPlus() { return totalReactivePowerPlus; }
        public double getTotalReactivePowerMinus() { return totalReactivePowerMinus; }

        // Setters
        public void setActivePowerPlusL1(double v) { this.activePowerPlusL1 = v; }
        public void setActivePowerMinusL1(double v) { this.activePowerMinusL1 = v; }
        public void setReactivePowerPlusL1(double v) { this.reactivePowerPlusL1 = v; }
        public void setReactivePowerMinusL1(double v) { this.reactivePowerMinusL1 = v; }

        public void setActivePowerPlusL2(double v) { this.activePowerPlusL2 = v; }
        public void setActivePowerMinusL2(double v) { this.activePowerMinusL2 = v; }
        public void setReactivePowerPlusL2(double v) { this.reactivePowerPlusL2 = v; }
        public void setReactivePowerMinusL2(double v) { this.reactivePowerMinusL2 = v; }

        public void setActivePowerPlusL3(double v) { this.activePowerPlusL3 = v; }
        public void setActivePowerMinusL3(double v) { this.activePowerMinusL3 = v; }
        public void setReactivePowerPlusL3(double v) { this.reactivePowerPlusL3 = v; }
        public void setReactivePowerMinusL3(double v) { this.reactivePowerMinusL3 = v; }

        public void setTotalActivePowerPlus(double v) { this.totalActivePowerPlus = v; }
        public void setTotalActivePowerMinus(double v) { this.totalActivePowerMinus = v; }
        public void setTotalReactivePowerPlus(double v) { this.totalReactivePowerPlus = v; }
        public void setTotalReactivePowerMinus(double v) { this.totalReactivePowerMinus = v; }

        @Override
        public String toString() {
            return String.format(
                    "Valores       P+ [Kw]    P- [Kw]    Q+ [Kvar]  Q- [Kvar]  :\n" +
                            "Fase 1 :       %.3f      %.3f      %.3f      %.3f\n" +
                            "Fase 2 :       %.3f      %.3f      %.3f      %.3f\n" +
                            "Fase 3 :       %.3f      %.3f      %.3f      %.3f\n" +
                            "Total  :       %.3f      %.3f      %.3f      %.3f",
                    activePowerPlusL1, activePowerMinusL1, reactivePowerPlusL1, reactivePowerMinusL1,
                    activePowerPlusL2, activePowerMinusL2, reactivePowerPlusL2, reactivePowerMinusL2,
                    activePowerPlusL3, activePowerMinusL3, reactivePowerPlusL3, reactivePowerMinusL3,
                    totalActivePowerPlus, totalActivePowerMinus, totalReactivePowerPlus, totalReactivePowerMinus
            );
        }
    }

    /**
     * Valores de energía acumulada
     */
    public static class EnergyValues {
        private double activeEnergyImport;  // Activa Importada
        private double activeEnergyExport;  // Activa Exportada
        private double reactiveEnergyQ1;    // Reactiva Q1
        private double reactiveEnergyQ2;    // Reactiva Q2
        private double reactiveEnergyQ3;    // Reactiva Q3
        private double reactiveEnergyQ4;    // Reactiva Q4

        // Getters
        public double getActiveEnergyImport() { return activeEnergyImport; }
        public double getActiveEnergyExport() { return activeEnergyExport; }
        public double getReactiveEnergyQ1() { return reactiveEnergyQ1; }
        public double getReactiveEnergyQ2() { return reactiveEnergyQ2; }
        public double getReactiveEnergyQ3() { return reactiveEnergyQ3; }
        public double getReactiveEnergyQ4() { return reactiveEnergyQ4; }

        // Setters
        public void setActiveEnergyImport(double activeEnergyImport) { this.activeEnergyImport = activeEnergyImport; }
        public void setActiveEnergyExport(double activeEnergyExport) { this.activeEnergyExport = activeEnergyExport; }
        public void setReactiveEnergyQ1(double reactiveEnergyQ1) { this.reactiveEnergyQ1 = reactiveEnergyQ1; }
        public void setReactiveEnergyQ2(double reactiveEnergyQ2) { this.reactiveEnergyQ2 = reactiveEnergyQ2; }
        public void setReactiveEnergyQ3(double reactiveEnergyQ3) { this.reactiveEnergyQ3 = reactiveEnergyQ3; }
        public void setReactiveEnergyQ4(double reactiveEnergyQ4) { this.reactiveEnergyQ4 = reactiveEnergyQ4; }

        @Override
        public String toString() {
            return String.format(
                    "Activa Importada   :      %.3f  [Kwh]\n" +
                            "Activa Exportada   :      %.3f  [Kwh]\n" +
                            "Reactiva Q1        :      %.3f  [KVArh]\n" +
                            "Reactiva Q2        :      %.3f  [KVArh]\n" +
                            "Reactiva Q3        :      %.3f  [KVArh]\n" +
                            "Reactiva Q4        :      %.3f  [KVArh]",
                    activeEnergyImport, activeEnergyExport,
                    reactiveEnergyQ1, reactiveEnergyQ2,
                    reactiveEnergyQ3, reactiveEnergyQ4
            );
        }
    }
}