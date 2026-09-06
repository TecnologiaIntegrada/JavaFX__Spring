package com.projetoj.frontend.identity.role.adapter.rest;

public class RoleRequestDto {
    public String name;
    public String description;
    public Boolean active;

    public RoleRequestDto(String name, String description, Boolean active) {
        this.name = name;
        this.description = description;
        this.active = active;
    }
}
