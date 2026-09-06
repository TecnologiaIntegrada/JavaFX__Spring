package com.projetoj.frontend.identity.permission.adapter.rest;

import java.util.UUID;

public class PermissionResponseDto {
    public UUID id;
    public String code;
    public UUID moduleId;
    public String moduleCode;
    public String moduleName;
    public UUID actionId;
    public String actionCode;
    public String actionDescription;

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public UUID getActionId() {
        return actionId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getActionDescription() {
        return actionDescription;
    }
}
