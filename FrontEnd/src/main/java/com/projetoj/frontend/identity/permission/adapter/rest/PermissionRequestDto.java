package com.projetoj.frontend.identity.permission.adapter.rest;

import java.util.UUID;

public class PermissionRequestDto {
    public UUID moduleId;
    public UUID actionId;

    public PermissionRequestDto(UUID moduleId, UUID actionId) {
        this.moduleId = moduleId;
        this.actionId = actionId;
    }
}
