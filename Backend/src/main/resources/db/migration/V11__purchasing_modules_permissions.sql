-- Modulos e permissoes do modulo Compras + LOG
INSERT INTO actions (id, code, description)
SELECT '11111111-1111-1111-1111-111111111011', 'SUBMIT', 'Submeter'
WHERE NOT EXISTS (SELECT 1 FROM actions WHERE code = 'SUBMIT');

INSERT INTO actions (id, code, description)
SELECT '11111111-1111-1111-1111-111111111012', 'RECEIVE', 'Receber'
WHERE NOT EXISTS (SELECT 1 FROM actions WHERE code = 'RECEIVE');

INSERT INTO modules (id, code, name, description, active, created_at, updated_at) VALUES
('22222222-2222-2222-2222-222222222010', 'PRODUCTS', 'Produtos', 'Cadastro de produtos', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222011', 'SUPPLIERS', 'Fornecedores', 'Cadastro de fornecedores', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222012', 'SUPPLIER_PRODUCTS', 'Produtos x Fornecedores', 'Associacao produto-fornecedor', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222013', 'SUPPLIER_PRICES', 'Listas de Precos', 'Precos por fornecedor/produto', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222014', 'PURCHASE_REQUISITIONS', 'Requisicoes de Compra', 'RC e itens', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222015', 'REQUISITION_APPROVALS', 'Aprovacao de RC', 'Fila de aprovacao', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222016', 'RFQS', 'RFQ', 'Solicitacoes de cotacao', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222017', 'SUPPLIER_QUOTES', 'Propostas', 'Propostas de fornecedores', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222018', 'QUOTE_AWARDS', 'Mapa Comparativo', 'Adjudicacao de cotacoes', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222019', 'PURCHASE_ORDERS', 'Pedidos de Compra', 'PO e follow-up', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222020', 'GOODS_RECEIPTS', 'Recebimentos', 'Recebimento de materiais', TRUE, NOW(), NOW()),
('22222222-2222-2222-2222-222222222021', 'API_LOGS', 'Log de API', 'Registro de operacoes da API', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Permissoes novas (modulos novos x todas as acoes)
INSERT INTO permissions (id, module_id, action_id)
SELECT
    ('33333333-3333-3333-4444-' || LPAD((ROW_NUMBER() OVER ())::text, 12, '0'))::uuid,
    m.id,
    a.id
FROM modules m
CROSS JOIN actions a
WHERE m.code IN (
    'PRODUCTS','SUPPLIERS','SUPPLIER_PRODUCTS','SUPPLIER_PRICES',
    'PURCHASE_REQUISITIONS','REQUISITION_APPROVALS','RFQS','SUPPLIER_QUOTES',
    'QUOTE_AWARDS','PURCHASE_ORDERS','GOODS_RECEIPTS','API_LOGS'
)
AND NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.module_id = m.id AND p.action_id = a.id
);

-- ADMIN recebe todas as novas permissoes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.name = 'ADMIN'
  AND m.code IN (
    'PRODUCTS','SUPPLIERS','SUPPLIER_PRODUCTS','SUPPLIER_PRICES',
    'PURCHASE_REQUISITIONS','REQUISITION_APPROVALS','RFQS','SUPPLIER_QUOTES',
    'QUOTE_AWARDS','PURCHASE_ORDERS','GOODS_RECEIPTS','API_LOGS'
  )
ON CONFLICT DO NOTHING;
