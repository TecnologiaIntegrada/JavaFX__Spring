package com.projetoj.frontend.shared.application;

import com.projetoj.frontend.shared.http.ApiClient;
import com.projetoj.frontend.shared.http.HealthStatus;

public class CheckApiHealthUseCase {

    private final ApiClient apiClient;

    public CheckApiHealthUseCase(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public HealthStatus execute() {
        return apiClient.getHealth();
    }
}
