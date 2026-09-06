package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.shared.di.DependencyContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductListController extends AbstractPurchasingListController {

    public ProductListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "PRODUCTS";
    }

    @Override
    protected String apiPath() {
        return "/products";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"code", "description", "unitOfMeasure", "supplierName", "active"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Código", "Descrição", "UN", "Fornecedor principal", "Ativo"};
    }

    @Override
    protected List<FormFieldSpec> formFieldSpecs() {
        return List.of(
                FormFieldSpec.text("code", "Código"),
                FormFieldSpec.text("description", "Descrição"),
                FormFieldSpec.text("unitOfMeasure", "Unidade"),
                FormFieldSpec.combo("supplierId", "Fornecedor principal", "/suppliers", "id", "supplierLabel")
        );
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", f.get("code"));
        m.put("description", f.get("description"));
        m.put("unitOfMeasure", blank(f.get("unitOfMeasure"), "UN"));
        if (f.get("supplierId") != null && !f.get("supplierId").isBlank()) {
            m.put("supplierId", f.get("supplierId"));
        }
        m.put("active", true);
        return m;
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return buildCreatePayload(f);
    }
}
