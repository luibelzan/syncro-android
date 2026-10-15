package com.celnet.syncro.session;

public class ConnectionConfig {

    public enum ConnectionType {
        BLUETOOTH,
        TCP
    }

    /** Protocolo de aplicación con el contador (v3.0). */
    public enum Protocol {
        DLMS,
        IEC870
    }

    private ConnectionType type;
    private Protocol protocol = Protocol.DLMS; // por defecto, comportamiento histórico
    private String bluetoothDeviceName;
    private String ip;
    private int port;

    // Parámetros específicos de IEC-870-5-102 (solo se usan si protocol == IEC870)
    private int iecLinkAddress;        // dirección de enlace del contador
    private int iecMeasuringPoint;     // dirección de punto de medida
    private long iecPassword;          // clave de acceso (0 = sin clave)

    public ConnectionConfig(ConnectionType type) {
        this.type = type;
    }

    public ConnectionType getType() {
        return type;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public boolean isIec870() {
        return protocol == Protocol.IEC870;
    }

    public String getBluetoothDeviceName() {
        return bluetoothDeviceName;
    }

    public void setBluetoothDeviceName(String bluetoothDeviceName) {
        this.bluetoothDeviceName = bluetoothDeviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIecLinkAddress() {
        return iecLinkAddress;
    }

    public void setIecLinkAddress(int iecLinkAddress) {
        this.iecLinkAddress = iecLinkAddress;
    }

    public int getIecMeasuringPoint() {
        return iecMeasuringPoint;
    }

    public void setIecMeasuringPoint(int iecMeasuringPoint) {
        this.iecMeasuringPoint = iecMeasuringPoint;
    }

    public long getIecPassword() {
        return iecPassword;
    }

    public void setIecPassword(long iecPassword) {
        this.iecPassword = iecPassword;
    }
}