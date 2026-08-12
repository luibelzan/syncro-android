package com.celnet.syncro.models.prime;

public class PrimeSecurityInfo {

    private ConstellationCoding constellationCoding;
    private String sarSize;
    private boolean arqEnabled;
    private String dualStackVersion;
    private int dualStackVersionCode;   // 👈 nuevo

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