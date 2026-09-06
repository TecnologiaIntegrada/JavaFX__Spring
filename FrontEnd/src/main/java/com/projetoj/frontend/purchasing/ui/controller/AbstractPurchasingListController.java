package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.adapter.rest.PurchasingApiAdapter;
import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.purchasing.ui.form.FormFieldType;
import com.projetoj.frontend.purchasing.ui.form.LookupOption;
import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.projetoj.frontend.purchasing.ui.form.SearchableComboBoxes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lista generica de entidades de Compras/Log baseada em Map JSON da API.
 */
public abstract class AbstractPurchasingListController {

    protected final DependencyContainer container;
    protected final PurchasingApiAdapter api;

    @FXML
    protected VBox rootPane;
    @FXML
    protected TableView<Map<String, Object>> table;
    @FXML
    protected Label messageLabel;
    @FXML
    protected ProgressIndicator loadingIndicator;
    @FXML
    protected Button createButton;
    @FXML
    protected Button editButton;
    @FXML
    protected Button deleteButton;
    @FXML
    protected HBox filterBar;
    @FXML
    protected Label filterLabel;
    @FXML
    protected ComboBox<LookupOption> filterCombo;

    protected AbstractPurchasingListController(DependencyContainer container) {
        this.container = container;
        this.api = container.getPurchasingApiAdapter();
    }

    protected abstract String moduleCode();

    protected abstract String apiPath();

    protected abstract String[] columnKeys();

    protected abstract String[] columnTitles();

    protected abstract Map<String, Object> buildCreatePayload(Map<String, String> formValues);

