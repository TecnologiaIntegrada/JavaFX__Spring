package com.projetoj.frontend.identity.role.adapter.rest;

import java.util.List;
import java.util.UUID;

public class UpdateRolePermissionsRequestDto {
    public List<UUID> permissionIds;

    public UpdateRolePermissionsRequestDto(List<UUID> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
