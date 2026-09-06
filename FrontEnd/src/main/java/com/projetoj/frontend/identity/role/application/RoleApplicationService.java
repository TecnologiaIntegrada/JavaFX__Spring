package com.projetoj.frontend.identity.role.application;

import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.identity.role.adapter.rest.RoleRequestDto;
import com.projetoj.frontend.identity.role.adapter.rest.RoleResponseDto;
import com.projetoj.frontend.identity.role.adapter.rest.UpdateRolePermissionsRequestDto;
import com.projetoj.frontend.identity.role.application.port.RoleApiPort;

import java.util.List;
import java.util.UUID;

public class RoleApplicationService {

    private final RoleApiPort roleApiPort;

    public RoleApplicationService(RoleApiPort roleApiPort) {
        this.roleApiPort = roleApiPort;
    }

    public List<RoleResponseDto> list() {
        return roleApiPort.list();
    }

    public RoleResponseDto create(RoleRequestDto request) {
        return roleApiPort.create(request);
    }

    public RoleResponseDto update(UUID id, RoleRequestDto request) {
        return roleApiPort.update(id, request);
    }

    public List<PermissionResponseDto> listPermissions(UUID roleId) {
        return roleApiPort.listPermissions(roleId);
    }

    public List<PermissionResponseDto> replacePermissions(UUID roleId, List<UUID> permissionIds) {
        return roleApiPort.updatePermissions(roleId, new UpdateRolePermissionsRequestDto(permissionIds));
    }

    public void delete(UUID id) {
        roleApiPort.delete(id);
    }
}
