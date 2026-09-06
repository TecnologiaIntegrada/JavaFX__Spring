package com.projetoj.frontend.identity.permission.application.port;

import com.projetoj.frontend.identity.permission.adapter.rest.ModuleCreateRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ModuleApiPort {
    List<ModuleResponseDto> list();

    ModuleResponseDto create(ModuleCreateRequestDto request);

    ModuleResponseDto update(UUID id, ModuleUpdateRequestDto request);

    void delete(UUID id);
}
