package com.projetoj.frontend.identity.role.adapter.rest;

import java.time.LocalDateTime;
import java.util.UUID;

public class RoleResponseDto {
    public UUID id;
    public String name;
    public String description;
    public boolean active;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
