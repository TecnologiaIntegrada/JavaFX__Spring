-- Usuario passa a ter exatamente um perfil (role_id)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id UUID;

-- Migra vinculo existente (primeiro perfil encontrado)
UPDATE users u
SET role_id = sub.role_id
FROM (
    SELECT DISTINCT ON (ur.user_id) ur.user_id, ur.role_id
    FROM user_roles ur
    ORDER BY ur.user_id, ur.role_id
) sub
WHERE u.id = sub.user_id
  AND u.role_id IS NULL;

-- Admin sem role: vincula ao ADMIN
UPDATE users u
SET role_id = r.id
FROM roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN'
  AND u.role_id IS NULL;

-- Demais usuarios sem role: VIEWER
UPDATE users u
SET role_id = r.id
FROM roles r
WHERE u.role_id IS NULL
  AND r.name = 'VIEWER';

ALTER TABLE users
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_role;

ALTER TABLE users
    ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id);

CREATE INDEX IF NOT EXISTS idx_users_role_id ON users (role_id);

-- Remove N:N antigo
DROP TABLE IF EXISTS user_roles;

-- Senha do admin = "admin" (BCrypt)
UPDATE users
SET password_hash = '$2a$10$wPdyYJUVEUIal1tupaUWKeqYb2tESRXL7JBnJG8/wWYfw5SX3P6.O',
    updated_at = NOW()
WHERE username = 'admin';

-- Garante modulo SYSTEM para tela de conectividade
INSERT INTO modules (id, code, name, description, active, created_at, updated_at)
SELECT '22222222-2222-2222-2222-222222222010',
       'SYSTEM',
       'Sistema',
       'Ferramentas de sistema e conectividade',
       TRUE,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM modules WHERE code = 'SYSTEM');

-- Permissoes SYSTEM x todas acoes
INSERT INTO permissions (id, module_id, action_id)
SELECT
    ('33333333-3333-3333-3335-' || LPAD((ROW_NUMBER() OVER (ORDER BY a.code))::text, 12, '0'))::uuid,
    m.id,
    a.id
FROM modules m
CROSS JOIN actions a
WHERE m.code = 'SYSTEM'
  AND NOT EXISTS (
      SELECT 1 FROM permissions p WHERE p.module_id = m.id AND p.action_id = a.id
  );

-- ADMIN recebe TODAS as permissoes (incluindo novas)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Admin vinculado ao perfil ADMIN
UPDATE users u
SET role_id = r.id,
    updated_at = NOW()
FROM roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN';
