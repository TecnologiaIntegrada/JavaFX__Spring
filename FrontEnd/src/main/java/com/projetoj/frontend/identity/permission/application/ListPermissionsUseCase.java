package com.projetoj.frontend.identity.permission.application;

import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.identity.permission.application.port.PermissionApiPort;

import java.util.List;

public class ListPermissionsUseCase {

    private final PermissionApiPort permissionApiPort;

    public ListPermissionsUseCase(PermissionApiPort permissionApiPort) {
        this.permissionApiPort = permissionApiPort;
    }

    public List<PermissionResponseDto> execute() {
        return permissionApiPort.list();
    }
}
