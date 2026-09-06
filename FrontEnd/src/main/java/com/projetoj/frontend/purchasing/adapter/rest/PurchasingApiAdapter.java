package com.projetoj.frontend.purchasing.adapter.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.projetoj.frontend.shared.http.ApiClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente REST consolidado do modulo de Compras e Log de API.
 */
public class PurchasingApiAdapter {

    private final ApiClient apiClient;

    public PurchasingApiAdapter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<Map<String, Object>> list(String path) {
        return apiClient.getList(path, new TypeReference<>() {
        });
    }

    public Map<String, Object> get(String path) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = apiClient.get(path, Map.class);
        return result;
    }

    public Map<String, Object> post(String path, Object body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = apiClient.post(path, body, Map.class);
        return result;
    }

    public Map<String, Object> put(String path, Object body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = apiClient.put(path, body, Map.class);
        return result;
    }

    public void delete(String path) {
        apiClient.delete(path);
    }

    public String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public UUID asUuid(Object value) {
        if (value == null) {
            return null;
        }
        return UUID.fromString(String.valueOf(value));
    }
}
