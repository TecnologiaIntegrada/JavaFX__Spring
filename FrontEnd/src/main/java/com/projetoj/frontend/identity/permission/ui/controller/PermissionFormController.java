package com.projetoj.frontend.identity.permission.ui.controller;

import com.projetoj.frontend.identity.permission.adapter.rest.ActionResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PermissionFormController {

    private final DependencyContainer container;
    private UUID editingId;
    private Consumer<Void> onSaved = ignored -> {};

    @FXML
    private ComboBox<ModuleResponseDto> moduleCombo;
    @FXML
    private ComboBox<ActionResponseDto> actionCombo;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;

    public PermissionFormController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        messageLabel.setText("Carregando catalogos...");
        saveButton.setDisable(true);

        Task<Catalogs> task = new Task<>() {
            @Override
            protected Catalogs call() {
                return new Catalogs(
                        container.getCatalogApplicationService().listModules(),
                        container.getCatalogApplicationService().listActions()
                );
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Catalogs catalogs = task.getValue();
            moduleCombo.setItems(FXCollections.observableArrayList(catalogs.modules));
            actionCombo.setItems(FXCollections.observableArrayList(catalogs.actions));
            messageLabel.setText("");
            saveButton.setDisable(false);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            messageLabel.setText("Falha ao carregar modulos/acoes.");
            saveButton.setDisable(true);
        }));

        Thread thread = new Thread(task, "permission-form-load");
        thread.setDaemon(true);
        thread.start();
    }

    public void prepareCreate(Consumer<Void> onSaved) {
        this.editingId = null;
        this.onSaved = onSaved;
    }

    public void prepareEdit(PermissionResponseDto permission, Consumer<Void> onSaved) {
        this.editingId = permission.getId();
        this.onSaved = onSaved;

        // Selecao apos catalogs carregarem
        moduleCombo.valueProperty().addListener((obs, oldV, newV) -> {
        });
        Platform.runLater(() -> selectAfterLoad(permission));
    }

    private void selectAfterLoad(PermissionResponseDto permission) {
        if (moduleCombo.getItems().isEmpty() || actionCombo.getItems().isEmpty()) {
            Platform.runLater(() -> selectAfterLoad(permission));
            return;
        }
        moduleCombo.getItems().stream()
                .filter(m -> m.getId().equals(permission.getModuleId()))
                .findFirst()
                .ifPresent(moduleCombo::setValue);
        actionCombo.getItems().stream()
                .filter(a -> a.getId().equals(permission.getActionId()))
                .findFirst()
                .ifPresent(actionCombo::setValue);
    }

    @FXML
    public void onSave() {
        ModuleResponseDto module = moduleCombo.getValue();
        ActionResponseDto action = actionCombo.getValue();
        if (module == null || action == null) {
            messageLabel.setText("Selecione modulo e acao.");
            return;
        }

        saveButton.setDisable(true);
        messageLabel.setText("Salvando...");
        PermissionRequestDto request = new PermissionRequestDto(module.getId(), action.getId());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (editingId == null) {
                    container.getCatalogApplicationService().createPermission(request);
                } else {
                    container.getCatalogApplicationService().updatePermission(editingId, request);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            onSaved.accept(null);
            close();
        });
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            messageLabel.setText(error instanceof ApiException ae
                    ? ApiErrorMessages.toUserMessage(ae)
                    : error.getMessage());
            saveButton.setDisable(false);
        });

        Thread thread = new Thread(task, "permission-save");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void onCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private record Catalogs(List<ModuleResponseDto> modules, List<ActionResponseDto> actions) {
    }
}