    protected abstract Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> formValues);

    protected String[] formFields() {
        return new String[0];
    }

    protected List<FormFieldSpec> formFieldSpecs() {
        return Arrays.stream(formFields()).map(f -> FormFieldSpec.text(f, f)).toList();
    }

    protected boolean showAuditColumns() {
        return true;
    }

    protected boolean hasParentFilter() {
        return false;
    }

    protected boolean filterRequiredForList() {
        return false;
    }

    protected String filterLabelText() {
        return "Filtrar";
    }

    protected String filterApiPath() {
        return "/suppliers";
    }

    protected String filterQueryParam() {
        return "supplierId";
    }

    protected String filterComboIdField() {
        return "id";
    }

    protected String filterComboLabelField() {
        return "legalName";
    }

    protected void afterInitialize() {
    }

    @FXML
    public void initialize() {
        setupFilterBar();
        setupColumns();
        loadingIndicator.setVisible(false);
        boolean canCreate = container.getSessionContext().canCreate(moduleCode());
        boolean canUpdate = container.getSessionContext().canUpdate(moduleCode());
        boolean canDelete = container.getSessionContext().canDelete(moduleCode());
        createButton.setDisable(!canCreate);
        createButton.setVisible(canCreate);
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            editButton.setDisable(n == null || !canUpdate);
            deleteButton.setDisable(n == null || !canDelete);
        });
        afterInitialize();
        if (!filterRequiredForList() || (filterCombo != null && filterCombo.getValue() != null)) {
            onRefresh();
        } else if (filterRequiredForList()) {
            messageLabel.setText("Selecione um filtro para listar os registros.");
        }
    }

    private void setupFilterBar() {
        if (!hasParentFilter() || filterBar == null) {
            if (filterBar != null) {
                filterBar.setManaged(false);
                filterBar.setVisible(false);
            }
            return;
        }
        filterBar.setManaged(true);
        filterBar.setVisible(true);
        if (filterLabel != null) {
            filterLabel.setText(filterLabelText() + ":");
        }
        if (filterCombo != null) {
            filterCombo.setOnAction(e -> onRefresh());
            loadFilterOptions();
        }
    }

    private void loadFilterOptions() {
        if (filterCombo == null) {
            return;
        }
        Task<List<LookupOption>> task = new Task<>() {
            @Override
            protected List<LookupOption> call() {
                return loadLookupOptions(filterApiPath(), filterComboIdField(), filterComboLabelField());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> filterCombo.setItems(FXCollections.observableArrayList(task.getValue()))));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        startTask(task, "purchasing-filter");
    }

    private void setupColumns() {
        List<String> keys = new ArrayList<>(Arrays.asList(columnKeys()));
        List<String> titles = new ArrayList<>(Arrays.asList(columnTitles()));
        if (showAuditColumns()) {
            keys.add("createdByName");
            keys.add("updatedByName");
            titles.add("Criado por");
            titles.add("Atualizado por");
        }
        table.getColumns().clear();
        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(titles.get(i));
            col.setPrefWidth(key.endsWith("Name") ? 160 : 140);
            col.setCellValueFactory(data -> new SimpleStringProperty(api.asString(data.getValue().get(key))));
            table.getColumns().add(col);
        }
    }

    protected String resolveListPath() {
        if (hasParentFilter() && filterCombo != null && filterCombo.getValue() != null) {
            return apiPath() + "?" + filterQueryParam() + "=" + filterCombo.getValue().id();
        }
        return apiPath();
    }

    @FXML
    public void onRefresh() {
        if (filterRequiredForList() && (filterCombo == null || filterCombo.getValue() == null)) {
            table.getItems().clear();
            messageLabel.setText("Selecione um filtro para listar os registros.");
            return;
        }
        loadingIndicator.setVisible(true);
        messageLabel.setText("Carregando...");
        String path = resolveListPath();
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() {
                return api.list(path);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Map<String, Object>> rows = task.getValue();
            table.setItems(FXCollections.observableArrayList(rows));
            messageLabel.setText(rows.size() + " registro(s).");
            loadingIndicator.setVisible(false);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        startTask(task, "purchasing-list");
    }

    @FXML
    public void onCreate() {
        if (filterRequiredForList() && (filterCombo == null || filterCombo.getValue() == null)) {
            messageLabel.setText("Selecione o registro pai antes de incluir.");
            return;
        }
        promptFormAsync("Novo registro", null, values -> {
            if (values == null) {
                return;
            }
            runWrite(() -> api.post(apiPath(), buildCreatePayload(values)), "Registro criado.");
        });
    }

    @FXML
    public void onEdit() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        promptFormAsync("Editar registro", selected, values -> {
            if (values == null) {
                return;
            }
            Object id = selected.get("id");
            runWrite(() -> api.put(apiPath() + "/" + id, buildUpdatePayload(selected, values)), "Registro atualizado.");
        });
    }

    @FXML
    public void onDelete() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        runWrite(() -> {
            api.delete(apiPath() + "/" + selected.get("id"));
            return null;
        }, "Registro excluido.");
    }

    protected void runWrite(WriteAction action, String successMessage) {
        loadingIndicator.setVisible(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                action.run();
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            messageLabel.setText(successMessage);
            loadingIndicator.setVisible(false);
            onRefresh();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        startTask(task, "purchasing-write");
    }

    protected void promptFormAsync(String title, Map<String, Object> current, java.util.function.Consumer<Map<String, String>> onResult) {
        loadingIndicator.setVisible(true);
        Task<Map<String, List<LookupOption>>> task = new Task<>() {
            @Override
            protected Map<String, List<LookupOption>> call() {
                Map<String, List<LookupOption>> comboOptions = new LinkedHashMap<>();
                for (FormFieldSpec spec : formFieldSpecs()) {
                    if (spec.type() == FormFieldType.COMBO) {
                        comboOptions.put(spec.key(), loadLookupOptions(
                                resolveComboPath(spec),
                                spec.comboIdField(),
                                spec.comboLabelField()
                        ));
                    }
                }
                return comboOptions;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            onResult.accept(showFormDialog(title, current, task.getValue()));
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
            onResult.accept(null);
        }));
        startTask(task, "purchasing-form-load");
    }

    protected Map<String, String> promptForm(String title, Map<String, Object> current) {
        try {
            Map<String, List<LookupOption>> comboOptions = new LinkedHashMap<>();
            for (FormFieldSpec spec : formFieldSpecs()) {
                if (spec.type() == FormFieldType.COMBO) {
                    comboOptions.put(spec.key(), loadLookupOptions(
                            resolveComboPath(spec),
                            spec.comboIdField(),
                            spec.comboLabelField()
                    ));
                }
            }
            return showFormDialog(title, current, comboOptions);
        } catch (Exception ex) {
            messageLabel.setText(ex instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : ex.getMessage());
            return null;
        }
    }

    private Map<String, String> showFormDialog(String title, Map<String, Object> current, Map<String, List<LookupOption>> comboOptions) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        Map<String, TextField> textFields = new LinkedHashMap<>();
        Map<String, TextArea> memoFields = new LinkedHashMap<>();
        Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();
        Map<String, ComboBox<LookupOption>> comboBoxes = new LinkedHashMap<>();
        Map<String, DatePicker> datePickers = new LinkedHashMap<>();
        int row = 0;
        for (FormFieldSpec spec : formFieldSpecs()) {
            grid.add(new Label(spec.label()), 0, row);
            switch (spec.type()) {
                case COMBO -> {
                    ComboBox<LookupOption> combo = SearchableComboBoxes.create(
                            comboOptions.getOrDefault(spec.key(), List.of())
                    );
                    if (current != null && current.get(spec.key()) != null) {
                        SearchableComboBoxes.selectById(combo, String.valueOf(current.get(spec.key())));
                    } else if (spec.defaultValue() != null) {
                        SearchableComboBoxes.selectById(combo, spec.defaultValue());
                    }
                    comboBoxes.put(spec.key(), combo);
                    grid.add(combo, 1, row);
                }
                case BOOLEAN -> {
                    CheckBox checkBox = new CheckBox();
                    if (current != null && current.get(spec.key()) != null) {
                        checkBox.setSelected(Boolean.parseBoolean(String.valueOf(current.get(spec.key()))));
                    }
                    checkBoxes.put(spec.key(), checkBox);
                    grid.add(checkBox, 1, row);
                }
                case DATE -> {
                    DatePicker picker = new DatePicker();
                    if (current != null && current.get(spec.key()) != null) {
                        String raw = String.valueOf(current.get(spec.key()));
                        if (raw.length() >= 10) {
                            picker.setValue(LocalDate.parse(raw.substring(0, 10)));
                        }
                    }
                    datePickers.put(spec.key(), picker);
                    grid.add(picker, 1, row);
                }
                case MEMO -> {
                    TextArea area = new TextArea();
                    area.setPrefRowCount(4);
                    area.setWrapText(true);
                    if (current != null && current.get(spec.key()) != null) {
                        area.setText(String.valueOf(current.get(spec.key())));
                    }
                    memoFields.put(spec.key(), area);
                    grid.add(area, 1, row);
                }
                case NUMBER, TEXT -> {
                    TextField input = new TextField();
                    if (current != null && current.get(spec.key()) != null) {
                        input.setText(String.valueOf(current.get(spec.key())));
                    }
                    textFields.put(spec.key(), input);
                    grid.add(input, 1, row);
                }
            }
            row++;
        }
        ColumnConstraints(grid);
        javafx.scene.control.Dialog<Map<String, String>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );
        dialog.setResultConverter(bt -> {
            if (bt != javafx.scene.control.ButtonType.OK) {
                return null;
            }
            Map<String, String> result = new LinkedHashMap<>();
            textFields.forEach((k, v) -> result.put(k, v.getText() == null ? "" : v.getText().trim()));
            memoFields.forEach((k, v) -> result.put(k, v.getText() == null ? "" : v.getText().trim()));
            checkBoxes.forEach((k, v) -> result.put(k, String.valueOf(v.isSelected())));
            datePickers.forEach((k, v) -> result.put(k, v.getValue() == null ? "" : v.getValue().toString()));
            comboBoxes.forEach((k, v) -> {
                LookupOption selected = v.getValue();
                if (selected == null) {
                    selected = v.getConverter().fromString(v.getEditor().getText());
                }
                result.put(k, selected == null ? "" : selected.id());
            });
            return result;
        });
        return dialog.showAndWait().orElse(null);
    }

    private static void ColumnConstraints(GridPane grid) {
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setMinWidth(120);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);
    }

    protected String resolveComboPath(FormFieldSpec spec) {
        String path = spec.comboApiPath();
        if (path == null) {
            return "";
        }
        if (filterCombo != null && filterCombo.getValue() != null) {
            path = path.replace("{supplierId}", filterCombo.getValue().id());
            path = path.replace("{productId}", filterCombo.getValue().id());
        }
        return path;
    }

    protected List<LookupOption> loadLookupOptions(String path, String idField, String labelField) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return api.list(path).stream()
                .map(row -> new LookupOption(
                        api.asString(row.get(idField)),
                        buildLabel(row, labelField)
                ))
                .collect(Collectors.toList());
    }

    protected String buildLabel(Map<String, Object> row, String labelField) {
        if ("supplierProductLabel".equals(labelField)) {
            String code = api.asString(row.get("productCode"));
            String desc = api.asString(row.get("productDescription"));
            return (code + " - " + desc).trim();
        }
        if ("supplierLabel".equals(labelField)) {
            String legal = api.asString(row.get("legalName"));
            String trade = api.asString(row.get("tradeName"));
            return trade.isBlank() ? legal : trade + " (" + legal + ")";
        }
        if ("currencyLabel".equals(labelField)) {
            String code = api.asString(row.get("code"));
            String name = api.asString(row.get("name"));
            return code + " - " + name;
        }
        if ("rfqItemLabel".equals(labelField)) {
            String line = api.asString(row.get("lineNumber"));
            String code = api.asString(row.get("productCode"));
            String desc = api.asString(row.get("productDescription"));
            if (desc.isBlank()) {
                desc = api.asString(row.get("description"));
            }
            return "#" + line + " " + code + " - " + desc;
        }
        if ("productLabel".equals(labelField)) {
            String code = api.asString(row.get("code"));
            String desc = api.asString(row.get("description"));
            return code.isBlank() ? desc : code + " - " + desc;
        }
        if ("requisitionLabel".equals(labelField)) {
            String number = api.asString(row.get("requisitionNumber"));
            String status = api.asString(row.get("status"));
            return status.isBlank() ? number : number + " (" + status + ")";
        }
        return api.asString(row.get(labelField));
    }

    private void selectComboValue(ComboBox<LookupOption> combo, Map<String, Object> current, String key) {
        if (current == null || current.get(key) == null) {
            return;
        }
        SearchableComboBoxes.selectById(combo, String.valueOf(current.get(key)));
    }

    protected LookupOption selectedFilter() {
        return filterCombo == null ? null : filterCombo.getValue();
    }

    protected static String blank(String v, String d) {
        return v == null || v.isBlank() ? d : v;
    }

    protected static String emptyToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private void startTask(Task<?> task, String name) {
        Thread t = new Thread(task, name);
        t.setDaemon(true);
        t.start();
    }

    @FunctionalInterface
    protected interface WriteAction {
        Object run();
    }
}
