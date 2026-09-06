package com.projetoj.frontend.identity.auth.adapter.rest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoginResponseDto {
    public UUID userId;
    public String username;
    public String fullName;
    public UUID roleId;
    public String roleName;
    public List<String> permissions = new ArrayList<>();
    public String apiKey;
    public Instant expiresAt;
}
