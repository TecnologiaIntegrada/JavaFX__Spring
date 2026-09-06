package com.projetoj.frontend.identity.user.adapter.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.projetoj.frontend.identity.user.application.port.UserApiPort;
import com.projetoj.frontend.shared.http.ApiClient;

import java.util.List;
import java.util.UUID;

public class RestUserApiAdapter implements UserApiPort {

    private final ApiClient apiClient;

    public RestUserApiAdapter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<UserResponseDto> list() {
        return apiClient.getList("/users", new TypeReference<>() {});
    }

    @Override
    public UserResponseDto create(CreateUserRequestDto request) {
        return apiClient.post("/users", request, UserResponseDto.class);
    }

    @Override
    public UserResponseDto update(UUID id, UpdateUserRequestDto request) {
        return apiClient.put("/users/" + id, request, UserResponseDto.class);
    }

    @Override
    public void delete(UUID id) {
        apiClient.delete("/users/" + id);
    }
}
