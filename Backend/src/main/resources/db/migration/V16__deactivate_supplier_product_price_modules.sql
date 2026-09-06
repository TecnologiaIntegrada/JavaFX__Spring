-- Remove do sistema os modulos Produtos x Fornecedores e Listas de Precos

UPDATE modules
SET active = FALSE,
    updated_at = NOW()
WHERE code IN ('SUPPLIER_PRODUCTS', 'SUPPLIER_PRICES');

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT p.id
    FROM permissions p
    JOIN modules m ON m.id = p.module_id
    WHERE m.code IN ('SUPPLIER_PRODUCTS', 'SUPPLIER_PRICES')
);

DELETE FROM user_permissions
WHERE permission_id IN (
    SELECT p.id
    FROM permissions p
    JOIN modules m ON m.id = p.module_id
    WHERE m.code IN ('SUPPLIER_PRODUCTS', 'SUPPLIER_PRICES')
);
