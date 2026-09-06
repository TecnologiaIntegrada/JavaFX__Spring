package com.projetoj.identity.role.adapter.input.rest;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
