package com.projetoj.frontend.identity.permission.application.port;

import com.projetoj.frontend.identity.permission.adapter.rest.ActionResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;

import java.util.List;
import java.util.UUID;

public interface PermissionApiPort {
    List<PermissionResponseDto> list();

    List<ActionResponseDto> listActions();

    PermissionResponseDto create(PermissionRequestDto request);

    PermissionResponseDto update(UUID id, PermissionRequestDto request);

    void delete(UUID id);
}
