package com.projetoj.frontend.identity.auth.ui.controller;

import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import com.projetoj.frontend.shared.ui.SceneNavigator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public class LoginController {

    private final DependencyContainer container;
    private final SceneNavigator navigator;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private Button loginButton;
    @FXML
    private ProgressIndicator loadingIndicator;

    public LoginController(DependencyContainer container, SceneNavigator navigator) {
        this.container = container;
        this.navigator = navigator;
    }

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);
        messageLabel.setText("");
        usernameField.setText("admin");
    }

    @FXML
    public void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            messageLabel.setText("Usuario e senha sao obrigatorios.");
            return;
        }

        loginButton.setDisable(true);
        loadingIndicator.setVisible(true);
        messageLabel.setText("Autenticando...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                container.getLoginUseCase().execute(username, password);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            navigator.showShell();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            loginButton.setDisable(false);
            Throwable error = task.getException();
            if (error instanceof ApiException ae) {
                messageLabel.setText(ApiErrorMessages.toUserMessage(ae));
            } else {
                messageLabel.setText(error.getMessage());
            }
        }));

        Thread thread = new Thread(task, "login-worker");
        thread.setDaemon(true);
        thread.start();
    }
}
