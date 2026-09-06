package com.projetoj.frontend.identity.auth.application;

import com.projetoj.frontend.identity.auth.adapter.rest.LoginResponseDto;
import com.projetoj.frontend.identity.auth.application.port.AuthApiPort;
import com.projetoj.frontend.shared.http.ApiClient;
import com.projetoj.frontend.shared.security.SessionContext;

public class LoginUseCase {

    private final AuthApiPort authApiPort;
    private final SessionContext sessionContext;
    private final ApiClient apiClient;

    public LoginUseCase(AuthApiPort authApiPort, SessionContext sessionContext, ApiClient apiClient) {
        this.authApiPort = authApiPort;
        this.sessionContext = sessionContext;
        this.apiClient = apiClient;
    }

    public LoginResponseDto execute(String username, String password) {
        LoginResponseDto response = authApiPort.login(username, password);
        if (response.apiKey == null || response.apiKey.isBlank()) {
            throw new IllegalStateException("API nao retornou chave de autenticacao");
        }
        apiClient.setAuthToken(response.apiKey);
        sessionContext.authenticate(
                response.userId,
                response.username,
                response.fullName,
                response.roleId,
                response.roleName,
                response.permissions
        );
        return response;
    }
}
