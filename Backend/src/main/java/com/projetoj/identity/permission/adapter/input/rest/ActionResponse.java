package com.projetoj.identity.permission.adapter.input.rest;

import java.util.UUID;

public record ActionResponse(UUID id, String code, String description) {
}
