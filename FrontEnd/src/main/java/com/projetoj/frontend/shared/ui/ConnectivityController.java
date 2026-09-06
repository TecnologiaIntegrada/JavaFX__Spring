package com.projetoj.frontend.shared.ui;

import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiException;
import com.projetoj.frontend.shared.http.HealthStatus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

public class ConnectivityController {

    private final DependencyContainer container;

    @FXML
    private Label statusLabel;
    @FXML
    private Label apiStatusLabel;
    @FXML
    private Label databaseStatusLabel;
    @FXML
    private Label apiUrlLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button checkHealthButton;
    @FXML
    private ProgressIndicator loadingIndicator;

    public ConnectivityController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        apiUrlLabel.setText(container.getAppConfig().getApiBaseUrl());
        loadingIndicator.setVisible(false);
        statusLabel.setText("Aguardando");
        apiStatusLabel.setText("-");
        databaseStatusLabel.setText("-");
        messageLabel.setText("Use esta tela para diagnosticar a comunicacao FrontEnd → Backend → PostgreSQL.");
        onCheckHealth();
    }

    @FXML
    public void onCheckHealth() {
        setLoading(true);
        messageLabel.setText("Verificando Backend e banco de dados...");

        Task<HealthStatus> task = new Task<>() {
            @Override
            protected HealthStatus call() {
                return container.getCheckApiHealthUseCase().execute();
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            HealthStatus health = task.getValue();
            statusLabel.setText(health.getStatus());
            apiStatusLabel.setText(health.getApi());
            databaseStatusLabel.setText(health.getDatabase());
            if (health.isHealthy()) {
                messageLabel.setText("Conexao OK: FrontEnd → Backend → PostgreSQL.");
            } else if ("DOWN".equalsIgnoreCase(health.getDatabase())) {
                messageLabel.setText("Backend respondeu, mas o PostgreSQL esta indisponivel.");
            } else {
                messageLabel.setText("Conexao incompleta. Verifique o Backend.");
            }
            setLoading(false);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable error = task.getException();
            statusLabel.setText("DOWN");
            apiStatusLabel.setText("DOWN");
            databaseStatusLabel.setText("UNKNOWN");
            messageLabel.setText(error instanceof ApiException ae ? ae.getMessage() : error.getMessage());
            setLoading(false);
        }));

        Thread thread = new Thread(task, "health-check-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        checkHealthButton.setDisable(loading);
    }
}
