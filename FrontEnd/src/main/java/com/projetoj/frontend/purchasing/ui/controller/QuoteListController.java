package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.LookupOption;
import com.projetoj.frontend.purchasing.ui.form.SearchableComboBoxes;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuoteListController extends AbstractPurchasingListController {

    public QuoteListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "SUPPLIER_QUOTES";
    }

    @Override
    protected String apiPath() {
        return "/supplier-quotes";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"rfqNumber", "supplierName", "quoteDate", "currency", "status", "totalHint"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"RFQ", "Fornecedor", "Data", "Moeda", "Status", "Obs."};
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
        createButton.setText("Nova proposta");
        createButton.setOnAction(e -> onCreateQuote());
    }

    private void onCreateQuote() {
        loadingIndicator.setVisible(true);
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() {
                Map<String, Object> data = new HashMap<>();
                data.put("rfqs", loadLookupOptions("/rfqs", "id", "rfqNumber"));
                data.put("suppliers", loadLookupOptions("/suppliers", "id", "supplierLabel"));
                data.put("currencies", loadLookupOptions("/moedas", "code", "currencyLabel"));
                data.put("rfqDetails", api.list("/rfqs"));
                return data;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Map<String, Object> payload = showQuoteDialog(task.getValue());
            if (payload != null) {
                runWrite(() -> api.post(apiPath(), payload), "Proposta criada.");
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        Thread t = new Thread(task, "quote-form-load");
        t.setDaemon(true);
        t.start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> showQuoteDialog(Map<String, Object> lookups) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Nova proposta");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(620);

        ComboBox<LookupOption> rfqCombo = SearchableComboBoxes.create((List<LookupOption>) lookups.get("rfqs"));
        ComboBox<LookupOption> supplierCombo = SearchableComboBoxes.create((List<LookupOption>) lookups.get("suppliers"));
        ComboBox<LookupOption> currencyCombo = SearchableComboBoxes.create((List<LookupOption>) lookups.get("currencies"));
        SearchableComboBoxes.selectById(currencyCombo, "BRL");

        ComboBox<LookupOption> rfqItemCombo = SearchableComboBoxes.create(List.of());
        DatePicker quoteDate = new DatePicker(LocalDate.now());
        TextField offeredQty = new TextField("1");
        TextField unitPrice = new TextField("0");

        List<Map<String, Object>> rfqDetails = (List<Map<String, Object>>) lookups.get("rfqDetails");
        rfqCombo.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                SearchableComboBoxes.configure(rfqItemCombo, List.of());
                return;
            }
            Map<String, Object> rfq = rfqDetails.stream()
                    .filter(row -> selected.id().equals(api.asString(row.get("id"))))
                    .findFirst()
                    .orElse(null);
            if (rfq == null) {
                SearchableComboBoxes.configure(rfqItemCombo, List.of());
                return;
            }
            Object itemsObj = rfq.get("items");
            List<LookupOption> itemOptions = new java.util.ArrayList<>();
            if (itemsObj instanceof List<?> items) {
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        itemOptions.add(new LookupOption(
                                api.asString(map.get("id")),
                                buildLabel(map, "rfqItemLabel")
                        ));
                    }
                }
            }
            SearchableComboBoxes.configure(rfqItemCombo, itemOptions);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("RFQ"), 0, 0);
        grid.add(rfqCombo, 1, 0);
        grid.add(new Label("Fornecedor"), 0, 1);
        grid.add(supplierCombo, 1, 1);
        grid.add(new Label("Data da proposta"), 0, 2);
        grid.add(quoteDate, 1, 2);
        grid.add(new Label("Moeda"), 0, 3);
        grid.add(currencyCombo, 1, 3);
        grid.add(new Label("Item da RFQ"), 0, 4);
        grid.add(rfqItemCombo, 1, 4);
        grid.add(new Label("Quantidade ofertada"), 0, 5);
        grid.add(offeredQty, 1, 5);
        grid.add(new Label("Preço unitário"), 0, 6);
        grid.add(unitPrice, 1, 6);
        GridPane.setHgrow(rfqCombo, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) {
                return null;
            }
            LookupOption rfq = resolveCombo(rfqCombo);
            LookupOption supplier = resolveCombo(supplierCombo);
            LookupOption currency = resolveCombo(currencyCombo);
            LookupOption item = resolveCombo(rfqItemCombo);
            if (rfq == null || supplier == null || currency == null || item == null || quoteDate.getValue() == null) {
                return null;
            }
            Map<String, Object> quoteItem = new HashMap<>();
            quoteItem.put("rfqItemId", item.id());
            quoteItem.put("offeredQty", Double.parseDouble(blank(offeredQty.getText(), "1")));
            quoteItem.put("unitPrice", Double.parseDouble(blank(unitPrice.getText(), "0")));
            quoteItem.put("discountPercent", 0);
            quoteItem.put("technicalApproved", true);

            Map<String, Object> payload = new HashMap<>();
            payload.put("rfqId", rfq.id());
            payload.put("supplierId", supplier.id());
            payload.put("quoteDate", quoteDate.getValue().toString());
            payload.put("currency", currency.id());
            payload.put("freightAmount", 0);
            payload.put("insuranceAmount", 0);
            payload.put("otherCosts", 0);
            payload.put("items", List.of(quoteItem));
            return payload;
        });

        return dialog.showAndWait().orElse(null);
    }

    private LookupOption resolveCombo(ComboBox<LookupOption> combo) {
        LookupOption selected = combo.getValue();
        if (selected != null) {
            return selected;
        }
        return combo.getConverter().fromString(combo.getEditor().getText());
    }
}
