package com.projetoj.identity.permission.adapter.input.rest;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModuleResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
