package com.projetoj.frontend.identity.role.application;

import com.projetoj.frontend.identity.role.adapter.rest.RoleResponseDto;
import com.projetoj.frontend.identity.role.application.port.RoleApiPort;

import java.util.List;

public class ListRolesUseCase {

    private final RoleApiPort roleApiPort;

    public ListRolesUseCase(RoleApiPort roleApiPort) {
        this.roleApiPort = roleApiPort;
    }

    public List<RoleResponseDto> execute() {
        return roleApiPort.list();
    }
}
