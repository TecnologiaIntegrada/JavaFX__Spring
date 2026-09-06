-- Produto passa a referenciar fornecedor no lugar do texto livre de fabricante

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS supplier_id UUID REFERENCES supplier(id);

CREATE INDEX IF NOT EXISTS ix_product_supplier ON product (supplier_id);
