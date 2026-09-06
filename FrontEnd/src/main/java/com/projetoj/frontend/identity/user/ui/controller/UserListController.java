package com.projetoj.frontend.identity.user.ui.controller;

import com.projetoj.frontend.identity.user.adapter.rest.UserResponseDto;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import com.projetoj.frontend.shared.ui.ConfirmDialogs;
import com.projetoj.frontend.shared.ui.FormDialogs;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserListController {

    private final DependencyContainer container;

    @FXML
    private TableView<UserResponseDto> userTable;
    @FXML
    private TableColumn<UserResponseDto, String> usernameColumn;
    @FXML
    private TableColumn<UserResponseDto, String> fullNameColumn;
    @FXML
    private TableColumn<UserResponseDto, String> emailColumn;
    @FXML
    private TableColumn<UserResponseDto, String> roleColumn;
    @FXML
    private TableColumn<UserResponseDto, String> statusColumn;
    @FXML
    private Label messageLabel;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Button createButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;

    public UserListController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadingIndicator.setVisible(false);

        createButton.setDisable(!container.getSessionContext().canCreate("USERS"));
        editButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> userTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canUpdate("USERS"),
                userTable.getSelectionModel().selectedItemProperty()
        ));
        deleteButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> userTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canDelete("USERS"),
                userTable.getSelectionModel().selectedItemProperty()
        ));

        userTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && userTable.getSelectionModel().getSelectedItem() != null
                    && container.getSessionContext().canUpdate("USERS")) {
                onEdit();
            }
        });
        onRefresh();
    }

    @FXML
    public void onRefresh() {
        loadingIndicator.setVisible(true);
        messageLabel.setText("Carregando usuarios...");

        Task<List<UserResponseDto>> task = new Task<>() {
            @Override
            protected List<UserResponseDto> call() {
                return container.getUserApplicationService().list();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<UserResponseDto> users = task.getValue();
            userTable.setItems(FXCollections.observableArrayList(users));
            messageLabel.setText(users.size() + " usuario(s) encontrado(s).");
            loadingIndicator.setVisible(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = task.getException();
            messageLabel.setText(error instanceof ApiException ae
                    ? ApiErrorMessages.toUserMessage(ae)
                    : error.getMessage());
            loadingIndicator.setVisible(false);
        }));

        Thread thread = new Thread(task, "users-load");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void onCreate() {
        if (!container.getSessionContext().canCreate("USERS")) {
            return;
        }
        FormDialogs.open(
                userTable.getScene().getWindow(),
                "/fxml/users/user-form.fxml",
                "Novo usuario",
                type -> type == UserFormController.class ? new UserFormController(container) : null,
                (UserFormController form) -> form.prepareCreate(ignored -> onRefresh())
        );
    }

    @FXML
    public void onEdit() {
        if (!container.getSessionContext().canUpdate("USERS")) {
            return;
        }
        UserResponseDto selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        FormDialogs.open(
                userTable.getScene().getWindow(),
                "/fxml/users/user-form.fxml",
                "Editar usuario",
                type -> type == UserFormController.class ? new UserFormController(container) : null,
                (UserFormController form) -> form.prepareEdit(selected, ignored -> onRefresh())
        );
    }

    @FXML
    public void onDelete() {
        if (!container.getSessionContext().canDelete("USERS")) {
            return;
        }
        UserResponseDto selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!ConfirmDialogs.confirmDelete(selected.getUsername())) {
            return;
        }

        loadingIndicator.setVisible(true);
        messageLabel.setText("Excluindo usuario...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                container.getUserApplicationService().delete(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::onRefresh));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable error = task.getException();
            messageLabel.setText(error instanceof ApiException ae
                    ? ApiErrorMessages.toUserMessage(ae)
                    : error.getMessage());
        }));
        Thread thread = new Thread(task, "user-delete");
        thread.setDaemon(true);
        thread.start();
    }
}
