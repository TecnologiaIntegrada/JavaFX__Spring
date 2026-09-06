package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.shared.di.DependencyContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupplierListController extends AbstractPurchasingListController {

    public SupplierListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "SUPPLIERS";
    }

    @Override
    protected String apiPath() {
        return "/suppliers";
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"taxId", "legalName", "tradeName", "city", "stateCode", "supplierType", "status"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"CNPJ/CPF", "Razão Social", "Fantasia", "Cidade", "UF", "Tipo", "Status"};
    }

    @Override
    protected List<FormFieldSpec> formFieldSpecs() {
        return List.of(
                FormFieldSpec.text("legalName", "Razão social"),
                FormFieldSpec.text("tradeName", "Nome fantasia"),
                FormFieldSpec.text("personType", "Tipo pessoa (J/F)"),
                FormFieldSpec.text("taxId", "CNPJ/CPF"),
                FormFieldSpec.text("supplierType", "Tipo fornecedor"),
                FormFieldSpec.text("status", "Status"),
                FormFieldSpec.text("city", "Cidade"),
                FormFieldSpec.text("stateCode", "UF"),
                FormFieldSpec.combo("defaultCurrency", "Moeda padrão", "/moedas", "code", "currencyLabel", "BRL")
        );
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        Map<String, Object> m = new HashMap<>();
        m.put("legalName", f.get("legalName"));
        m.put("tradeName", f.get("tradeName"));
        m.put("personType", blank(f.get("personType"), "J"));
        m.put("taxId", f.get("taxId"));
        m.put("supplierType", blank(f.get("supplierType"), "DISTRIBUIDOR"));
        m.put("status", blank(f.get("status"), "ATIVO"));
        m.put("city", f.get("city"));
        m.put("stateCode", f.get("stateCode"));
        m.put("defaultCurrency", blank(f.get("defaultCurrency"), "BRL"));
        m.put("active", true);
        return m;
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return buildCreatePayload(f);
    }
}
