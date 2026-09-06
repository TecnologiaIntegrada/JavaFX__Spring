package com.projetoj.identity.permission.adapter.input.rest;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        UUID moduleId,
        String moduleCode,
        String moduleName,
        UUID actionId,
        String actionCode,
        String actionDescription
) {
}
