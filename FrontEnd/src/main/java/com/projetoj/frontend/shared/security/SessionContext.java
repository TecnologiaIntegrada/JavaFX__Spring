package com.projetoj.frontend.shared.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SessionContext {

    private UUID userId;
    private String username;
    private String fullName;
    private UUID roleId;
    private String roleName;
    private Set<String> permissions = new HashSet<>();

    public void authenticate(
            UUID userId,
            String username,
            String fullName,
            UUID roleId,
            String roleName,
            List<String> permissions
    ) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.roleId = roleId;
        this.roleName = roleName;
        this.permissions = new HashSet<>(permissions == null ? List.of() : permissions);
    }

    public void clear() {
        userId = null;
        username = null;
        fullName = null;
        roleId = null;
        roleName = null;
        permissions = new HashSet<>();
    }

    public boolean isAuthenticated() {
        return userId != null && roleId != null;
    }

    public boolean hasPermission(String permissionCode) {
        return permissions.contains(permissionCode);
    }

    public boolean canView(String moduleCode) {
        return hasPermission(moduleCode + ":VIEW");
    }

    public boolean canCreate(String moduleCode) {
        return hasPermission(moduleCode + ":CREATE");
    }

    public boolean canUpdate(String moduleCode) {
        return hasPermission(moduleCode + ":UPDATE");
    }

    public boolean canDelete(String moduleCode) {
        return hasPermission(moduleCode + ":DELETE");
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
}
