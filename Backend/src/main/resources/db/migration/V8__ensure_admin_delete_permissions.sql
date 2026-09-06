-- Garante acao DELETE nas telas administrativas para o perfil ADMIN

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON TRUE
JOIN modules m ON m.id = p.module_id
JOIN actions a ON a.id = p.action_id
WHERE r.name = 'ADMIN'
  AND m.code IN ('USERS', 'ROLES', 'PERMISSIONS', 'MODULES', 'SYSTEM')
  AND a.code = 'DELETE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Permissoes diretas DELETE tambem para o usuario admin (reforco)
INSERT INTO user_permissions (user_id, permission_id)
SELECT u.id, p.id
FROM users u
JOIN permissions p ON TRUE
JOIN modules m ON m.id = p.module_id
JOIN actions a ON a.id = p.action_id
WHERE u.username = 'admin'
  AND m.code IN ('USERS', 'ROLES', 'PERMISSIONS', 'MODULES', 'SYSTEM')
  AND a.code = 'DELETE'
  AND NOT EXISTS (
      SELECT 1 FROM user_permissions up
      WHERE up.user_id = u.id AND up.permission_id = p.id
  );
