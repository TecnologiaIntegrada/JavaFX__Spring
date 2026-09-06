package com.projetoj.frontend.purchasing.ui.form;

public record LookupOption(String id, String label) {
    @Override
    public String toString() {
        return label == null || label.isBlank() ? id : label;
    }
}
