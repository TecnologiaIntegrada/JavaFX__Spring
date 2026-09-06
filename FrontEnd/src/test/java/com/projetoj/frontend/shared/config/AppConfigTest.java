package com.projetoj.frontend.shared.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void shouldLoadDefaultApiBaseUrl() {
        AppConfig config = AppConfig.load();

        assertTrue(config.getApiBaseUrl().contains("/api/v1"));
        assertTrue(config.getConnectTimeoutSeconds() > 0);
        assertTrue(config.getRequestTimeoutSeconds() > 0);
    }
}
