package com.projetoj.purchasing.shared;

import java.util.UUID;

public record AuditUserDto(
        UUID createdById,
        String createdByName,
        UUID updatedById,
        String updatedByName
) {
    public static AuditUserDto empty() {
        return new AuditUserDto(null, null, null, null);
    }
}
