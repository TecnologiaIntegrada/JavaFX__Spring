-- Corrige ausencia do modulo PRODUCTS: o UUID ...0010 ja era usado por SYSTEM (V6),
-- entao o INSERT de V11 ignorou PRODUCTS via ON CONFLICT DO NOTHING.

INSERT INTO modules (id, code, name, description, active, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222022',
    'PRODUCTS',
    'Produtos',
    'Cadastro de produtos',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (id, module_id, action_id)
SELECT
    ('33333333-3333-3333-5555-' || LPAD((ROW_NUMBER() OVER ())::text, 12, '0'))::uuid,
    m.id,
    a.id
FROM modules m
CROSS JOIN actions a
WHERE m.code = 'PRODUCTS'
  AND NOT EXISTS (
      SELECT 1 FROM permissions p WHERE p.module_id = m.id AND p.action_id = a.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.name = 'ADMIN'
  AND m.code = 'PRODUCTS'
ON CONFLICT DO NOTHING;
