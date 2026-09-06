package com.projetoj.frontend.identity.role.ui.viewmodel;

import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class PermissionSelectionRow {

    private final UUID permissionId;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty moduleName = new SimpleStringProperty();
    private final StringProperty actionCode = new SimpleStringProperty();
    private final StringProperty actionDescription = new SimpleStringProperty();

    public PermissionSelectionRow(PermissionResponseDto permission, boolean selected) {
        this.permissionId = permission.getId();
        this.selected.set(selected);
        this.code.set(permission.getCode());
        this.moduleName.set(permission.getModuleName());
        this.actionCode.set(permission.getActionCode());
        this.actionDescription.set(permission.getActionDescription());
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public StringProperty codeProperty() {
        return code;
    }

    public StringProperty moduleNameProperty() {
        return moduleName;
    }

    public StringProperty actionCodeProperty() {
        return actionCode;
    }

    public StringProperty actionDescriptionProperty() {
        return actionDescription;
    }

    public String getCode() {
        return code.get();
    }

    public String getModuleName() {
        return moduleName.get();
    }

    public String getActionCode() {
        return actionCode.get();
    }

    public String getActionDescription() {
        return actionDescription.get();
    }
}
