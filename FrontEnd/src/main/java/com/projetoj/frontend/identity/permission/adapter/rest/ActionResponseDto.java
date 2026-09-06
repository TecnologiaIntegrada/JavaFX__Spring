package com.projetoj.frontend.identity.permission.adapter.rest;

import java.util.UUID;

public class ActionResponseDto {
    public UUID id;
    public String code;
    public String description;

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code + (description == null || description.isBlank() ? "" : " - " + description);
    }
}
