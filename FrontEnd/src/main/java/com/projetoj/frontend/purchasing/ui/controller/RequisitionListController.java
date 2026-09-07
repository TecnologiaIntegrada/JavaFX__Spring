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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RequisitionListController extends AbstractPurchasingListController {

    private Button submitButton;
    private Button viewButton;

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

        viewButton = new Button("Visualizar");
        viewButton.getStyleClass().add("secondary-button");
        viewButton.setOnAction(e -> onView());
        viewButton.setDisable(true);

        submitButton = new Button("Submeter");
        submitButton.getStyleClass().add("secondary-button");
        submitButton.setOnAction(e -> onSubmit());
        submitButton.setDisable(true);

        if (createButton.getParent() instanceof HBox toolbar) {
            int createIndex = toolbar.getChildren().indexOf(createButton);
            toolbar.getChildren().add(createIndex + 1, viewButton);
            int editIndex = toolbar.getChildren().indexOf(editButton);
            toolbar.getChildren().add(editIndex + 1, submitButton);
        }

        boolean canUpdate = container.getSessionContext().canUpdate(moduleCode());
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean draft = n != null && "DRAFT".equalsIgnoreCase(api.asString(n.get("status")));
            viewButton.setDisable(n == null);
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

    private void onView() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Object id = selected.get("id");
        loadingIndicator.setVisible(true);
        Task<RequisitionViewData> task = new Task<>() {
            @Override
            protected RequisitionViewData call() {
                Map<String, Object> requisition = api.get(apiPath() + "/" + id);
                List<Map<String, Object>> rfqs = api.list("/rfqs");
                List<Map<String, Object>> orders = api.list("/purchase-orders");
                return new RequisitionViewData(requisition, rfqs, orders);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            showRequisitionView(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        Thread t = new Thread(task, "requisition-view-load");
        t.setDaemon(true);
        t.start();
    }

    @SuppressWarnings("unchecked")
    private void showRequisitionView(RequisitionViewData data) {
        Map<String, Object> requisition = data.requisition();
        Set<String> itemIds = new HashSet<>();
        List<Map<String, Object>> items = new ArrayList<>();
        if (requisition.get("items") instanceof List<?> rawItems) {
            for (Object itemObj : rawItems) {
                if (itemObj instanceof Map<?, ?> raw) {
                    Map<String, Object> item = (Map<String, Object>) raw;
                    items.add(item);
                    String itemId = api.asString(item.get("id"));
                    if (!itemId.isBlank()) {
                        itemIds.add(itemId);
                    }
                }
            }
        }

        List<Map<String, Object>> relatedRfqs = data.rfqs().stream()
                .filter(rfq -> hasLinkedItem(rfq.get("items"), itemIds))
                .toList();
        List<Map<String, Object>> relatedOrders = data.orders().stream()
                .filter(order -> hasLinkedItem(order.get("items"), itemIds))
                .toList();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Visualizar requisição");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(820);
        dialog.getDialogPane().setPrefHeight(680);

        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(8);
        addReadOnly(header, 0, "Número", api.asString(requisition.get("requisitionNumber")));
        addReadOnly(header, 1, "Status", api.asString(requisition.get("status")));
        addReadOnly(header, 2, "Prioridade", api.asString(requisition.get("priority")));
        addReadOnly(header, 3, "Origem", api.asString(requisition.get("demandSource")));
        addReadOnly(header, 4, "Data", api.asString(requisition.get("requestDate")));
        addReadOnly(header, 5, "Justificativa", api.asString(requisition.get("justification")));

        TableView<Map<String, Object>> itemGrid = mapTable(items, List.of(
                col("Produto", row -> productLabel(row)),
                col("Quantidade", row -> api.asString(row.get("quantity"))),
                col("Unidade", row -> api.asString(row.get("uom"))),
                col("Status item", row -> api.asString(row.get("status")))
        ), 180);

        TableView<Map<String, Object>> rfqGrid = mapTable(relatedRfqs, List.of(
                col("RFQ", row -> api.asString(row.get("rfqNumber"))),
                col("Status", row -> api.asString(row.get("status"))),
                col("Emissão", row -> api.asString(row.get("issueDate"))),
                col("Validade", row -> api.asString(row.get("responseDueDate")))
        ), 120);

        TableView<Map<String, Object>> orderGrid = mapTable(relatedOrders, List.of(
                col("Pedido", row -> api.asString(row.get("orderNumber"))),
                col("Fornecedor", row -> blank(api.asString(row.get("supplierName")), api.asString(row.get("supplierTradeName")))),
                col("Status", row -> api.asString(row.get("status"))),
                col("Data", row -> api.asString(row.get("orderDate"))),
                col("Total", row -> api.asString(row.get("totalAmount")))
        ), 140);

        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (Map<String, Object> order : relatedOrders) {
            if (!(order.get("items") instanceof List<?> rawOrderItems)) {
                continue;
            }
            for (Object itemObj : rawOrderItems) {
                if (!(itemObj instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) raw;
                if (!itemIds.contains(api.asString(item.get("requisitionItemId")))) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>(item);
                row.put("orderNumber", order.get("orderNumber"));
                orderItems.add(row);
            }
        }
        TableView<Map<String, Object>> orderItemGrid = mapTable(orderItems, List.of(
                col("Pedido", row -> api.asString(row.get("orderNumber"))),
                col("Produto", row -> productLabel(row)),
                col("Qtde pedida", row -> api.asString(row.get("orderedQty"))),
                col("Unidade", row -> api.asString(row.get("uom"))),
                col("Status", row -> api.asString(row.get("status")))
        ), 140);

        VBox root = new VBox(10,
                header,
                new Label("Itens da requisição"),
                itemGrid,
                new Label("Cotações (RFQ) vinculadas"),
                rfqGrid,
                new Label("Pedidos de compra vinculados"),
                orderGrid,
                new Label("Itens dos pedidos"),
                orderItemGrid
        );
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    private boolean hasLinkedItem(Object itemsObj, Set<String> itemIds) {
        if (!(itemsObj instanceof List<?> items) || itemIds.isEmpty()) {
            return false;
        }
        for (Object itemObj : items) {
            if (itemObj instanceof Map<?, ?> item
                    && itemIds.contains(api.asString(item.get("requisitionItemId")))) {
                return true;
            }
        }
        return false;
    }

    private String productLabel(Map<String, Object> row) {
        String code = api.asString(row.get("productCode"));
        String desc = api.asString(row.get("productDescription"));
        if (desc.isBlank()) {
            desc = api.asString(row.get("description"));
        }
        return code.isBlank() ? desc : code + " - " + desc;
    }

    private void addReadOnly(GridPane grid, int row, String label, String value) {
        Label field = new Label(blank(value, "-"));
        field.setWrapText(true);
        field.setMaxWidth(560);
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private TableView<Map<String, Object>> mapTable(
            List<Map<String, Object>> rows,
            List<TableColumn<Map<String, Object>, String>> columns,
            double height
    ) {
        TableView<Map<String, Object>> tableView = new TableView<>(FXCollections.observableArrayList(rows));
        tableView.setPrefHeight(height);
        tableView.getColumns().addAll(columns);
        tableView.setPlaceholder(new Label("Nenhum registro vinculado."));
        return tableView;
    }

    private TableColumn<Map<String, Object>, String> col(String title, java.util.function.Function<Map<String, Object>, String> value) {
        TableColumn<Map<String, Object>, String> column = new TableColumn<>(title);
        column.setPrefWidth(150);
        column.setCellValueFactory(c -> new SimpleStringProperty(value.apply(c.getValue())));
        return column;
    }

    private void openRequisitionForm(Map<String, Object> current) {
        loadingIndicator.setVisible(true);
        Task<ProductLookup> task = new Task<>() {
            @Override
            protected ProductLookup call() {
                List<LookupOption> options = new ArrayList<>();
                Map<String, String> units = new HashMap<>();
                for (Map<String, Object> product : api.list("/products")) {
                    String id = api.asString(product.get("id"));
                    options.add(new LookupOption(id, buildLabel(product, "productLabel")));
                    units.put(id, blank(api.asString(product.get("unitOfMeasure")), "UN"));
                }
                return new ProductLookup(options, units);
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
    private Map<String, Object> showRequisitionDialog(Map<String, Object> current, ProductLookup products) {
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
                String productId = api.asString(item.get("productId"));
                Map<String, String> row = new HashMap<>();
                row.put("productId", productId);
                String code = api.asString(item.get("productCode"));
                String desc = api.asString(item.get("productDescription"));
                row.put("productName", code.isBlank() ? desc : code + " - " + desc);
                row.put("quantity", api.asString(item.get("quantity")));
                row.put("uom", blank(products.units().get(productId), blank(api.asString(item.get("uom")), "UN")));
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

        ComboBox<LookupOption> productCombo = SearchableComboBoxes.create(products.options());
        TextField quantityField = new TextField("1");
        TextField uomField = new TextField();
        uomField.setEditable(false);
        uomField.setDisable(true);
        uomField.setPromptText("UN");
        productCombo.valueProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                uomField.clear();
                return;
            }
            uomField.setText(blank(products.units().get(selected.id()), "UN"));
        });
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
            row.put("uom", blank(products.units().get(selected.id()), "UN"));
            row.put("requiredDate", requiredDate.getValue() == null ? LocalDate.now().toString() : requiredDate.getValue().toString());
            itemRows.add(row);
            quantityField.setText("1");
            uomField.clear();
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
                item.put("uom", blank(products.units().get(row.get("productId")), blank(row.get("uom"), "UN")));
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

    private record ProductLookup(List<LookupOption> options, Map<String, String> units) {
    }

    private record RequisitionViewData(
            Map<String, Object> requisition,
            List<Map<String, Object>> rfqs,
            List<Map<String, Object>> orders
    ) {
    }
}
