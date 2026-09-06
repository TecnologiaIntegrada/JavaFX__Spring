package com.projetoj.frontend.purchasing.ui.controller;

import com.projetoj.frontend.purchasing.ui.form.FormFieldSpec;
import com.projetoj.frontend.purchasing.ui.form.LookupOption;
import com.projetoj.frontend.shared.di.DependencyContainer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalListController extends AbstractPurchasingListController {

    public ApprovalListController(DependencyContainer container) {
        super(container);
    }

    @Override
    protected String moduleCode() {
        return "REQUISITION_APPROVALS";
    }

    @Override
    protected String apiPath() {
        return "/requisition-approvals/pending";
    }

    @Override
    protected boolean showAuditColumns() {
        return false;
    }

    @Override
    protected String[] columnKeys() {
        return new String[]{"requisitionNumber", "approvalLevel", "status", "comments"};
    }

    @Override
    protected String[] columnTitles() {
        return new String[]{"RC", "Nível", "Status", "Comentários"};
    }

    @Override
    protected List<FormFieldSpec> formFieldSpecs() {
        return List.of(
                FormFieldSpec.memo("comments", "Comentários")
        );
    }

    @Override
    protected Map<String, Object> buildCreatePayload(Map<String, String> f) {
        return Map.of();
    }

    @Override
    protected Map<String, Object> buildUpdatePayload(Map<String, Object> selected, Map<String, String> f) {
        Map<String, Object> body = new HashMap<>();
        body.put("comments", f.get("comments"));
        return body;
    }

    @Override
    protected void afterInitialize() {
        createButton.setVisible(false);
        deleteButton.setVisible(false);
        editButton.setText("Editar comentários");
        editButton.setOnAction(e -> onEditComments());

        javafx.scene.control.Button approveButton = new javafx.scene.control.Button("APROVAR");
        approveButton.getStyleClass().add("primary-button");
        approveButton.setDisable(true);
        approveButton.setOnAction(e -> onApprove());

        if (editButton.getParent() instanceof javafx.scene.layout.HBox toolbar) {
            toolbar.getChildren().add(toolbar.getChildren().indexOf(editButton) + 1, approveButton);
        }

        boolean canUpdate = container.getSessionContext().canUpdate(moduleCode());
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean pending = n != null && "PENDING".equalsIgnoreCase(api.asString(n.get("status")));
            editButton.setDisable(n == null || !canUpdate || !pending);
            approveButton.setDisable(n == null || !canUpdate || !pending);
        });
    }

    private void onEditComments() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !"PENDING".equalsIgnoreCase(api.asString(selected.get("status")))) {
            messageLabel.setText("Comentários só podem ser editados com status PENDING.");
            return;
        }
        promptFormAsync("Editar comentários", selected, values -> {
            if (values == null) {
                return;
            }
            runWrite(
                    () -> api.put("/requisition-approvals/" + selected.get("id") + "/comments",
                            Map.of("comments", values.getOrDefault("comments", ""))),
                    "Comentários atualizados."
            );
        });
    }

    private void onApprove() {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Aprovar requisição");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        ComboBox<LookupOption> decisionCombo = new ComboBox<>(FXCollections.observableArrayList(
                new LookupOption("APPROVED", "Aprovado"),
                new LookupOption("REJECTED", "Rejeitado"),
                new LookupOption("RETURNED", "Devolvido")
        ));
        decisionCombo.setValue(decisionCombo.getItems().get(0));
        decisionCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea comments = new TextArea(api.asString(selected.get("comments")));
        comments.setPrefRowCount(5);
        comments.setWrapText(true);
        GridPane.setHgrow(comments, Priority.ALWAYS);

        grid.add(new Label("Decisão"), 0, 0);
        grid.add(decisionCombo, 1, 0);
        grid.add(new Label("Comentários"), 0, 1);
        grid.add(comments, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) {
                return null;
            }
            Map<String, String> result = new HashMap<>();
            result.put("decision", decisionCombo.getValue() == null ? "APPROVED" : decisionCombo.getValue().id());
            result.put("comments", comments.getText() == null ? "" : comments.getText().trim());
            return result;
        });

        dialog.showAndWait().ifPresent(values -> {
            Map<String, Object> body = new HashMap<>();
            body.put("decision", values.get("decision"));
            body.put("comments", values.get("comments"));
            runWrite(() -> api.post("/requisition-approvals/" + selected.get("id") + "/decide", body), "Decisão registrada.");
        });
    }
}
