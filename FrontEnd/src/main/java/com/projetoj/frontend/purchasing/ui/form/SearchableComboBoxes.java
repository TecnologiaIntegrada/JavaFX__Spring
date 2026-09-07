package com.projetoj.frontend.purchasing.ui.form;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Locale;

/**
 * ComboBox pesquisavel por texto (ID + descricao).
 */
public final class SearchableComboBoxes {

    private SearchableComboBoxes() {
    }

    public static ComboBox<LookupOption> create(List<LookupOption> options) {
        ComboBox<LookupOption> combo = new ComboBox<>();
        configure(combo, options);
        return combo;
    }

    public static void configure(ComboBox<LookupOption> combo, List<LookupOption> options) {
        ObservableList<LookupOption> source = FXCollections.observableArrayList(options == null ? List.of() : options);
        FilteredList<LookupOption> filtered = new FilteredList<>(source, item -> true);
        combo.setItems(filtered);
        combo.setEditable(true);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LookupOption option) {
                return option == null ? "" : option.toString();
            }

            @Override
            public LookupOption fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return source.stream()
                        .filter(option -> option.toString().equalsIgnoreCase(text.trim())
                                || option.id().equalsIgnoreCase(text.trim()))
                        .findFirst()
                        .orElse(null);
            }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LookupOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        combo.getEditor().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB
                    || event.getCode() == KeyCode.ESCAPE || event.getCode().isArrowKey()) {
                return;
            }
            String filter = combo.getEditor().getText();
            filtered.setPredicate(option -> {
                if (filter == null || filter.isBlank()) {
                    return true;
                }
                String needle = filter.toLowerCase(Locale.ROOT);
                return option.toString().toLowerCase(Locale.ROOT).contains(needle)
                        || option.id().toLowerCase(Locale.ROOT).contains(needle);
            });
            if (!combo.isShowing()) {
                combo.show();
            }
        });
        combo.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                LookupOption selected = combo.getConverter().fromString(combo.getEditor().getText());
                if (selected != null) {
                    combo.setValue(selected);
                }
            }
        });
    }

    public static void selectById(ComboBox<LookupOption> combo, String id) {
        if (combo == null || id == null || id.isBlank()) {
            return;
        }
        combo.getItems().stream()
                .filter(option -> option.id().equalsIgnoreCase(id))
                .findFirst()
                .ifPresent(option -> {
                    combo.setValue(option);
                    combo.getEditor().setText(option.toString());
                });
    }
}
