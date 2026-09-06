package com.projetoj.frontend.identity.auth.adapter.rest;

import com.projetoj.frontend.identity.auth.application.port.AuthApiPort;
import com.projetoj.frontend.shared.http.ApiClient;

public class RestAuthApiAdapter implements AuthApiPort {

    private final ApiClient apiClient;

    public RestAuthApiAdapter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public LoginResponseDto login(String username, String password) {
        return apiClient.postLogin(username, password, LoginResponseDto.class);
    }
}
