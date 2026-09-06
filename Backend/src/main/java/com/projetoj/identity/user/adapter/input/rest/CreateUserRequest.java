package com.projetoj.identity.user.adapter.input.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 5, max = 100) String password,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE|LOCKED|PENDING") String status,
        @NotNull UUID roleId
) {
}
