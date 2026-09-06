package com.projetoj.frontend.identity.permission.application;

import com.projetoj.frontend.identity.permission.adapter.rest.ModuleResponseDto;
import com.projetoj.frontend.identity.permission.application.port.ModuleApiPort;

import java.util.List;

public class ListModulesUseCase {

    private final ModuleApiPort moduleApiPort;

    public ListModulesUseCase(ModuleApiPort moduleApiPort) {
        this.moduleApiPort = moduleApiPort;
    }

    public List<ModuleResponseDto> execute() {
        return moduleApiPort.list();
    }
}
