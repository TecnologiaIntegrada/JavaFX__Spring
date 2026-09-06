package com.projetoj.frontend.shared.ui;

import com.projetoj.frontend.identity.auth.ui.controller.LoginController;
import com.projetoj.frontend.shared.di.DependencyContainer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public final class SceneNavigator {

    private final Stage stage;
    private final DependencyContainer container;
    private final Function<Class<?>, Object> controllerFactory;

    public SceneNavigator(Stage stage, DependencyContainer container) {
        this.stage = stage;
        this.container = container;
        this.controllerFactory = type -> {
            if (type == LoginController.class) {
                return new LoginController(container, this);
            }
            if (type == ConnectivityController.class) {
                return new ConnectivityController(container);
            }
            if (type == ShellController.class) {
                return new ShellController(container, this);
            }
            if (type == DashboardController.class) {
                return new DashboardController(container);
            }
            if (type == com.projetoj.frontend.identity.user.ui.controller.UserListController.class) {
                return new com.projetoj.frontend.identity.user.ui.controller.UserListController(container);
            }
            if (type == com.projetoj.frontend.identity.user.ui.controller.UserFormController.class) {
                return new com.projetoj.frontend.identity.user.ui.controller.UserFormController(container);
            }
            if (type == com.projetoj.frontend.identity.role.ui.controller.RoleListController.class) {
                return new com.projetoj.frontend.identity.role.ui.controller.RoleListController(container);
            }
            if (type == com.projetoj.frontend.identity.role.ui.controller.RoleFormController.class) {
                return new com.projetoj.frontend.identity.role.ui.controller.RoleFormController(container);
            }
            if (type == com.projetoj.frontend.identity.permission.ui.controller.ModuleListController.class) {
                return new com.projetoj.frontend.identity.permission.ui.controller.ModuleListController(container);
            }
            if (type == com.projetoj.frontend.identity.permission.ui.controller.ModuleFormController.class) {
                return new com.projetoj.frontend.identity.permission.ui.controller.ModuleFormController(container);
            }
            if (type == com.projetoj.frontend.identity.permission.ui.controller.PermissionListController.class) {
                return new com.projetoj.frontend.identity.permission.ui.controller.PermissionListController(container);
            }
            if (type == com.projetoj.frontend.identity.permission.ui.controller.PermissionFormController.class) {
                return new com.projetoj.frontend.identity.permission.ui.controller.PermissionFormController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.ProductListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.ProductListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.SupplierListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.SupplierListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.RequisitionListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.RequisitionListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.ApprovalListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.ApprovalListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.RfqListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.RfqListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.QuoteListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.QuoteListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.AwardListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.AwardListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.PurchaseOrderListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.PurchaseOrderListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.GoodsReceiptListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.GoodsReceiptListController(container);
            }
            if (type == com.projetoj.frontend.purchasing.ui.controller.ApiLogListController.class) {
                return new com.projetoj.frontend.purchasing.ui.controller.ApiLogListController(container);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Falha ao instanciar controller: " + type.getName(), e);
            }
        };
    }

    public Function<Class<?>, Object> getControllerFactory() {
        return controllerFactory;
    }

    public void showLogin() {
        show("/fxml/login.fxml", "Gestao operacional — Login", 720, 480, false, true, true);
    }

    public void showShell() {
        show("/fxml/shell.fxml", "Gestao operacional — Administracao Corporativa", 1180, 720, true, false, false);
    }

    public Parent loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            loader.setControllerFactory(controllerFactory::apply);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar view: " + fxmlPath, e);
        }
    }

    private void show(
            String fxmlPath,
            String title,
            double width,
            double height,
            boolean maximizedPreferred,
            boolean centerOnScreen,
            boolean fixedSize
    ) {
        Parent root = loadView(fxmlPath);
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, width, height);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm()
            );
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/layout.css")).toExternalForm()
            );
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
            if (!maximizedPreferred) {
                stage.setMaximized(false);
                stage.setWidth(width);
                stage.setHeight(height);
            }
        }
        stage.setTitle(title);
        stage.setResizable(!fixedSize);
        if (maximizedPreferred) {
            stage.setMaximized(true);
        } else if (centerOnScreen) {
            stage.setMaximized(false);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.centerOnScreen();
        }
        if (!stage.isShowing()) {
            stage.show();
            if (centerOnScreen && !maximizedPreferred) {
                stage.centerOnScreen();
            }
        }
    }
}
