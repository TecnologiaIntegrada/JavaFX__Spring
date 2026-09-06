package com.projetoj.identity.permission.adapter.input.rest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePermissionRequest(
        @NotNull UUID moduleId,
        @NotNull UUID actionId
) {
}
