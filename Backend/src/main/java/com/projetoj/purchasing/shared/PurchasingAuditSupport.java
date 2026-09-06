package com.projetoj.purchasing.shared;

import java.util.UUID;

public final class PurchasingAuditSupport {

    private PurchasingAuditSupport() {
    }

    public static void stampCreate(
            UUID userId,
            String userName,
            java.util.function.Consumer<UUID> setCreatedBy,
            java.util.function.Consumer<String> setCreatedByName,
            java.util.function.Consumer<UUID> setUpdatedBy,
            java.util.function.Consumer<String> setUpdatedByName
    ) {
        setCreatedBy.accept(userId);
        setCreatedByName.accept(userName);
        setUpdatedBy.accept(userId);
        setUpdatedByName.accept(userName);
    }

    public static void stampUpdate(
            UUID userId,
            String userName,
            java.util.function.Consumer<UUID> setUpdatedBy,
            java.util.function.Consumer<String> setUpdatedByName
    ) {
        setUpdatedBy.accept(userId);
        setUpdatedByName.accept(userName);
    }

    public static AuditUserDto audit(
            UUID createdBy,
            String createdByName,
            UUID updatedBy,
            String updatedByName
    ) {
        return new AuditUserDto(createdBy, createdByName, updatedBy, updatedByName);
    }
}
