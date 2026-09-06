package com.projetoj.frontend.shared.ui;

import com.projetoj.frontend.shared.di.DependencyContainer;
import com.projetoj.frontend.shared.security.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;

public class ShellController {

    private final DependencyContainer container;
    private final SceneNavigator navigator;

    @FXML private StackPane contentArea;
    @FXML private Label headerTitleLabel;
    @FXML private Label statusBarLabel;
    @FXML private Label sessionLabel;
    @FXML private TitledPane principalGroup;
    @FXML private TitledPane adminGroup;
    @FXML private TitledPane purchasingMasterGroup;
    @FXML private TitledPane purchasingOpsGroup;
    @FXML private TitledPane systemGroup;
    @FXML private Button dashboardButton;
    @FXML private Button usersButton;
    @FXML private Button rolesButton;
    @FXML private Button permissionsButton;
    @FXML private Button modulesButton;
    @FXML private Button productsButton;
    @FXML private Button suppliersButton;
    @FXML private Button requisitionsButton;
    @FXML private Button approvalsButton;
    @FXML private Button rfqsButton;
    @FXML private Button quotesButton;
    @FXML private Button awardsButton;
    @FXML private Button purchaseOrdersButton;
    @FXML private Button goodsReceiptsButton;
    @FXML private Button apiLogsButton;
    @FXML private Button connectivityButton;
    @FXML private Button logoutButton;

    public ShellController(DependencyContainer container, SceneNavigator navigator) {
        this.container = container;
        this.navigator = navigator;
    }

    @FXML
    public void initialize() {
        SessionContext session = container.getSessionContext();
        if (!session.isAuthenticated()) {
            navigator.showLogin();
            return;
        }

        sessionLabel.setText(session.getUsername() + " | Perfil: " + session.getRoleName());
        statusBarLabel.setText("API: " + container.getAppConfig().getApiBaseUrl());

        bindView(usersButton, "USERS");
        bindView(rolesButton, "ROLES");
        bindView(permissionsButton, "PERMISSIONS");
        bindView(modulesButton, "MODULES");
        bindView(productsButton, "PRODUCTS");
        bindView(suppliersButton, "SUPPLIERS");
        bindView(requisitionsButton, "PURCHASE_REQUISITIONS");
        bindView(approvalsButton, "REQUISITION_APPROVALS");
        bindView(rfqsButton, "RFQS");
        bindView(quotesButton, "SUPPLIER_QUOTES");
        bindView(awardsButton, "QUOTE_AWARDS");
        bindView(purchaseOrdersButton, "PURCHASE_ORDERS");
        bindView(goodsReceiptsButton, "GOODS_RECEIPTS");
        bindView(apiLogsButton, "API_LOGS");
        bindView(connectivityButton, "SYSTEM");

        bindGroupVisibility(adminGroup, usersButton, rolesButton, permissionsButton, modulesButton);
        bindGroupVisibility(purchasingMasterGroup, productsButton, suppliersButton);
        bindGroupVisibility(purchasingOpsGroup, requisitionsButton, approvalsButton, rfqsButton, quotesButton, awardsButton, purchaseOrdersButton, goodsReceiptsButton);
        bindGroupVisibility(systemGroup, apiLogsButton, connectivityButton);

        openDashboard();
    }

    private void bindGroupVisibility(TitledPane group, Button... buttons) {
        if (group == null) {
            return;
        }
        boolean anyVisible = false;
        for (Button button : buttons) {
            if (button.isVisible()) {
                anyVisible = true;
                break;
            }
        }
        group.setVisible(anyVisible);
        group.setManaged(anyVisible);
    }

    private void bindView(Button button, String module) {
        boolean visible = container.getSessionContext().canView(module);
        button.setVisible(visible);
        button.setManaged(visible);
    }

    @FXML public void openDashboard() { setActive(dashboardButton); showContent("/fxml/dashboard.fxml", "Dashboard Compras"); }
    @FXML public void openUsers() { openIfCan("USERS", usersButton, "/fxml/users/user-list.fxml", "Usuários"); }
    @FXML public void openRoles() { openIfCan("ROLES", rolesButton, "/fxml/roles/role-list.fxml", "Perfis"); }
    @FXML public void openPermissions() { openIfCan("PERMISSIONS", permissionsButton, "/fxml/permissions/permission-list.fxml", "Permissões"); }
    @FXML public void openModules() { openIfCan("MODULES", modulesButton, "/fxml/modules/module-list.fxml", "Módulos"); }
    @FXML public void openProducts() { openIfCan("PRODUCTS", productsButton, "/fxml/purchasing/product-list.fxml", "Produtos"); }
    @FXML public void openSuppliers() { openIfCan("SUPPLIERS", suppliersButton, "/fxml/purchasing/supplier-list.fxml", "Fornecedores"); }
    @FXML public void openRequisitions() { openIfCan("PURCHASE_REQUISITIONS", requisitionsButton, "/fxml/purchasing/requisition-list.fxml", "Req. compras"); }
    @FXML public void openApprovals() { openIfCan("REQUISITION_APPROVALS", approvalsButton, "/fxml/purchasing/approval-list.fxml", "Aprovar RC"); }
    @FXML public void openRfqs() { openIfCan("RFQS", rfqsButton, "/fxml/purchasing/rfq-list.fxml", "Solicitar cotação (RFQ)"); }
    @FXML public void openQuotes() { openIfCan("SUPPLIER_QUOTES", quotesButton, "/fxml/purchasing/quote-list.fxml", "Propostas"); }
    @FXML public void openAwards() { openIfCan("QUOTE_AWARDS", awardsButton, "/fxml/purchasing/award-list.fxml", "Mapa Comparativo"); }
    @FXML public void openPurchaseOrders() { openIfCan("PURCHASE_ORDERS", purchaseOrdersButton, "/fxml/purchasing/purchase-order-list.fxml", "Pedidos de Compra"); }
    @FXML public void openGoodsReceipts() { openIfCan("GOODS_RECEIPTS", goodsReceiptsButton, "/fxml/purchasing/goods-receipt-list.fxml", "Recebimentos"); }
    @FXML public void openApiLogs() { openIfCan("API_LOGS", apiLogsButton, "/fxml/purchasing/api-log-list.fxml", "Log de API"); }
    @FXML public void openConnectivity() { openIfCan("SYSTEM", connectivityButton, "/fxml/connectivity.fxml", "Conectividade"); }

    private void openIfCan(String module, Button button, String fxml, String title) {
        if (!container.getSessionContext().canView(module)) {
            return;
        }
        setActive(button);
        showContent(fxml, title);
    }

    @FXML
    public void onLogout() {
        container.getApiClient().clearAuthToken();
        container.getSessionContext().clear();
        navigator.showLogin();
    }

    private void showContent(String fxmlPath, String title) {
        headerTitleLabel.setText(title);
        contentArea.getChildren().setAll(navigator.loadView(fxmlPath));
    }

    private void setActive(Button active) {
        Button[] all = {
                dashboardButton, usersButton, rolesButton, permissionsButton, modulesButton,
                productsButton, suppliersButton,
                requisitionsButton, approvalsButton, rfqsButton, quotesButton, awardsButton,
                purchaseOrdersButton, goodsReceiptsButton, apiLogsButton, connectivityButton
        };
        for (Button button : all) {
            button.getStyleClass().remove("nav-button-active");
        }
        if (active != null && !active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }
}
