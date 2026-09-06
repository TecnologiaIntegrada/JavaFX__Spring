package com.projetoj.frontend.identity.permission.ui.controller;

import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
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

public class PermissionListController {

    private final DependencyContainer container;

    @FXML
    private TableView<PermissionResponseDto> permissionTable;
    @FXML
    private TableColumn<PermissionResponseDto, String> codeColumn;
    @FXML
    private TableColumn<PermissionResponseDto, String> moduleColumn;
    @FXML
    private TableColumn<PermissionResponseDto, String> actionColumn;
    @FXML
    private TableColumn<PermissionResponseDto, String> descriptionColumn;
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

    public PermissionListController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        moduleColumn.setCellValueFactory(new PropertyValueFactory<>("moduleName"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("actionCode"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("actionDescription"));
        loadingIndicator.setVisible(false);
        createButton.setDisable(!container.getSessionContext().canCreate("PERMISSIONS"));
        editButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> permissionTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canUpdate("PERMISSIONS"),
                permissionTable.getSelectionModel().selectedItemProperty()
        ));
        deleteButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> permissionTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canDelete("PERMISSIONS"),
                permissionTable.getSelectionModel().selectedItemProperty()
        ));
        permissionTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && permissionTable.getSelectionModel().getSelectedItem() != null
                    && container.getSessionContext().canUpdate("PERMISSIONS")) {
                onEdit();
            }
        });
        onRefresh();
    }

    @FXML
    public void onRefresh() {
        loadingIndicator.setVisible(true);
        messageLabel.setText("Carregando permissoes...");

        Task<List<PermissionResponseDto>> task = new Task<>() {
            @Override
            protected List<PermissionResponseDto> call() {
                return container.getCatalogApplicationService().listPermissions();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<PermissionResponseDto> permissions = task.getValue();
            permissionTable.setItems(FXCollections.observableArrayList(permissions));
            messageLabel.setText(permissions.size() + " permissao(oes) encontrada(s).");
            loadingIndicator.setVisible(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = task.getException();
            messageLabel.setText(error instanceof ApiException ae
                    ? ApiErrorMessages.toUserMessage(ae)
                    : error.getMessage());
            loadingIndicator.setVisible(false);
        }));

        Thread thread = new Thread(task, "permissions-load");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void onCreate() {
        if (!container.getSessionContext().canCreate("PERMISSIONS")) {
            return;
        }
        FormDialogs.open(
                permissionTable.getScene().getWindow(),
                "/fxml/permissions/permission-form.fxml",
                "Nova permissao",
                type -> type == PermissionFormController.class ? new PermissionFormController(container) : null,
                (PermissionFormController form) -> form.prepareCreate(ignored -> onRefresh())
        );
    }

    @FXML
    public void onEdit() {
        if (!container.getSessionContext().canUpdate("PERMISSIONS")) {
            return;
        }
        PermissionResponseDto selected = permissionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        FormDialogs.open(
                permissionTable.getScene().getWindow(),
                "/fxml/permissions/permission-form.fxml",
                "Editar permissao",
                type -> type == PermissionFormController.class ? new PermissionFormController(container) : null,
                (PermissionFormController form) -> form.prepareEdit(selected, ignored -> onRefresh())
        );
    }

    @FXML
    public void onDelete() {
        if (!container.getSessionContext().canDelete("PERMISSIONS")) {
            return;
        }
        PermissionResponseDto selected = permissionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!ConfirmDialogs.confirmDelete(selected.getCode())) {
            return;
        }

        loadingIndicator.setVisible(true);
        messageLabel.setText("Excluindo permissao...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                container.getCatalogApplicationService().deletePermission(selected.getId());
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
        Thread thread = new Thread(task, "permission-delete");
        thread.setDaemon(true);
        thread.start();
    }
}
