-- Campos de auditoria: id + nome do usuario criador/atualizador (preenchidos pela API)

-- Produto
ALTER TABLE product ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE product ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE product ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE product ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Fornecedor (created_by/updated_by ja existem)
ALTER TABLE supplier ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE supplier ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Contatos
ALTER TABLE supplier_contact ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE supplier_contact ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE supplier_contact ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE supplier_contact ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Produto x Fornecedor
ALTER TABLE supplier_product ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE supplier_product ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE supplier_product ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE supplier_product ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Precos (created_by ja existe)
ALTER TABLE supplier_price ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE supplier_price ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE supplier_price ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Requisicao
ALTER TABLE purchase_requisition ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE purchase_requisition ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE purchase_requisition_item ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE purchase_requisition_item ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE purchase_requisition_item ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE purchase_requisition_item ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE purchase_requisition_approval ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE purchase_requisition_approval ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE purchase_requisition_approval ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE purchase_requisition_approval ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- RFQ
ALTER TABLE rfq ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE rfq ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE rfq ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE rfq ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE rfq_item ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE rfq_item ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE rfq_item ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE rfq_item ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE rfq_supplier ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE rfq_supplier ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE rfq_supplier ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE rfq_supplier ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Propostas
ALTER TABLE supplier_quote ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE supplier_quote ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE supplier_quote ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE supplier_quote ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE supplier_quote_item ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE supplier_quote_item ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE supplier_quote_item ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE supplier_quote_item ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

-- Adjudicacao
ALTER TABLE quote_award ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE quote_award ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE quote_award ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE quote_award ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);
UPDATE quote_award SET created_by = awarded_by WHERE created_by IS NULL;

-- Pedido
ALTER TABLE purchase_order ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE purchase_order ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE purchase_order ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE purchase_order ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE purchase_order_item ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE purchase_order_item ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE purchase_order_item ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE purchase_order_item ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);

ALTER TABLE purchase_order_followup ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE purchase_order_followup ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE purchase_order_followup ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE purchase_order_followup ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);
UPDATE purchase_order_followup SET created_by = user_id WHERE created_by IS NULL;

-- Recebimento
ALTER TABLE goods_receipt ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE goods_receipt ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE goods_receipt ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE goods_receipt ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);
UPDATE goods_receipt SET created_by = received_by WHERE created_by IS NULL;

ALTER TABLE goods_receipt_item ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);
ALTER TABLE goods_receipt_item ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);
ALTER TABLE goods_receipt_item ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE goods_receipt_item ADD COLUMN IF NOT EXISTS updated_by_name VARCHAR(200);
