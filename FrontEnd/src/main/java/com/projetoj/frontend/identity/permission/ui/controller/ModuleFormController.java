package com.projetoj.frontend.identity.permission.ui.controller;

import com.projetoj.frontend.identity.permission.adapter.rest.ModuleCreateRequestDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleResponseDto;
import com.projetoj.frontend.identity.permission.adapter.rest.ModuleUpdateRequestDto;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.UUID;
import java.util.function.Consumer;

public class ModuleFormController {

    private final DependencyContainer container;
    private UUID editingId;
    private Consumer<Void> onSaved = ignored -> {};

    @FXML
    private TextField codeField;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;

    public ModuleFormController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        activeCheck.setSelected(true);
        messageLabel.setText("");
    }

    public void prepareCreate(Consumer<Void> onSaved) {
        this.editingId = null;
        this.onSaved = onSaved;
        codeField.setDisable(false);
    }

    public void prepareEdit(ModuleResponseDto module, Consumer<Void> onSaved) {
        this.editingId = module.getId();
        this.onSaved = onSaved;
        codeField.setText(module.getCode());
        codeField.setDisable(true);
        nameField.setText(module.getName());
        descriptionField.setText(module.getDescription());
        activeCheck.setSelected(module.isActive());
    }

    @FXML
    public void onSave() {
        String code = safe(codeField.getText());
        String name = safe(nameField.getText());
        String description = safe(descriptionField.getText());
        if (name.isBlank() || (editingId == null && code.isBlank())) {
            messageLabel.setText("Codigo e nome sao obrigatorios.");
            return;
        }

        saveButton.setDisable(true);
        messageLabel.setText("Salvando...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (editingId == null) {
                    container.getCatalogApplicationService().createModule(
                            new ModuleCreateRequestDto(code, name, description, activeCheck.isSelected())
                    );
                } else {
                    container.getCatalogApplicationService().updateModule(
                            editingId,
                            new ModuleUpdateRequestDto(name, description, activeCheck.isSelected())
                    );
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

        Thread thread = new Thread(task, "module-save");
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
