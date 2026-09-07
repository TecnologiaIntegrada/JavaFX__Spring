package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.purchasing.ui.form.LookupOption;
import com.projetoj.frontend.shared.di.DependencyContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class SupplierListController extends AbstractPurchasingListController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ObservableList<Map<String, String>> emailRows = FXCollections.observableArrayList();
    private List<Map<String, Object>> pendingContacts = List.of();

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
                FormFieldSpec.staticCombo("personType", "Tipo pessoa", List.of(
                        new LookupOption("J", "J - Jurídico"),
                        new LookupOption("F", "F - Físico")
                ), "J"),
                FormFieldSpec.text("taxId", "CNPJ/CPF"),
                FormFieldSpec.combo("supplierType", "Tipo fornecedor", "/tipos-fornecedor", "code", "lookupLabel", "DISTRIBUIDOR"),
                FormFieldSpec.combo("status", "Status", "/status-fornecedor", "code", "lookupLabel", "ATIVO"),
                FormFieldSpec.text("city", "Cidade"),
                FormFieldSpec.text("stateCode", "UF"),
                FormFieldSpec.combo("defaultCurrency", "Moeda padrão", "/moedas", "code", "currencyLabel", "BRL")
        );
    }

    @Override
    protected Node extraFormContent(Map<String, Object> current) {
        emailRows.clear();
        if (current != null && current.get("contacts") instanceof List<?> contacts) {
            for (Object contactObj : contacts) {
                if (!(contactObj instanceof Map<?, ?> raw)) {
                    continue;
                }
                String email = api.asString(raw.get("email")).trim();
                if (email.isBlank()) {
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                row.put("email", email);
                row.put("name", blank(api.asString(raw.get("name")), email));
                emailRows.add(row);
            }
        }

        TableView<Map<String, String>> grid = new TableView<>(emailRows);
        grid.setPrefHeight(160);
        TableColumn<Map<String, String>, String> emailCol = new TableColumn<>("E-mail");
        emailCol.setPrefWidth(360);
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrDefault("email", "")));
        grid.getColumns().add(emailCol);

        TextField emailField = new TextField();
        emailField.setPromptText("contato@fornecedor.com");
        HBox.setHgrow(emailField, Priority.ALWAYS);

        Button add = new Button("Adicionar");
        add.setOnAction(e -> {
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            if (!isValidEmail(email)) {
                messageLabel.setText("Informe um e-mail válido.");
                return;
            }
            boolean exists = emailRows.stream()
                    .anyMatch(row -> email.equalsIgnoreCase(row.getOrDefault("email", "")));
            if (exists) {
                messageLabel.setText("Este e-mail já está na lista.");
                return;
            }
            Map<String, String> row = new HashMap<>();
            row.put("email", email);
            row.put("name", email);
            emailRows.add(row);
            emailField.clear();
        });

        Button remove = new Button("Remover");
        remove.setOnAction(e -> {
            Map<String, String> selected = grid.getSelectionModel().getSelectedItem();
            if (selected != null) {
                emailRows.remove(selected);
            }
        });

        HBox actions = new HBox(8, emailField, add, remove);
        VBox box = new VBox(8, new Label("E-mails de contato"), actions, grid);
        box.setPadding(new Insets(4, 0, 0, 0));
        return box;
    }

    @Override
    protected void collectExtraForm(Map<String, String> values) {
        List<Map<String, Object>> contacts = new ArrayList<>();
        boolean primary = true;
        for (Map<String, String> row : emailRows) {
            String email = blank(row.get("email"), "").trim();
            if (email.isBlank()) {
                continue;
            }
            Map<String, Object> contact = new HashMap<>();
            contact.put("name", blank(row.get("name"), email));
            contact.put("email", email);
            contact.put("isPrimary", primary);
            contact.put("active", true);
            contacts.add(contact);
            primary = false;
        }
        pendingContacts = contacts;
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        Map<String, Object> m = new HashMap<>();
        m.put("legalName", f.get("legalName"));
        m.put("tradeName", f.get("tradeName"));
        m.put("personType", blank(f.get("personType"), "J").toUpperCase(Locale.ROOT));
        m.put("taxId", f.get("taxId"));
        m.put("supplierType", blank(f.get("supplierType"), "DISTRIBUIDOR"));
        m.put("status", blank(f.get("status"), "ATIVO"));
        m.put("city", f.get("city"));
        m.put("stateCode", f.get("stateCode"));
        m.put("defaultCurrency", blank(f.get("defaultCurrency"), "BRL"));
        m.put("active", true);
        m.put("contacts", pendingContacts == null ? List.of() : pendingContacts);
        return m;
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        return buildCreatePayload(f);
    }

    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
