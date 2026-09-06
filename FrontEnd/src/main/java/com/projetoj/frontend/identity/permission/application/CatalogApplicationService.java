package com.projetoj.frontend.identity.permission.application;

import com.projetoj.frontend.identity.permission.adapter.rest.ActionResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleCreateRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleUpdateRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.identity.permission.application.port.ModuleApiPort;
import com.projetoj.frontend.identity.permission.application.port.PermissionApiPort;

import java.util.List;
import java.util.UUID;

public class CatalogApplicationService {

    private final ModuleApiPort moduleApiPort;
    private final PermissionApiPort permissionApiPort;

    public CatalogApplicationService(ModuleApiPort moduleApiPort, PermissionApiPort permissionApiPort) {
        this.moduleApiPort = moduleApiPort;
        this.permissionApiPort = permissionApiPort;
    }

    public List<ModuleResponseDto> listModules() {
        return moduleApiPort.list();
    }

    public ModuleResponseDto createModule(ModuleCreateRequestDto request) {
        return moduleApiPort.create(request);
    }

    public ModuleResponseDto updateModule(UUID id, ModuleUpdateRequestDto request) {
        return moduleApiPort.update(id, request);
    }

    public List<PermissionResponseDto> listPermissions() {
        return permissionApiPort.list();
    }

    public List<ActionResponseDto> listActions() {
        return permissionApiPort.listActions();
    }

    public PermissionResponseDto createPermission(PermissionRequestDto request) {
        return permissionApiPort.create(request);
    }

    public PermissionResponseDto updatePermission(UUID id, PermissionRequestDto request) {
        return permissionApiPort.update(id, request);
    }

    public void deleteModule(UUID id) {
        moduleApiPort.delete(id);
    }

    public void deletePermission(UUID id) {
        permissionApiPort.delete(id);
    }
}
