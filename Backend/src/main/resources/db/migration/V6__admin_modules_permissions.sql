-- Modulo MODULES + acoes de cadastro e garantia de permissoes do ADMIN

INSERT INTO modules (id, code, name, description, active, created_at, updated_at)
SELECT '22222222-2222-2222-2222-222222222009',
       'MODULES',
       'Modulos',
       'Administracao de modulos do sistema',
       TRUE,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM modules WHERE code = 'MODULES');

-- Permissoes do modulo MODULES para todas as acoes
INSERT INTO permissions (id, module_id, action_id)
SELECT
    ('33333333-3333-3333-3334-' || LPAD((ROW_NUMBER() OVER (ORDER BY a.code))::text, 12, '0'))::uuid,
    m.id,
    a.id
FROM modules m
CROSS JOIN actions a
WHERE m.code = 'MODULES'
  AND NOT EXISTS (
      SELECT 1 FROM permissions p
      WHERE p.module_id = m.id AND p.action_id = a.id
  );

-- ADMIN recebe qualquer permissao ainda nao atribuida
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Garante vinculo do usuario admin ao perfil ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Permissoes diretas do admin para VIEW/CREATE/UPDATE nos cadastros administrativos
INSERT INTO user_permissions (user_id, permission_id)
SELECT u.id, p.id
FROM users u
JOIN permissions p ON TRUE
JOIN modules m ON m.id = p.module_id
JOIN actions a ON a.id = p.action_id
WHERE u.username = 'admin'
  AND m.code IN ('USERS', 'ROLES', 'PERMISSIONS', 'MODULES')
  AND a.code IN ('VIEW', 'CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE')
  AND NOT EXISTS (
      SELECT 1 FROM user_permissions up
      WHERE up.user_id = u.id AND up.permission_id = p.id
  );
