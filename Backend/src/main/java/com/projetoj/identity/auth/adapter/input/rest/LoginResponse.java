package com.projetoj.identity.auth.adapter.input.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username,
        String fullName,
        UUID roleId,
        String roleName,
        List<String> permissions,
        String apiKey,
        Instant expiresAt
) {
}
