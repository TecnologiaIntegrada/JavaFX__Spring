package com.projetoj.frontend.shared.ui;

import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.http.ApiErrorMessages;
import com.projetoj.frontend.shared.http.ApiException;
import com.projetoj.frontend.shared.security.SessionContext;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import java.util.List;
import java.util.Map;

public class DashboardController {

    private final DependencyContainer container;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label suppliersKpi;
    @FXML
    private Label productsKpi;
    @FXML
    private Label openRequisitionsKpi;
    @FXML
    private Label openRfqsKpi;
    @FXML
    private Label openOrdersKpi;
    @FXML
    private Label pendingApprovalsKpi;
    @FXML
    private PieChart ordersStatusChart;
    @FXML
    private PieChart requisitionsStatusChart;
    @FXML
    private BarChart<String, Number> monthlyOrdersChart;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label messageLabel;

    public DashboardController(DependencyContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        SessionContext session = container.getSessionContext();
        welcomeLabel.setText("Bem-vindo, " + session.getFullName() + ". Indicadores operacionais do modulo de Compras.");
        loadingIndicator.setVisible(true);
        loadDashboard();
    }

    private void loadDashboard() {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() {
                return container.getPurchasingApiAdapter().get("/purchasing/dashboard");
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            bindDashboard(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable err = task.getException();
            messageLabel.setText(err instanceof ApiException ae ? ApiErrorMessages.toUserMessage(ae) : err.getMessage());
        }));
        Thread t = new Thread(task, "dashboard-load");
        t.setDaemon(true);
        t.start();
    }

    @SuppressWarnings("unchecked")
    private void bindDashboard(Map<String, Object> data) {
        if (data == null) {
            messageLabel.setText("Nenhum dado retornado pelo dashboard.");
            return;
        }
        Map<String, Object> totals = (Map<String, Object>) data.get("totals");
        if (totals != null) {
            suppliersKpi.setText(String.valueOf(totals.get("suppliers")));
            productsKpi.setText(String.valueOf(totals.get("products")));
            openRequisitionsKpi.setText(String.valueOf(totals.get("openRequisitions")));
            openRfqsKpi.setText(String.valueOf(totals.get("openRfqs")));
            openOrdersKpi.setText(String.valueOf(totals.get("openPurchaseOrders")));
            pendingApprovalsKpi.setText(String.valueOf(totals.get("pendingApprovals")));
        }

        ordersStatusChart.setData(buildPieData((List<Map<String, Object>>) data.get("purchaseOrdersByStatus")));
        requisitionsStatusChart.setData(buildPieData((List<Map<String, Object>>) data.get("requisitionsByStatus")));

        monthlyOrdersChart.getData().clear();
        XYChart.Series<String, Number> countSeries = new XYChart.Series<>();
        countSeries.setName("Quantidade de pedidos");
        List<Map<String, Object>> monthly = (List<Map<String, Object>>) data.get("monthlyPurchaseOrders");
        if (monthly != null) {
            for (Map<String, Object> row : monthly) {
                String month = String.valueOf(row.get("month"));
                Number count = row.get("count") instanceof Number n ? n : 0;
                countSeries.getData().add(new XYChart.Data<>(month, count));
            }
        }
        monthlyOrdersChart.getData().add(countSeries);
        messageLabel.setText("Dashboard atualizado.");
    }

    private javafx.collections.ObservableList<PieChart.Data> buildPieData(List<Map<String, Object>> rows) {
        javafx.collections.ObservableList<PieChart.Data> slices = javafx.collections.FXCollections.observableArrayList();
        if (rows == null) {
            return slices;
        }
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            long count = row.get("count") instanceof Number n ? n.longValue() : 0L;
            if (count > 0) {
                slices.add(new PieChart.Data(status + " (" + count + ")", count));
            }
        }
        return slices;
    }
}
