package com.projetoj.frontend.identity.role.adapter.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.identity.role.application.port.RoleApiPort;
import com.projetoj.frontend.shared.http.ApiClient;

import java.util.List;
import java.util.UUID;

public class RestRoleApiAdapter implements RoleApiPort {

    private final ApiClient apiClient;

    public RestRoleApiAdapter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<RoleResponseDto> list() {
        return apiClient.getList("/roles", new TypeReference<>() {});
    }

    @Override
    public RoleResponseDto create(RoleRequestDto request) {
        return apiClient.post("/roles", request, RoleResponseDto.class);
    }

    @Override
    public RoleResponseDto update(UUID id, RoleRequestDto request) {
        return apiClient.put("/roles/" + id, request, RoleResponseDto.class);
    }

    @Override
    public List<PermissionResponseDto> listPermissions(UUID roleId) {
        return apiClient.getList("/roles/" + roleId + "/permissions", new TypeReference<>() {});
    }

    @Override
    public List<PermissionResponseDto> updatePermissions(UUID roleId, UpdateRolePermissionsRequestDto request) {
        return apiClient.putList("/roles/" + roleId + "/permissions", request, new TypeReference<>() {});
    }

    @Override
    public void delete(UUID id) {
        apiClient.delete("/roles/" + id);
    }
}
