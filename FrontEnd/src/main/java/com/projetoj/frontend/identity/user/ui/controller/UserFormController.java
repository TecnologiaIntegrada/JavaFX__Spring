package com.projetoj.frontend.identity.user.ui.controller;

import com.projetoj.frontend.identity.role.adapter.rest.RoleResponseDto;
import com.projetoj.frontend.identity.user.adapter.rest.CreateUserRequestDto;
import com.projetoj.frontend.identity.user.adapter.rest.UpdateUserRequestDto;
import com.projetoj.frontend.identity.user.adapter.rest.UserResponseDto;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class UserFormController {

    private final DependencyContainer container;
    private UUID editingId;
    private Consumer<Void> onSaved = ignored -> {};

    @FXML
    private TextField usernameField;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<RoleResponseDto> roleCombo;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;

    public UserFormController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        statusCombo.getItems().setAll("ACTIVE", "INACTIVE", "LOCKED", "PENDING");
        statusCombo.setValue("ACTIVE");
        messageLabel.setText("Carregando perfis...");
        saveButton.setDisable(true);

        Task<List<RoleResponseDto>> task = new Task<>() {
            @Override
            protected List<RoleResponseDto> call() {
                return container.getRoleApplicationService().list();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            roleCombo.setItems(FXCollections.observableArrayList(task.getValue()));
            messageLabel.setText("");
            saveButton.setDisable(false);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            messageLabel.setText("Falha ao carregar perfis.");
            saveButton.setDisable(true);
        }));
        Thread thread = new Thread(task, "user-form-roles");
        thread.setDaemon(true);
        thread.start();
    }

    public void prepareCreate(Consumer<Void> onSaved) {
        this.editingId = null;
        this.onSaved = onSaved;
        usernameField.setDisable(false);
        passwordField.setPromptText("Obrigatorio");
    }

    public void prepareEdit(UserResponseDto user, Consumer<Void> onSaved) {
        this.editingId = user.getId();
        this.onSaved = onSaved;
        usernameField.setText(user.getUsername());
        usernameField.setDisable(true);
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        statusCombo.setValue(user.getStatus());
        passwordField.setPromptText("Deixe em branco para manter");
        confirmPasswordField.setPromptText("Deixe em branco para manter");
        Platform.runLater(() -> selectRole(user.getRoleId()));
    }

    private void selectRole(UUID roleId) {
        if (roleCombo.getItems().isEmpty()) {
            Platform.runLater(() -> selectRole(roleId));
            return;
        }
        roleCombo.getItems().stream()
                .filter(role -> role.getId().equals(roleId))
                .findFirst()
                .ifPresent(roleCombo::setValue);
    }

    @FXML
    public void onSave() {
        String username = safe(usernameField.getText());
        String fullName = safe(fullNameField.getText());
        String email = safe(emailField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String status = statusCombo.getValue();
        RoleResponseDto role = roleCombo.getValue();

        if (fullName.isBlank() || email.isBlank() || status == null || role == null) {
            messageLabel.setText("Preencha os campos obrigatorios, incluindo o perfil.");
            return;
        }
        if (editingId == null && (username.isBlank() || password.isBlank())) {
            messageLabel.setText("Username e senha sao obrigatorios no cadastro.");
            return;
        }
        if (!password.isBlank() && !password.equals(confirm)) {
            messageLabel.setText("Senha e confirmacao nao conferem.");
            return;
        }
        if (!password.isBlank() && password.length() < 5) {
            messageLabel.setText("Senha deve ter no minimo 5 caracteres.");
            return;
        }

        saveButton.setDisable(true);
        messageLabel.setText("Salvando...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (editingId == null) {
                    container.getUserApplicationService().create(
                            new CreateUserRequestDto(username, fullName, email, password, status, role.getId())
                    );
                } else {
                    container.getUserApplicationService().update(
                            editingId,
                            new UpdateUserRequestDto(
                                    fullName,
                                    email,
                                    password.isBlank() ? null : password,
                                    status,
                                    role.getId()
                            )
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

        Thread thread = new Thread(task, "user-save");
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
