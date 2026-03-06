package com.example.syncro.session;

public class ConnectionConfig {

    public enum ConnectionType {
        BLUETOOTH,
        TCP
    }

    private ConnectionType type;
    private String bluetoothDeviceName;
    private String ip;
    private int port;

    public ConnectionConfig(ConnectionType type) {
        this.type = type;
    }

    public ConnectionType getType() {
        return type;
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
}
