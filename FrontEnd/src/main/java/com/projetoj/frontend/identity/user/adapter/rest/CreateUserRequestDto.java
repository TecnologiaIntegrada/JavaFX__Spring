package com.projetoj.frontend.identity.user.adapter.rest;

import java.util.UUID;

public class CreateUserRequestDto {
    public String username;
    public String fullName;
    public String email;
    public String password;
    public String status;
    public UUID roleId;

    public CreateUserRequestDto(
            String username,
            String fullName,
            String email,
            String password,
            String status,
            UUID roleId
    ) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.status = status;
        this.roleId = roleId;
    }
}
