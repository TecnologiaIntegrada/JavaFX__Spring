package com.projetoj.frontend.identity.role.ui.controller;

import com.projetoj.frontend.identity.role.adapter.rest.RoleResponseDto;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import com.projetoj.frontend.shared.ui.ConfirmDialogs;
import com.projetoj.frontend.shared.ui.FormDialogs;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
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

public class RoleListController {

    private final DependencyContainer container;

    @FXML
    private TableView<RoleResponseDto> roleTable;
    @FXML
    private TableColumn<RoleResponseDto, String> nameColumn;
    @FXML
    private TableColumn<RoleResponseDto, String> descriptionColumn;
    @FXML
    private TableColumn<RoleResponseDto, String> activeColumn;
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

    public RoleListController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        activeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().isActive() ? "Sim" : "Nao"));
        loadingIndicator.setVisible(false);
        createButton.setDisable(!container.getSessionContext().canCreate("ROLES"));
        editButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> roleTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canUpdate("ROLES"),
                roleTable.getSelectionModel().selectedItemProperty()
        ));
        deleteButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> roleTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canDelete("ROLES"),
                roleTable.getSelectionModel().selectedItemProperty()
        ));
        roleTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && roleTable.getSelectionModel().getSelectedItem() != null
                    && container.getSessionContext().canUpdate("ROLES")) {
                onEdit();
            }
        });
        onRefresh();
    }

    @FXML
    public void onRefresh() {
        loadingIndicator.setVisible(true);
        messageLabel.setText("Carregando perfis...");

        Task<List<RoleResponseDto>> task = new Task<>() {
            @Override
            protected List<RoleResponseDto> call() {
                return container.getRoleApplicationService().list();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<RoleResponseDto> roles = task.getValue();
            roleTable.setItems(FXCollections.observableArrayList(roles));
            messageLabel.setText(roles.size() + " perfil(is) encontrado(s).");
            loadingIndicator.setVisible(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = task.getException();
            messageLabel.setText(error instanceof ApiException ae
                    ? ApiErrorMessages.toUserMessage(ae)
                    : error.getMessage());
            loadingIndicator.setVisible(false);
        }));

        Thread thread = new Thread(task, "roles-load");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void onCreate() {
        if (!container.getSessionContext().canCreate("ROLES")) {
            return;
        }
        FormDialogs.open(
                roleTable.getScene().getWindow(),
                "/fxml/roles/role-form.fxml",
                "Novo perfil",
                type -> type == RoleFormController.class ? new RoleFormController(container) : null,
                (RoleFormController form) -> form.prepareCreate(ignored -> onRefresh())
        );
    }

    @FXML
    public void onEdit() {
        if (!container.getSessionContext().canUpdate("ROLES")) {
            return;
        }
        RoleResponseDto selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        FormDialogs.open(
                roleTable.getScene().getWindow(),
                "/fxml/roles/role-form.fxml",
                "Editar perfil",
                type -> type == RoleFormController.class ? new RoleFormController(container) : null,
                (RoleFormController form) -> form.prepareEdit(selected, ignored -> onRefresh())
        );
    }

    @FXML
    public void onDelete() {
        if (!container.getSessionContext().canDelete("ROLES")) {
            return;
        }
        RoleResponseDto selected = roleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!ConfirmDialogs.confirmDelete(selected.getName())) {
            return;
        }

        loadingIndicator.setVisible(true);
        messageLabel.setText("Excluindo perfil...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                container.getRoleApplicationService().delete(selected.getId());
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
        Thread thread = new Thread(task, "role-delete");
        thread.setDaemon(true);
        thread.start();
    }
}
