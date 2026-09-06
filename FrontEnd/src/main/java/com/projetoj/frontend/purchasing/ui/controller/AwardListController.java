package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.shared.di.DependencyContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwardListController extends AbstractPurchasingListController {

    public AwardListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "QUOTE_AWARDS";
    }

    @Override
    protected String apiPath() {
        return "/quote-awards";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"rfqItemId", "supplierQuoteItemId", "awardedQty", "awardReason", "awardedAt"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Item RFQ", "Item Proposta", "Qtd", "Motivo", "Data"};
    }

    @Override
    protected List<FormFieldSpec> formFieldSpecs() {
        return List.of(
                FormFieldSpec.text("rfqItemId", "Item RFQ (ID)"),
                FormFieldSpec.text("supplierQuoteItemId", "Item proposta (ID)"),
                FormFieldSpec.number("awardedQty", "Quantidade adjudicada"),
                FormFieldSpec.text("awardReason", "Motivo"),
                FormFieldSpec.text("notes", "Observacoes")
        );
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        Map<String, Object> m = new HashMap<>();
        m.put("rfqItemId", f.get("rfqItemId"));
        m.put("supplierQuoteItemId", f.get("supplierQuoteItemId"));
        m.put("awardedQty", Double.parseDouble(blank(f.get("awardedQty"), "1")));
        m.put("awardReason", blank(f.get("awardReason"), "MENOR_PRECO"));
        m.put("notes", f.get("notes"));
        return m;
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return buildCreatePayload(f);
    }

    @Override
    protected void afterInitialize() {
        editButton.setVisible(false);
        deleteButton.setVisible(false);
        createButton.setText("Adjudicar");
    }
}
