package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.LookupOption;
import com.projetoj.frontend.purchasing.ui.form.SearchableComboBoxes;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RfqListController extends AbstractPurchasingListController {

    public RfqListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "RFQS";
    }

    @Override
    protected String apiPath() {
        return "/rfqs";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"rfqNumber", "description", "status", "issueDate", "responseDueDate"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Número", "Descrição", "Status", "Emissão", "Prazo"};
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        return Map.of();
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return Map.of();
    }

    @Override
    protected void afterInitialize() {
        editButton.setVisible(false);
        deleteButton.setVisible(false);
        createButton.setText("Nova solicitação");
        createButton.setOnAction(e -> onCreateRfq());
    }

    private void onCreateRfq() {
        loadingIndicator.setVisible(true);
        Task<Map<String, List<LookupOption>>> task = new Task<>() {
            @Override
            protected Map<String, List<LookupOption>> call() {
                Map<String, List<LookupOption>> data = new HashMap<>();
                data.put("requisitions", loadLookupOptions("/purchase-requisitions?status=APPROVED", "id", "requisitionLabel"));
                data.put("suppliers", loadLookupOptions("/suppliers", "id", "supplierLabel"));
                return data;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Map<String, Object> payload = showRfqDialog(task.getValue());
            if (payload != null) {
                runWrite(() -> api.post(apiPath(), payload), "Solicitação de cotação criada.");
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        Thread t = new Thread(task, "rfq-form-load");
        t.setDaemon(true);
        t.start();
    }

    private Map<String, Object> showRfqDialog(Map<String, List<LookupOption>> lookups) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Nova solicitação de cotação (RFQ)");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(720);
        dialog.getDialogPane().setPrefHeight(560);

        ComboBox<LookupOption> requisitionCombo = SearchableComboBoxes.create(lookups.getOrDefault("requisitions", List.of()));
        DatePicker dueDate = new DatePicker(LocalDate.now().plusDays(7));
        TextArea description = new TextArea();
        description.setPrefRowCount(5);
        description.setWrapText(true);
        description.setPromptText("Descrição da solicitação de cotação");

        ObservableList<Map<String, String>> supplierRows = FXCollections.observableArrayList();
        TableView<Map<String, String>> supplierGrid = new TableView<>(supplierRows);
        supplierGrid.setPrefHeight(180);

        TableColumn<Map<String, String>, String> nameCol = new TableColumn<>("Fornecedor");
        nameCol.setPrefWidth(280);
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrDefault("supplierName", "")));
        TableColumn<Map<String, String>, String> commentsCol = new TableColumn<>("Orientações / comentários");
        commentsCol.setPrefWidth(320);
        commentsCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrDefault("orientationComments", "")));
        supplierGrid.getColumns().addAll(nameCol, commentsCol);

        ComboBox<LookupOption> supplierCombo = SearchableComboBoxes.create(lookups.getOrDefault("suppliers", List.of()));
        TextField orientationField = new TextField();
        orientationField.setPromptText("Orientações para este fornecedor");
        Button addSupplier = new Button("Adicionar");
        addSupplier.setOnAction(e -> {
            LookupOption chosen = supplierCombo.getValue();
            if (chosen == null) {
                chosen = supplierCombo.getConverter().fromString(supplierCombo.getEditor().getText());
            }
            if (chosen == null) {
                return;
            }
            final LookupOption selected = chosen;
            boolean exists = supplierRows.stream().anyMatch(row -> selected.id().equals(row.get("supplierId")));
            if (exists) {
                return;
            }
            Map<String, String> row = new HashMap<>();
            row.put("supplierId", selected.id());
            row.put("supplierName", selected.label());
            row.put("orientationComments", orientationField.getText() == null ? "" : orientationField.getText().trim());
            supplierRows.add(row);
            orientationField.clear();
            supplierCombo.setValue(null);
            supplierCombo.getEditor().clear();
        });
        Button removeSupplier = new Button("Remover");
        removeSupplier.setOnAction(e -> {
            Map<String, String> selected = supplierGrid.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supplierRows.remove(selected);
            }
        });

        HBox supplierActions = new HBox(8, supplierCombo, orientationField, addSupplier, removeSupplier);
        HBox.setHgrow(supplierCombo, Priority.ALWAYS);
        HBox.setHgrow(orientationField, Priority.ALWAYS);

        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);
        header.add(new Label("Requisição"), 0, 0);
        header.add(requisitionCombo, 1, 0);
        header.add(new Label("Prazo de resposta"), 0, 1);
        header.add(dueDate, 1, 1);
        header.add(new Label("Descrição"), 0, 2);
        header.add(description, 1, 2);
        GridPane.setHgrow(requisitionCombo, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);

        VBox root = new VBox(12,
                header,
                new Label("Fornecedores (tabela filha da RFQ)"),
                supplierActions,
                supplierGrid
        );
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) {
                return null;
            }
            LookupOption requisition = requisitionCombo.getValue();
            if (requisition == null) {
                requisition = requisitionCombo.getConverter().fromString(requisitionCombo.getEditor().getText());
            }
            if (requisition == null || dueDate.getValue() == null || supplierRows.isEmpty()) {
                return null;
            }
            List<Map<String, Object>> suppliers = new ArrayList<>();
            for (Map<String, String> row : supplierRows) {
                Map<String, Object> item = new HashMap<>();
                item.put("supplierId", row.get("supplierId"));
                item.put("orientationComments", row.get("orientationComments"));
                suppliers.add(item);
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("requisitionId", requisition.id());
            payload.put("responseDueDate", dueDate.getValue().toString());
            payload.put("description", description.getText() == null ? "" : description.getText().trim());
            payload.put("suppliers", suppliers);
            return payload;
        });

        return dialog.showAndWait().orElse(null);
    }
}
