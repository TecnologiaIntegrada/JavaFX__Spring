package com.projetoj.frontend.shared.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FormDialogs {

    private FormDialogs() {
    }

    public static <T> void open(
            Window owner,
            String fxmlPath,
            String title,
            Function<Class<?>, Object> controllerFactory,
            Consumer<T> afterLoad
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(FormDialogs.class.getResource(fxmlPath)));
        loader.setControllerFactory(type -> {
            Object controller = controllerFactory.apply(type);
            if (controller == null) {
                throw new IllegalStateException("Controller nao registrado: " + type.getName());
            }
            return controller;
        });
            Parent root = loader.load();
            @SuppressWarnings("unchecked")
            T controller = (T) loader.getController();
            afterLoad.accept(controller);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(title);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    Objects.requireNonNull(FormDialogs.class.getResource("/css/base.css")).toExternalForm()
            );
            scene.getStylesheets().add(
                    Objects.requireNonNull(FormDialogs.class.getResource("/css/layout.css")).toExternalForm()
            );
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao abrir formulario: " + fxmlPath, e);
        }
    }
}
