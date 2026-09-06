package com.projetoj.frontend.shared.di;

import com.projetoj.frontend.identity.auth.adapter.rest.RestAuthApiAdapter;
import com.projetoj.frontend.identity.auth.application.LoginUseCase;
import com.projetoj.frontend.identity.permission.adapter.rest.RestModuleApiAdapter;
import com.projetoj.frontend.identity.permission.adapter.rest.RestPermissionApiAdapter;
import com.projetoj.frontend.identity.permission.application.CatalogApplicationService;
import com.projetoj.frontend.identity.permission.application.ListModulesUseCase;
import com.projetoj.frontend.identity.permission.application.ListPermissionsUseCase;
import com.projetoj.frontend.identity.role.adapter.rest.RestRoleApiAdapter;
import com.projetoj.frontend.identity.role.application.ListRolesUseCase;
import com.projetoj.frontend.identity.role.application.RoleApplicationService;
import com.projetoj.frontend.identity.user.adapter.rest.RestUserApiAdapter;
import com.projetoj.frontend.identity.user.application.ListUsersUseCase;
import com.projetoj.frontend.identity.user.application.UserApplicationService;
import com.projetoj.frontend.purchasing.adapter.rest.PurchasingApiAdapter;
import com.projetoj.frontend.shared.application.CheckApiHealthUseCase;
import com.projetoj.frontend.shared.config.AppConfig;
import com.projetoj.frontend.shared.http.ApiClient;
import com.projetoj.frontend.shared.security.SessionContext;

public final class DependencyContainer {

    private final AppConfig appConfig;
    private final ApiClient apiClient;
    private final SessionContext sessionContext;
    private final CheckApiHealthUseCase checkApiHealthUseCase;
    private final LoginUseCase loginUseCase;
    private final UserApplicationService userApplicationService;
    private final RoleApplicationService roleApplicationService;
    private final CatalogApplicationService catalogApplicationService;
    private final ListUsersUseCase listUsersUseCase;
    private final ListRolesUseCase listRolesUseCase;
    private final ListModulesUseCase listModulesUseCase;
    private final ListPermissionsUseCase listPermissionsUseCase;
    private final PurchasingApiAdapter purchasingApiAdapter;

    private DependencyContainer(AppConfig appConfig, ApiClient apiClient) {
        this.appConfig = appConfig;
        this.apiClient = apiClient;
        this.sessionContext = new SessionContext();
        this.checkApiHealthUseCase = new CheckApiHealthUseCase(apiClient);
        this.loginUseCase = new LoginUseCase(new RestAuthApiAdapter(apiClient), sessionContext, apiClient);

        RestUserApiAdapter userApi = new RestUserApiAdapter(apiClient);
        RestRoleApiAdapter roleApi = new RestRoleApiAdapter(apiClient);
        RestModuleApiAdapter moduleApi = new RestModuleApiAdapter(apiClient);
        RestPermissionApiAdapter permissionApi = new RestPermissionApiAdapter(apiClient);
        this.purchasingApiAdapter = new PurchasingApiAdapter(apiClient);

        this.userApplicationService = new UserApplicationService(userApi);
        this.roleApplicationService = new RoleApplicationService(roleApi);
        this.catalogApplicationService = new CatalogApplicationService(moduleApi, permissionApi);
        this.listUsersUseCase = new ListUsersUseCase(userApi);
        this.listRolesUseCase = new ListRolesUseCase(roleApi);
        this.listModulesUseCase = new ListModulesUseCase(moduleApi);
        this.listPermissionsUseCase = new ListPermissionsUseCase(permissionApi);
    }

    public static DependencyContainer createDefault() {
        AppConfig config = AppConfig.load();
        return new DependencyContainer(config, new ApiClient(config));
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public SessionContext getSessionContext() {
        return sessionContext;
    }

    public CheckApiHealthUseCase getCheckApiHealthUseCase() {
        return checkApiHealthUseCase;
    }

    public LoginUseCase getLoginUseCase() {
        return loginUseCase;
    }

    public UserApplicationService getUserApplicationService() {
        return userApplicationService;
    }

    public RoleApplicationService getRoleApplicationService() {
        return roleApplicationService;
    }

    public CatalogApplicationService getCatalogApplicationService() {
        return catalogApplicationService;
    }

    public ListUsersUseCase getListUsersUseCase() {
        return listUsersUseCase;
    }

    public ListRolesUseCase getListRolesUseCase() {
        return listRolesUseCase;
    }

    public ListModulesUseCase getListModulesUseCase() {
        return listModulesUseCase;
    }

    public ListPermissionsUseCase getListPermissionsUseCase() {
        return listPermissionsUseCase;
    }

    public PurchasingApiAdapter getPurchasingApiAdapter() {
        return purchasingApiAdapter;
    }
}
