package com.projetoj.identity.role.adapter.input.rest;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateRolePermissionsRequest(
        @NotNull List<UUID> permissionIds
) {
}
