package com.celnet.syncro.session;

public class SessionManager {

    private static SessionManager instance;
    private ConnectionConfig connectionConfig;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setConnectionConfig(ConnectionConfig config) {
        this.connectionConfig = config;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

}
