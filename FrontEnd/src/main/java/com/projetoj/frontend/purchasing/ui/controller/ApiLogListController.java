package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.shared.di.DependencyContainer;

import java.util.Map;

public class ApiLogListController extends AbstractPurchasingListController {

    public ApiLogListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "API_LOGS";
    }

    @Override
    protected String apiPath() {
        return "/api-logs";
    }

    @Override
    protected boolean showAuditColumns() {
        return false;
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"occurredAt", "username", "httpMethod", "requestPath", "responseStatus", "requestBody"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"Data/Hora", "Usuario", "Metodo", "Path", "Status", "Payload"};
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
        createButton.setVisible(false);
        editButton.setVisible(false);
        deleteButton.setVisible(false);
    }
}
