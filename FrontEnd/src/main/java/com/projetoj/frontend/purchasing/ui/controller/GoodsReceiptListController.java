package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.shared.di.DependencyContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoodsReceiptListController extends AbstractPurchasingListController {

    public GoodsReceiptListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "GOODS_RECEIPTS";
    }

    @Override
    protected String apiPath() {
        return "/goods-receipts";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"receiptNumber", "orderNumber", "supplierName", "receivedAt", "status"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Numero", "Pedido", "Fornecedor", "Data", "Status"};
    }

    @Override
    protected List<FormFieldSpec> formFieldSpecs() {
        return List.of(
                FormFieldSpec.combo("purchaseOrderId", "Pedido de compra", "/purchase-orders", "id", "orderNumber"),
                FormFieldSpec.text("purchaseOrderItemId", "Item do pedido (ID)"),
                FormFieldSpec.number("receivedQty", "Quantidade recebida"),
                FormFieldSpec.text("lotNumber", "Lote"),
                FormFieldSpec.text("supplierDocumentNumber", "Documento fornecedor")
        );
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        Map<String, Object> item = new HashMap<>();
        item.put("purchaseOrderItemId", f.get("purchaseOrderItemId"));
        item.put("receivedQty", Double.parseDouble(blank(f.get("receivedQty"), "1")));
        item.put("lotNumber", f.get("lotNumber"));
        Map<String, Object> m = new HashMap<>();
        m.put("purchaseOrderId", f.get("purchaseOrderId"));
        m.put("supplierDocumentNumber", f.get("supplierDocumentNumber"));
        m.put("items", List.of(item));
        return m;
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return Map.of();
    }

    @Override
    protected void afterInitialize() {
        editButton.setText("Reverter");
        editButton.setOnAction(e -> onReverse());
        deleteButton.setVisible(false);
        createButton.setText("Receber");
    }

    private void onReverse() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        runWrite(() -> api.post(apiPath() + "/" + selected.get("id") + "/reverse", Map.of()), "Recebimento revertido.");
    }
}
