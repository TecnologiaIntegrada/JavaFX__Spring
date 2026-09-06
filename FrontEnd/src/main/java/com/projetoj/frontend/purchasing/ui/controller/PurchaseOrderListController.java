package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.shared.di.DependencyContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseOrderListController extends AbstractPurchasingListController {

    public PurchaseOrderListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "PURCHASE_ORDERS";
    }

    @Override
    protected String apiPath() {
        return "/purchase-orders";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"orderNumber", "supplierName", "status", "orderDate", "totalAmount", "currency"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Numero", "Fornecedor", "Status", "Data", "Total", "Moeda"};
    }

    @Override
    protected List<FormFieldSpec> formFieldSpecs() {
        return List.of(
                FormFieldSpec.combo("rfqId", "RFQ", "/rfqs", "id", "rfqNumber")
        );
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        Map<String, Object> m = new HashMap<>();
        m.put("rfqId", f.get("rfqId"));
        return m;
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return Map.of();
    }

    @Override
    protected void afterInitialize() {
        createButton.setText("Gerar PO das adjudicacoes");
        createButton.setOnAction(e -> onGenerate());
        editButton.setText("Enviar");
        editButton.setOnAction(e -> onSend());
        deleteButton.setText("Follow-up");
        deleteButton.setOnAction(e -> onFollowup());
        deleteButton.setVisible(true);
        deleteButton.setDisable(false);
    }

    private void onGenerate() {
        promptFormAsync("RFQ para gerar pedidos", null, values -> {
            if (values == null) {
                return;
            }
            runWrite(() -> api.post("/purchase-orders/from-awards", Map.of("rfqId", values.get("rfqId"))), "Pedidos gerados.");
        });
    }

    private void onSend() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        runWrite(() -> api.post(apiPath() + "/" + selected.get("id") + "/send", Map.of()), "Pedido enviado.");
    }

    private void onFollowup() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Follow-up");
        dialog.setHeaderText("Registrar follow-up do pedido " + selected.get("orderNumber"));
        dialog.setContentText("Comentarios:");
        dialog.showAndWait().ifPresent(comments -> {
            Map<String, Object> body = new HashMap<>();
            body.put("contactType", "TELEFONE");
            body.put("reportedStatus", "EM_ANDAMENTO");
            body.put("comments", comments);
            runWrite(() -> api.post(apiPath() + "/" + selected.get("id") + "/followups", body), "Follow-up registrado.");
        });
    }
}
