package com.projetoj.frontend.identity.permission.ui.controller;

import com.projetoj.frontend.identity.permission.adapter.rest.ModuleResponseDto;
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

public class ModuleListController {

    private final DependencyContainer container;

    @FXML
    private TableView<ModuleResponseDto> moduleTable;
    @FXML
    private TableColumn<ModuleResponseDto, String> codeColumn;
    @FXML
    private TableColumn<ModuleResponseDto, String> nameColumn;
    @FXML
    private TableColumn<ModuleResponseDto, String> descriptionColumn;
    @FXML
    private TableColumn<ModuleResponseDto, String> activeColumn;
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

    public ModuleListController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        activeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().isActive() ? "Sim" : "Nao"));
        loadingIndicator.setVisible(false);
        createButton.setDisable(!container.getSessionContext().canCreate("MODULES"));
        editButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> moduleTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canUpdate("MODULES"),
                moduleTable.getSelectionModel().selectedItemProperty()
        ));
        deleteButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> moduleTable.getSelectionModel().getSelectedItem() == null
                        || !container.getSessionContext().canDelete("MODULES"),
                moduleTable.getSelectionModel().selectedItemProperty()
        ));
        moduleTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && moduleTable.getSelectionModel().getSelectedItem() != null
                    && container.getSessionContext().canUpdate("MODULES")) {
                onEdit();
            }
        });
        onRefresh();
    }

    @FXML
    public void onRefresh() {
        loadingIndicator.setVisible(true);
        messageLabel.setText("Carregando modulos...");

        Task<List<ModuleResponseDto>> task = new Task<>() {
            @Override
            protected List<ModuleResponseDto> call() {
                return container.getCatalogApplicationService().listModules();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<ModuleResponseDto> modules = task.getValue();
            moduleTable.setItems(FXCollections.observableArrayList(modules));
            messageLabel.setText(modules.size() + " modulo(s) encontrado(s).");
            loadingIndicator.setVisible(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = task.getException();
            messageLabel.setText(error instanceof ApiException ae
                    ? ApiErrorMessages.toUserMessage(ae)
                    : error.getMessage());
            loadingIndicator.setVisible(false);
        }));

        Thread thread = new Thread(task, "modules-load");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void onCreate() {
        if (!container.getSessionContext().canCreate("MODULES")) {
            return;
        }
        FormDialogs.open(
                moduleTable.getScene().getWindow(),
                "/fxml/modules/module-form.fxml",
                "Novo modulo",
                type -> type == ModuleFormController.class ? new ModuleFormController(container) : null,
                (ModuleFormController form) -> form.prepareCreate(ignored -> onRefresh())
        );
    }

    @FXML
    public void onEdit() {
        if (!container.getSessionContext().canUpdate("MODULES")) {
            return;
        }
        ModuleResponseDto selected = moduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        FormDialogs.open(
                moduleTable.getScene().getWindow(),
                "/fxml/modules/module-form.fxml",
                "Editar modulo",
                type -> type == ModuleFormController.class ? new ModuleFormController(container) : null,
                (ModuleFormController form) -> form.prepareEdit(selected, ignored -> onRefresh())
        );
    }

    @FXML
    public void onDelete() {
        if (!container.getSessionContext().canDelete("MODULES")) {
            return;
        }
        ModuleResponseDto selected = moduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!ConfirmDialogs.confirmDelete(selected.getCode())) {
            return;
        }

        loadingIndicator.setVisible(true);
        messageLabel.setText("Excluindo modulo...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                container.getCatalogApplicationService().deleteModule(selected.getId());
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
        Thread thread = new Thread(task, "module-delete");
        thread.setDaemon(true);
        thread.start();
    }
}
