-- Remove APIs/cadastros de Produtos x Fornecedores e Listas de Precos

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

DELETE FROM permissions
WHERE module_id IN (
    SELECT id FROM modules WHERE code IN ('SUPPLIER_PRODUCTS', 'SUPPLIER_PRICES')
);

DELETE FROM modules
WHERE code IN ('SUPPLIER_PRODUCTS', 'SUPPLIER_PRICES');

DROP TABLE IF EXISTS supplier_price;
DROP TABLE IF EXISTS supplier_product;
