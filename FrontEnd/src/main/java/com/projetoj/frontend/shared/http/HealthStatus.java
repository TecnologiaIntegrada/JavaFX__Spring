package com.projetoj.frontend.shared.http;

public final class HealthStatus {

    private final String status;
    private final String api;
    private final String database;

    public HealthStatus(String status, String api, String database) {
        this.status = status == null ? "UNKNOWN" : status;
        this.api = api == null ? "UNKNOWN" : api;
        this.database = database == null ? "UNKNOWN" : database;
    }

    public String getStatus() {
        return status;
    }

    public String getApi() {
        return api;
    }

    public String getDatabase() {
        return database;
    }

    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status)
                && "UP".equalsIgnoreCase(api)
                && "UP".equalsIgnoreCase(database);
    }
}
