-- Seed inicial de seguranca / cadastros

-- Actions
INSERT INTO actions (id, code, description) VALUES
('11111111-1111-1111-1111-111111111001', 'VIEW', 'Visualizar'),
('11111111-1111-1111-1111-111111111002', 'CREATE', 'Criar'),
('11111111-1111-1111-1111-111111111003', 'UPDATE', 'Atualizar'),
('11111111-1111-1111-1111-111111111004', 'DELETE', 'Excluir'),
('11111111-1111-1111-1111-111111111005', 'ACTIVATE', 'Ativar'),
('11111111-1111-1111-1111-111111111006', 'DEACTIVATE', 'Desativar'),
('11111111-1111-1111-1111-111111111007', 'EXPORT', 'Exportar'),
('11111111-1111-1111-1111-111111111008', 'IMPORT', 'Importar'),
('11111111-1111-1111-1111-111111111009', 'APPROVE', 'Aprovar'),
('11111111-1111-1111-1111-111111111010', 'EXECUTE', 'Executar');

-- Modules
INSERT INTO modules (id, code, name, description, active, created_at, updated_at) VALUES
('22222222-2222-2222-2222-222222222001', 'USERS', 'Usuarios', 'Administracao de usuarios', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222002', 'ROLES', 'Perfis', 'Administracao de perfis', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222003', 'PERMISSIONS', 'Permissoes', 'Administracao de permissoes', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222004', 'CUSTOMERS', 'Clientes', 'Gestao de clientes', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222005', 'ORDERS', 'Pedidos', 'Gestao de pedidos', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222006', 'REPORTS', 'Relatorios', 'Relatorios gerenciais', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222007', 'SETTINGS', 'Configuracoes', 'Configuracoes do sistema', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222008', 'AUDIT', 'Auditoria', 'Trilha de auditoria', TRUE, NOW(), NOW());

-- Permissions = todos os modulos x todas as acoes
INSERT INTO permissions (id, module_id, action_id)
SELECT
    ('33333333-3333-3333-3333-' || LPAD((ROW_NUMBER() OVER ())::text, 12, '0'))::uuid,
    m.id,
    a.id
FROM modules m
CROSS JOIN actions a;

-- Roles
INSERT INTO roles (id, name, description, active, created_at, updated_at) VALUES
('44444444-4444-4444-4444-444444444001', 'ADMIN', 'Administrador do sistema', TRUE, NOW(), NOW()),
('44444444-4444-4444-4444-444444444002', 'MANAGER', 'Gestor operacional', TRUE, NOW(), NOW()),
('44444444-4444-4444-4444-444444444003', 'OPERATOR', 'Operador', TRUE, NOW(), NOW()),
('44444444-4444-4444-4444-444444444004', 'VIEWER', 'Somente leitura', TRUE, NOW(), NOW());

-- ADMIN recebe todas as permissoes
INSERT INTO role_permissions (role_id, permission_id)
SELECT '44444444-4444-4444-4444-444444444001', p.id
FROM permissions p;

-- VIEWER: apenas VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT '44444444-4444-4444-4444-444444444004', p.id
FROM permissions p
JOIN actions a ON a.id = p.action_id
WHERE a.code = 'VIEW';

-- MANAGER: VIEW/CREATE/UPDATE/EXPORT/APPROVE
INSERT INTO role_permissions (role_id, permission_id)
SELECT '44444444-4444-4444-4444-444444444002', p.id
FROM permissions p
JOIN actions a ON a.id = p.action_id
WHERE a.code IN ('VIEW', 'CREATE', 'UPDATE', 'EXPORT', 'APPROVE', 'ACTIVATE', 'DEACTIVATE');

-- OPERATOR: VIEW/CREATE/UPDATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT '44444444-4444-4444-4444-444444444003', p.id
FROM permissions p
JOIN actions a ON a.id = p.action_id
WHERE a.code IN ('VIEW', 'CREATE', 'UPDATE');

-- Usuario admin de desenvolvimento
-- Senha: Admin@123 (BCrypt)
INSERT INTO users (id, username, full_name, email, password_hash, status, created_at, updated_at) VALUES
('55555555-5555-5555-5555-555555555001',
 'admin',
 'Administrador do Sistema',
 'admin@projetoj.local',
 '$2a$10$UdM3FhHh5pqXX/mz.jMxzedSh2zum20ZOfaAregePMYc8NekC1yie',
 'ACTIVE',
 NOW(),
 NOW());

INSERT INTO user_roles (user_id, role_id) VALUES
('55555555-5555-5555-5555-555555555001', '44444444-4444-4444-4444-444444444001');
