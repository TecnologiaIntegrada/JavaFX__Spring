package com.projetoj.frontend.identity.permission.adapter.rest;

public class ModuleUpdateRequestDto {
    public String name;
    public String description;
    public Boolean active;

    public ModuleUpdateRequestDto(String name, String description, Boolean active) {
        this.name = name;
        this.description = description;
        this.active = active;
    }
}
