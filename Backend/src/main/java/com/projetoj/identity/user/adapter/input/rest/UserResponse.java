package com.projetoj.identity.user.adapter.input.rest;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String status,
        UUID roleId,
        String roleName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
