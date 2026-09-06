package com.projetoj.frontend.identity.permission.adapter.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.projetoj.frontend.identity.permission.application.port.PermissionApiPort;
import com.projetoj.frontend.shared.http.ApiClient;

import java.util.List;
import java.util.UUID;

public class RestPermissionApiAdapter implements PermissionApiPort {

    private final ApiClient apiClient;

    public RestPermissionApiAdapter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<PermissionResponseDto> list() {
        return apiClient.getList("/permissions", new TypeReference<>() {});
    }

    @Override
    public List<ActionResponseDto> listActions() {
        return apiClient.getList("/actions", new TypeReference<>() {});
    }

    @Override
    public PermissionResponseDto create(PermissionRequestDto request) {
        return apiClient.post("/permissions", request, PermissionResponseDto.class);
    }

    @Override
    public PermissionResponseDto update(UUID id, PermissionRequestDto request) {
        return apiClient.put("/permissions/" + id, request, PermissionResponseDto.class);
    }

    @Override
    public void delete(UUID id) {
        apiClient.delete("/permissions/" + id);
    }
}
