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

public class RequisitionListController extends AbstractPurchasingListController {

    private Button submitButton;

    public RequisitionListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "PURCHASE_REQUISITIONS";
    }

    @Override
    protected String apiPath() {
        return "/purchase-requisitions";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"requisitionNumber", "priority", "demandSource", "status", "requestDate", "justification"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Número", "Prioridade", "Origem", "Status", "Data", "Justificativa"};
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
        createButton.setText("Nova RC");
        createButton.setOnAction(e -> openRequisitionForm(null));
        editButton.setText("Editar");
        editButton.setOnAction(e -> onEditDraft());
        deleteButton.setVisible(container.getSessionContext().canDelete(moduleCode()));

        submitButton = new Button("Submeter");
        submitButton.getStyleClass().add("secondary-button");
        submitButton.setOnAction(e -> onSubmit());
        submitButton.setDisable(true);

        if (createButton.getParent() instanceof HBox toolbar) {
            int editIndex = toolbar.getChildren().indexOf(editButton);
            toolbar.getChildren().add(editIndex + 1, submitButton);
        }

        boolean canUpdate = container.getSessionContext().canUpdate(moduleCode());
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean draft = n != null && "DRAFT".equalsIgnoreCase(api.asString(n.get("status")));
            editButton.setDisable(n == null || !canUpdate || !draft);
            deleteButton.setDisable(n == null || !container.getSessionContext().canDelete(moduleCode()) || !draft);
            submitButton.setDisable(n == null || !canUpdate || !draft);
        });
    }

    private void onEditDraft() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !"DRAFT".equalsIgnoreCase(api.asString(selected.get("status")))) {
            messageLabel.setText("Somente requisições em rascunho (DRAFT) podem ser editadas.");
            return;
        }
        openRequisitionForm(selected);
    }

    private void onSubmit() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!"DRAFT".equalsIgnoreCase(api.asString(selected.get("status")))) {
            messageLabel.setText("Somente requisições em rascunho podem ser submetidas.");
            return;
        }
        runWrite(() -> api.post(apiPath() + "/" + selected.get("id") + "/submit", Map.of()), "RC submetida.");
    }

    private void openRequisitionForm(Map<String, Object> current) {
        loadingIndicator.setVisible(true);
        Task<List<LookupOption>> task = new Task<>() {
            @Override
            protected List<LookupOption> call() {
                return loadLookupOptions("/products", "id", "productLabel");
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Map<String, Object> payload = showRequisitionDialog(current, task.getValue());
            if (payload == null) {
                return;
            }
            if (current == null) {
                runWrite(() -> api.post(apiPath(), payload), "Requisição criada.");
            } else {
                runWrite(() -> api.put(apiPath() + "/" + current.get("id"), payload), "Requisição atualizada.");
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        Thread t = new Thread(task, "requisition-form-load");
        t.setDaemon(true);
        t.start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> showRequisitionDialog(Map<String, Object> current, List<LookupOption> products) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle(current == null ? "Novo registro" : "Editar requisição");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().setPrefHeight(580);

        ComboBox<LookupOption> priorityCombo = new ComboBox<>(FXCollections.observableArrayList(
                new LookupOption("ALTA", "Alta"),
                new LookupOption("MEDIA", "Média"),
                new LookupOption("BAIXA", "Baixa")
        ));
        priorityCombo.setMaxWidth(Double.MAX_VALUE);
        priorityCombo.setValue(priorityCombo.getItems().get(1));

        TextField demandSource = new TextField("MANUAL");
        TextArea justification = new TextArea();
        justification.setPrefRowCount(4);
        justification.setWrapText(true);
        DatePicker requiredDate = new DatePicker(LocalDate.now().plusDays(7));

        if (current != null) {
            String priority = api.asString(current.get("priority")).toUpperCase();
            priorityCombo.getItems().stream()
                    .filter(option -> option.id().equals(priority))
                    .findFirst()
                    .ifPresentOrElse(priorityCombo::setValue, () -> priorityCombo.setValue(priorityCombo.getItems().get(1)));
            demandSource.setText(blank(api.asString(current.get("demandSource")), "MANUAL"));
            justification.setText(api.asString(current.get("justification")));
        }

        ObservableList<Map<String, String>> itemRows = FXCollections.observableArrayList();
        if (current != null && current.get("items") instanceof List<?> items) {
            for (Object itemObj : items) {
                if (!(itemObj instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) raw;
                Map<String, String> row = new HashMap<>();
                row.put("productId", api.asString(item.get("productId")));
                String code = api.asString(item.get("productCode"));
                String desc = api.asString(item.get("productDescription"));
                row.put("productName", code.isBlank() ? desc : code + " - " + desc);
                row.put("quantity", api.asString(item.get("quantity")));
                row.put("uom", blank(api.asString(item.get("uom")), "UN"));
                row.put("requiredDate", api.asString(item.get("requiredDate")));
                itemRows.add(row);
            }
        }

        TableView<Map<String, String>> productGrid = new TableView<>(itemRows);
        productGrid.setPrefHeight(200);
        TableColumn<Map<String, String>, String> productCol = new TableColumn<>("Produto");
        productCol.setPrefWidth(320);
        productCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrDefault("productName", "")));
        TableColumn<Map<String, String>, String> qtyCol = new TableColumn<>("Quantidade");
        qtyCol.setPrefWidth(110);
        qtyCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrDefault("quantity", "")));
        TableColumn<Map<String, String>, String> uomCol = new TableColumn<>("Unidade");
        uomCol.setPrefWidth(100);
        uomCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrDefault("uom", "")));
        productGrid.getColumns().addAll(productCol, qtyCol, uomCol);

        ComboBox<LookupOption> productCombo = SearchableComboBoxes.create(products);
        TextField quantityField = new TextField("1");
        TextField uomField = new TextField("UN");
        Button addItem = new Button("Adicionar");
        addItem.setOnAction(e -> {
            LookupOption chosen = productCombo.getValue();
            if (chosen == null) {
                chosen = productCombo.getConverter().fromString(productCombo.getEditor().getText());
            }
            if (chosen == null) {
                return;
            }
            final LookupOption selected = chosen;
            Map<String, String> row = new HashMap<>();
            row.put("productId", selected.id());
            row.put("productName", selected.label());
            row.put("quantity", blank(quantityField.getText(), "1"));
            row.put("uom", blank(uomField.getText(), "UN"));
            row.put("requiredDate", requiredDate.getValue() == null ? LocalDate.now().toString() : requiredDate.getValue().toString());
            itemRows.add(row);
            quantityField.setText("1");
            uomField.setText("UN");
            productCombo.setValue(null);
            productCombo.getEditor().clear();
        });
        Button removeItem = new Button("Remover");
        removeItem.setOnAction(e -> {
            Map<String, String> selected = productGrid.getSelectionModel().getSelectedItem();
            if (selected != null) {
                itemRows.remove(selected);
            }
        });

        HBox itemActions = new HBox(8, productCombo, quantityField, uomField, addItem, removeItem);
        HBox.setHgrow(productCombo, Priority.ALWAYS);
        quantityField.setPrefWidth(90);
        uomField.setPrefWidth(80);

        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);
        header.add(new Label("Prioridade"), 0, 0);
        header.add(priorityCombo, 1, 0);
        header.add(new Label("Origem da demanda"), 0, 1);
        header.add(demandSource, 1, 1);
        header.add(new Label("Justificativa"), 0, 2);
        header.add(justification, 1, 2);
        header.add(new Label("Data de necessidade"), 0, 3);
        header.add(requiredDate, 1, 3);
        GridPane.setHgrow(priorityCombo, Priority.ALWAYS);
        GridPane.setHgrow(justification, Priority.ALWAYS);

        VBox root = new VBox(12,
                header,
                new Label("Produtos da requisição"),
                itemActions,
                productGrid
        );
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) {
                return null;
            }
            if (priorityCombo.getValue() == null || itemRows.isEmpty()) {
                return null;
            }
            if (justification.getText() == null || justification.getText().isBlank()) {
                return null;
            }
            String dateValue = requiredDate.getValue() == null
                    ? LocalDate.now().toString()
                    : requiredDate.getValue().toString();

            List<Map<String, Object>> items = new ArrayList<>();
            int line = 1;
            for (Map<String, String> row : itemRows) {
                Map<String, Object> item = new HashMap<>();
                item.put("lineNumber", line++);
                item.put("productId", row.get("productId"));
                item.put("quantity", Double.parseDouble(blank(row.get("quantity"), "1")));
                item.put("uom", blank(row.get("uom"), "UN"));
                item.put("requiredDate", blank(row.get("requiredDate"), dateValue));
                items.add(item);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("priority", priorityCombo.getValue().id());
            payload.put("demandSource", blank(demandSource.getText(), "MANUAL"));
            payload.put("justification", justification.getText().trim());
            payload.put("requestDate", LocalDate.now().toString());
            payload.put("items", items);
            return payload;
        });

        return dialog.showAndWait().orElse(null);
    }
}
