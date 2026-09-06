-- Modulos, acoes e permissoes
CREATE TABLE modules (
    id              UUID            PRIMARY KEY,
    code            VARCHAR(50)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_modules_code UNIQUE (code)
);

CREATE INDEX idx_modules_code ON modules (code);
CREATE INDEX idx_modules_active ON modules (active);

CREATE TABLE actions (
    id              UUID            PRIMARY KEY,
    code            VARCHAR(50)     NOT NULL,
    description     VARCHAR(255),
    CONSTRAINT uk_actions_code UNIQUE (code)
);

CREATE TABLE permissions (
    id              UUID            PRIMARY KEY,
    module_id       UUID            NOT NULL,
    action_id       UUID            NOT NULL,
    CONSTRAINT uk_permissions_module_action UNIQUE (module_id, action_id),
    CONSTRAINT fk_permissions_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_permissions_action FOREIGN KEY (action_id) REFERENCES actions (id)
);

CREATE INDEX idx_permissions_module_action ON permissions (module_id, action_id);

CREATE TABLE role_permissions (
    role_id         UUID            NOT NULL,
    permission_id   UUID            NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);

CREATE TABLE user_permissions (
    user_id         UUID            NOT NULL,
    permission_id   UUID            NOT NULL,
    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE INDEX idx_user_permissions_user_id ON user_permissions (user_id);
