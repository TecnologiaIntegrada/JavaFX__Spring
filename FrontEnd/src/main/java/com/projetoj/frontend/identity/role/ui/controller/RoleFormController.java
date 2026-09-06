package com.projetoj.frontend.identity.role.ui.controller;

import com.projetoj.frontend.identity.permission.adapter.rest.PermissionResponseDto;
import com.projetoj.frontend.identity.role.adapter.rest.RoleRequestDto;
import com.projetoj.frontend.identity.role.adapter.rest.RoleResponseDto;
import com.projetoj.frontend.identity.role.ui.viewmodel.PermissionSelectionRow;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RoleFormController {

    private final DependencyContainer container;
    private UUID editingId;
    private Consumer<Void> onSaved = ignored -> {};
    private final ObservableList<PermissionSelectionRow> permissionRows = FXCollections.observableArrayList();
    private FilteredList<PermissionSelectionRow> filteredRows;

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private TextField permissionFilterField;
    @FXML
    private TableView<PermissionSelectionRow> permissionTable;
    @FXML
    private TableColumn<PermissionSelectionRow, Boolean> selectedColumn;
    @FXML
    private TableColumn<PermissionSelectionRow, String> codeColumn;
    @FXML
    private TableColumn<PermissionSelectionRow, String> moduleColumn;
    @FXML
    private TableColumn<PermissionSelectionRow, String> actionColumn;
    @FXML
    private TableColumn<PermissionSelectionRow, String> descriptionColumn;
    @FXML
    private Label selectedCountLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;

    public RoleFormController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        activeCheck.setSelected(true);
        messageLabel.setText("Carregando permissoes...");
        saveButton.setDisable(true);

        permissionTable.setEditable(true);
        selectedColumn.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        selectedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectedColumn));
        selectedColumn.setEditable(true);

        codeColumn.setCellValueFactory(cell -> cell.getValue().codeProperty());
        moduleColumn.setCellValueFactory(cell -> cell.getValue().moduleNameProperty());
        actionColumn.setCellValueFactory(cell -> cell.getValue().actionCodeProperty());
        descriptionColumn.setCellValueFactory(cell -> cell.getValue().actionDescriptionProperty());

        filteredRows = new FilteredList<>(permissionRows, row -> true);
        permissionTable.setItems(filteredRows);

        permissionFilterField.textProperty().addListener((obs, oldV, newV) -> applyFilter(newV));
    }

    public void prepareCreate(Consumer<Void> onSaved) {
        this.editingId = null;
        this.onSaved = onSaved;
        loadPermissionGrid(Set.of());
    }

    public void prepareEdit(RoleResponseDto role, Consumer<Void> onSaved) {
        this.editingId = role.getId();
        this.onSaved = onSaved;
        nameField.setText(role.getName());
        descriptionField.setText(role.getDescription());
        activeCheck.setSelected(role.isActive());
        loadPermissionGridForEdit(role.getId());
    }

    private void loadPermissionGridForEdit(UUID roleId) {
        messageLabel.setText("Carregando permissoes do perfil...");
        saveButton.setDisable(true);

        Task<Set<UUID>> task = new Task<>() {
            @Override
            protected Set<UUID> call() {
                return container.getRoleApplicationService().listPermissions(roleId).stream()
                        .map(PermissionResponseDto::getId)
                        .collect(Collectors.toSet());
            }
        };

        task.setOnSucceeded(e -> loadPermissionGrid(task.getValue()));
        task.setOnFailed(e -> Platform.runLater(() -> {
            messageLabel.setText("Falha ao carregar permissoes do perfil.");
            saveButton.setDisable(false);
        }));

        Thread thread = new Thread(task, "role-permissions-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadPermissionGrid(Set<UUID> selectedIds) {
        Task<List<PermissionResponseDto>> task = new Task<>() {
            @Override
            protected List<PermissionResponseDto> call() {
                return container.getCatalogApplicationService().listPermissions();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Set<UUID> selected = selectedIds == null ? Set.of() : selectedIds;
            permissionRows.setAll(
                    task.getValue().stream()
                            .map(permission -> new PermissionSelectionRow(permission, selected.contains(permission.getId())))
                            .toList()
            );
            permissionRows.forEach(row -> row.selectedProperty().addListener((obs, o, n) -> updateSelectedCount()));
            updateSelectedCount();
            messageLabel.setText("Marque as permissoes que este perfil deve possuir.");
            saveButton.setDisable(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            messageLabel.setText("Falha ao carregar catalogo de permissoes.");
            saveButton.setDisable(false);
        }));

        Thread thread = new Thread(task, "permissions-catalog-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyFilter(String filterText) {
        String filter = filterText == null ? "" : filterText.trim().toLowerCase();
        filteredRows.setPredicate(row -> {
            if (filter.isBlank()) {
                return true;
            }
            return row.getCode().toLowerCase().contains(filter)
                    || row.getModuleName().toLowerCase().contains(filter)
                    || row.getActionCode().toLowerCase().contains(filter)
                    || (row.getActionDescription() != null && row.getActionDescription().toLowerCase().contains(filter));
        });
    }

    private void updateSelectedCount() {
        long count = permissionRows.stream().filter(PermissionSelectionRow::isSelected).count();
        selectedCountLabel.setText(count + " permissao(oes) selecionada(s)");
    }

    @FXML
    public void onSelectAllVisible() {
        filteredRows.forEach(row -> row.setSelected(true));
        updateSelectedCount();
    }

    @FXML
    public void onClearVisible() {
        filteredRows.forEach(row -> row.setSelected(false));
        updateSelectedCount();
    }

    @FXML
    public void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        if (name.isBlank()) {
            messageLabel.setText("Nome do perfil e obrigatorio.");
            return;
        }

        List<UUID> selectedPermissionIds = permissionRows.stream()
                .filter(PermissionSelectionRow::isSelected)
                .map(PermissionSelectionRow::getPermissionId)
                .toList();

        saveButton.setDisable(true);
        messageLabel.setText("Salvando perfil e permissoes...");
        RoleRequestDto request = new RoleRequestDto(name, description, activeCheck.isSelected());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                RoleResponseDto saved;
                if (editingId == null) {
                    saved = container.getRoleApplicationService().create(request);
                } else {
                    saved = container.getRoleApplicationService().update(editingId, request);
                }
                container.getRoleApplicationService().replacePermissions(saved.getId(), selectedPermissionIds);
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

        Thread thread = new Thread(task, "role-save");
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
}
