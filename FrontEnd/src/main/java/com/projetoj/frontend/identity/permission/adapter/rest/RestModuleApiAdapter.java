package com.projetoj.frontend.identity.permission.adapter.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.projetoj.frontend.identity.permission.application.port.ModuleApiPort;
import com.projetoj.frontend.shared.http.ApiClient;

import java.util.List;
import java.util.UUID;

public class RestModuleApiAdapter implements ModuleApiPort {

    private final ApiClient apiClient;

    public RestModuleApiAdapter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<ModuleResponseDto> list() {
        return apiClient.getList("/modules", new TypeReference<>() {});
    }

    @Override
    public ModuleResponseDto create(ModuleCreateRequestDto request) {
        return apiClient.post("/modules", request, ModuleResponseDto.class);
    }

    @Override
    public ModuleResponseDto update(UUID id, ModuleUpdateRequestDto request) {
        return apiClient.put("/modules/" + id, request, ModuleResponseDto.class);
    }

    @Override
    public void delete(UUID id) {
        apiClient.delete("/modules/" + id);
    }
}
