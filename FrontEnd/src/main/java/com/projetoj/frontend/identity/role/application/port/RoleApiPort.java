package com.projetoj.frontend.identity.role.application.port;

import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.identity.role.adapter.rest.RoleRequestDto;
import com.projetoj.frontend.identity.role.adapter.rest.RoleResponseDto;
import com.projetoj.frontend.identity.role.adapter.rest.UpdateRolePermissionsRequestDto;

import java.util.List;
import java.util.UUID;

public interface RoleApiPort {
    List<RoleResponseDto> list();

    RoleResponseDto create(RoleRequestDto request);

    RoleResponseDto update(UUID id, RoleRequestDto request);

    List<PermissionResponseDto> listPermissions(UUID roleId);

    List<PermissionResponseDto> updatePermissions(UUID roleId, UpdateRolePermissionsRequestDto request);

    void delete(UUID id);
}
