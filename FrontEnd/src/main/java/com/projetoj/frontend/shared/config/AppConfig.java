package com.projetoj.frontend.shared.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/v1";
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 30;

    private final String apiBaseUrl;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;

    private AppConfig(String apiBaseUrl, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        this.apiBaseUrl = apiBaseUrl;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar application.properties", e);
        }

        String baseUrl = properties.getProperty("api.base-url", DEFAULT_BASE_URL);
        int connectTimeout = parseIntProperty(properties, "api.connect-timeout-seconds", DEFAULT_CONNECT_TIMEOUT_SECONDS);
        int requestTimeout = parseIntProperty(properties, "api.request-timeout-seconds", DEFAULT_REQUEST_TIMEOUT_SECONDS);

        return new AppConfig(baseUrl, connectTimeout, requestTimeout);
    }

    private static int parseIntProperty(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }
}
