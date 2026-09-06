package com.projetoj.frontend.shared.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class ConfirmDialogs {

    private ConfirmDialogs() {
    }

    public static boolean confirmDelete(String itemDescription) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar exclusao");
        alert.setHeaderText("Excluir registro");
        alert.setContentText("Deseja realmente excluir: " + itemDescription + "?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
