package com.projetoj.frontend;

import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import com.projetoj.frontend.shared.ui.SceneNavigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private final DependencyContainer container = DependencyContainer.createDefault();

    @Override
    public void start(Stage stage) {
        SceneNavigator navigator = new SceneNavigator(stage, container);
        container.getApiClient().setOnAuthSessionInvalid(exception ->
                Platform.runLater(() -> handleAuthSessionInvalid(navigator, exception))
        );
        navigator.showLogin();
    }

    private void handleAuthSessionInvalid(SceneNavigator navigator, ApiException exception) {
        container.getSessionContext().clear();
        container.getApiClient().clearAuthToken();

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Sessao expirada");
        alert.setHeaderText("Tempo de autenticacao excedido");
        alert.setContentText(ApiErrorMessages.toUserMessage(exception));
        alert.showAndWait();

        navigator.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
