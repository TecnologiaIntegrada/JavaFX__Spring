package com.projetoj.frontend.identity.user.adapter.rest;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponseDto {
    public UUID id;
    public String username;
    public String fullName;
    public String email;
    public String status;
    public UUID roleId;
    public String roleName;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }
}
