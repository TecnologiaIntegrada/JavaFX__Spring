package com.projetoj.frontend.purchasing.ui.form;

import java.util.List;

public record FormFieldSpec(
        String key,
        String label,
        FormFieldType type,
        String comboApiPath,
        String comboIdField,
        String comboLabelField,
        String defaultValue,
        List<LookupOption> staticOptions
) {
    public FormFieldSpec(String key, String label, FormFieldType type, String comboApiPath, String comboIdField, String comboLabelField) {
        this(key, label, type, comboApiPath, comboIdField, comboLabelField, null, null);
    }

    public static FormFieldSpec text(String key, String label) {
        return new FormFieldSpec(key, label, FormFieldType.TEXT, null, null, null, null, null);
    }

    public static FormFieldSpec number(String key, String label) {
        return new FormFieldSpec(key, label, FormFieldType.NUMBER, null, null, null, null, null);
    }

    public static FormFieldSpec date(String key, String label) {
        return new FormFieldSpec(key, label, FormFieldType.DATE, null, null, null, null, null);
    }

    public static FormFieldSpec bool(String key, String label) {
        return new FormFieldSpec(key, label, FormFieldType.BOOLEAN, null, null, null, null, null);
    }

    public static FormFieldSpec memo(String key, String label) {
        return new FormFieldSpec(key, label, FormFieldType.MEMO, null, null, null, null, null);
    }

    public static FormFieldSpec combo(String key, String label, String apiPath, String idField, String labelField) {
        return new FormFieldSpec(key, label, FormFieldType.COMBO, apiPath, idField, labelField, null, null);
    }

    public static FormFieldSpec combo(String key, String label, String apiPath, String idField, String labelField, String defaultValue) {
        return new FormFieldSpec(key, label, FormFieldType.COMBO, apiPath, idField, labelField, defaultValue, null);
    }

    public static FormFieldSpec staticCombo(String key, String label, List<LookupOption> options, String defaultValue) {
        return new FormFieldSpec(key, label, FormFieldType.COMBO, null, "id", "label", defaultValue, List.copyOf(options));
    }
}
