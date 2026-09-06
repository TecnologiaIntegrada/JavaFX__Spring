package com.projetoj.frontend.identity.permission.adapter.rest;

public class ModuleCreateRequestDto {
    public String code;
    public String name;
    public String description;
    public Boolean active;

    public ModuleCreateRequestDto(String code, String name, String description, Boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }
}
