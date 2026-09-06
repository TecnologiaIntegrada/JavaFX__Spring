-- =====================================================================
-- Modulo de Compras (MVP) + cadastros auxiliares + LOG de API
-- PKs em UUID para alinhar com identity existente
-- =====================================================================

-- Sequencias de documentos
CREATE TABLE document_sequence (
    document_type VARCHAR(30) PRIMARY KEY,
    next_number   BIGINT NOT NULL
);

INSERT INTO document_sequence (document_type, next_number) VALUES
('PURCHASE_REQUISITION', 1),
('RFQ', 1),
('PURCHASE_ORDER', 1),
('GOODS_RECEIPT', 1);

-- LOG de operacoes da API
CREATE TABLE api_operation_log (
    id              UUID PRIMARY KEY,
    user_id         UUID,
    username        VARCHAR(100),
    http_method     VARCHAR(10) NOT NULL,
    request_path    VARCHAR(500) NOT NULL,
    query_string    VARCHAR(1000),
    request_body    TEXT,
    response_status INTEGER,
    client_ip       VARCHAR(64),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_api_operation_log_occurred ON api_operation_log (occurred_at DESC);
CREATE INDEX ix_api_operation_log_user ON api_operation_log (user_id, occurred_at DESC);
CREATE INDEX ix_api_operation_log_path ON api_operation_log (request_path);

-- Auxiliares
CREATE TABLE product (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(250) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL,
    manufacturer VARCHAR(150),
    manufacturer_part_number VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_product_code ON product (code);
CREATE INDEX ix_product_description ON product (description);

CREATE TABLE department (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_department_code ON department (code);

CREATE TABLE cost_center (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_cost_center_code ON cost_center (code);

CREATE TABLE project (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_project_code ON project (code);

-- Fornecedores
CREATE TABLE supplier (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(200) NOT NULL,
    trade_name VARCHAR(200),
    person_type CHAR(1) NOT NULL CHECK (person_type IN ('J','F')),
    tax_id VARCHAR(20) NOT NULL,
    state_registration VARCHAR(30),
    municipal_registration VARCHAR(30),
    supplier_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    default_currency CHAR(3) NOT NULL DEFAULT 'BRL',
    default_payment_terms VARCHAR(100),
    average_lead_time_days INTEGER,
    minimum_order_value NUMERIC(18,4),
    buyer_user_id UUID REFERENCES users(id),
    zip_code VARCHAR(10),
    address_line VARCHAR(200),
    address_number VARCHAR(20),
    address_complement VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100),
    state_code VARCHAR(10),
    country_code CHAR(2) DEFAULT 'BR',
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE UNIQUE INDEX ux_supplier_tax_id_active ON supplier (tax_id) WHERE active = TRUE;
CREATE INDEX ix_supplier_legal_name ON supplier (legal_name);
CREATE INDEX ix_supplier_status ON supplier (status);

CREATE TABLE supplier_contact (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    department VARCHAR(100),
    job_title VARCHAR(100),
    phone VARCHAR(30),
    mobile VARCHAR(30),
    email VARCHAR(200),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_supplier_contact_supplier ON supplier_contact (supplier_id);
CREATE UNIQUE INDEX ux_supplier_primary_contact ON supplier_contact (supplier_id) WHERE is_primary = TRUE AND active = TRUE;

CREATE TABLE supplier_product (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    product_id UUID NOT NULL REFERENCES product(id),
    supplier_product_code VARCHAR(100),
    manufacturer VARCHAR(150),
    manufacturer_part_number VARCHAR(100),
    preferred BOOLEAN NOT NULL DEFAULT FALSE,
    approved BOOLEAN NOT NULL DEFAULT TRUE,
    lead_time_days INTEGER,
    minimum_order_qty NUMERIC(18,6),
    order_multiple NUMERIC(18,6),
    purchase_uom VARCHAR(20) NOT NULL,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_supplier_product ON supplier_product (supplier_id, product_id) WHERE active = TRUE;
CREATE UNIQUE INDEX ux_supplier_product_preferred ON supplier_product (product_id) WHERE preferred = TRUE AND active = TRUE;

CREATE TABLE supplier_price (
    id UUID PRIMARY KEY,
    supplier_product_id UUID NOT NULL REFERENCES supplier_product(id),
    minimum_qty NUMERIC(18,6) NOT NULL DEFAULT 1,
    unit_price NUMERIC(18,4) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    discount_percent NUMERIC(8,4) DEFAULT 0,
    valid_from DATE NOT NULL,
    valid_to DATE,
    lead_time_days INTEGER,
    payment_terms VARCHAR(100),
    freight_included BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID
);
CREATE INDEX ix_supplier_price_supplier_product ON supplier_price (supplier_product_id);

-- Requisição
CREATE TABLE purchase_requisition (
    id UUID PRIMARY KEY,
    requisition_number VARCHAR(30) NOT NULL,
    requester_user_id UUID NOT NULL REFERENCES users(id),
    department_id UUID REFERENCES department(id),
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    priority VARCHAR(20) NOT NULL,
    demand_source VARCHAR(30) NOT NULL,
    project_id UUID REFERENCES project(id),
    cost_center_id UUID REFERENCES cost_center(id),
    justification TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE UNIQUE INDEX ux_purchase_requisition_number ON purchase_requisition (requisition_number);
CREATE INDEX ix_pr_status ON purchase_requisition (status);

CREATE TABLE purchase_requisition_item (
    id UUID PRIMARY KEY,
    requisition_id UUID NOT NULL REFERENCES purchase_requisition(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES product(id),
    description_override VARCHAR(500),
    quantity NUMERIC(18,6) NOT NULL CHECK (quantity > 0),
    uom VARCHAR(20) NOT NULL,
    required_date DATE NOT NULL,
    project_id UUID REFERENCES project(id),
    cost_center_id UUID REFERENCES cost_center(id),
    suggested_supplier_id UUID REFERENCES supplier(id),
    technical_notes TEXT,
    ordered_quantity NUMERIC(18,6) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_pr_item_line ON purchase_requisition_item (requisition_id, line_number);

CREATE TABLE purchase_requisition_approval (
    id UUID PRIMARY KEY,
    requisition_id UUID NOT NULL REFERENCES purchase_requisition(id) ON DELETE CASCADE,
    approval_level INTEGER NOT NULL,
    approver_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_at TIMESTAMPTZ,
    comments TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_pr_approval_level ON purchase_requisition_approval (requisition_id, approval_level);

-- RFQ
CREATE TABLE rfq (
    id UUID PRIMARY KEY,
    rfq_number VARCHAR(30) NOT NULL,
    description VARCHAR(250),
    buyer_user_id UUID NOT NULL REFERENCES users(id),
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    response_due_date DATE NOT NULL,
    default_currency CHAR(3) NOT NULL DEFAULT 'BRL',
    general_terms TEXT,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_rfq_number ON rfq (rfq_number);

CREATE TABLE rfq_item (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfq(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    requisition_item_id UUID REFERENCES purchase_requisition_item(id),
    product_id UUID NOT NULL REFERENCES product(id),
    description VARCHAR(500),
    quantity NUMERIC(18,6) NOT NULL,
    uom VARCHAR(20) NOT NULL,
    required_date DATE,
    technical_specification TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);
CREATE UNIQUE INDEX ux_rfq_item_line ON rfq_item (rfq_id, line_number);

CREATE TABLE rfq_supplier (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfq(id) ON DELETE CASCADE,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    supplier_contact_id UUID REFERENCES supplier_contact(id),
    sent_at TIMESTAMPTZ,
    response_received_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'INVITED'
);
CREATE UNIQUE INDEX ux_rfq_supplier ON rfq_supplier (rfq_id, supplier_id);

CREATE TABLE supplier_quote (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfq(id),
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    supplier_quote_number VARCHAR(100),
    quote_date DATE NOT NULL,
    valid_until DATE,
    currency CHAR(3) NOT NULL,
    payment_terms VARCHAR(100),
    freight_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    insurance_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    other_costs NUMERIC(18,4) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_supplier_quote_rfq_supplier ON supplier_quote (rfq_id, supplier_id);

CREATE TABLE supplier_quote_item (
    id UUID PRIMARY KEY,
    supplier_quote_id UUID NOT NULL REFERENCES supplier_quote(id) ON DELETE CASCADE,
    rfq_item_id UUID NOT NULL REFERENCES rfq_item(id),
    offered_qty NUMERIC(18,6) NOT NULL,
    unit_price NUMERIC(18,4) NOT NULL,
    discount_percent NUMERIC(8,4) NOT NULL DEFAULT 0,
    promised_date DATE,
    lead_time_days INTEGER,
    manufacturer VARCHAR(150),
    manufacturer_part_number VARCHAR(100),
    technical_approved BOOLEAN,
    technical_notes TEXT,
    commercial_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_supplier_quote_item ON supplier_quote_item (supplier_quote_id, rfq_item_id);

CREATE TABLE quote_award (
    id UUID PRIMARY KEY,
    rfq_item_id UUID NOT NULL REFERENCES rfq_item(id),
    supplier_quote_item_id UUID NOT NULL REFERENCES supplier_quote_item(id),
    awarded_qty NUMERIC(18,6) NOT NULL,
    award_reason VARCHAR(100) NOT NULL,
    notes TEXT,
    awarded_by UUID NOT NULL REFERENCES users(id),
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_quote_award_rfq_item ON quote_award (rfq_item_id);

-- Pedido
CREATE TABLE purchase_order (
    id UUID PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    buyer_user_id UUID NOT NULL REFERENCES users(id),
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    payment_terms VARCHAR(100),
    delivery_address TEXT,
    freight_terms VARCHAR(50),
    project_id UUID REFERENCES project(id),
    cost_center_id UUID REFERENCES cost_center(id),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    subtotal NUMERIC(18,4) NOT NULL DEFAULT 0,
    freight_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    other_costs NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    notes TEXT,
    sent_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_purchase_order_number ON purchase_order (order_number);
CREATE INDEX ix_po_status ON purchase_order (status);

CREATE TABLE purchase_order_item (
    id UUID PRIMARY KEY,
    purchase_order_id UUID NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES product(id),
    requisition_item_id UUID REFERENCES purchase_requisition_item(id),
    rfq_item_id UUID REFERENCES rfq_item(id),
    quote_award_id UUID REFERENCES quote_award(id),
    description VARCHAR(500),
    ordered_qty NUMERIC(18,6) NOT NULL,
    received_qty NUMERIC(18,6) NOT NULL DEFAULT 0,
    uom VARCHAR(20) NOT NULL,
    unit_price NUMERIC(18,4) NOT NULL,
    discount_percent NUMERIC(8,4) NOT NULL DEFAULT 0,
    promised_date DATE,
    project_id UUID REFERENCES project(id),
    cost_center_id UUID REFERENCES cost_center(id),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_po_item_line ON purchase_order_item (purchase_order_id, line_number);

CREATE TABLE purchase_order_followup (
    id UUID PRIMARY KEY,
    purchase_order_id UUID NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
    purchase_order_item_id UUID REFERENCES purchase_order_item(id),
    contact_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    contact_type VARCHAR(30) NOT NULL,
    supplier_contact_id UUID REFERENCES supplier_contact(id),
    reported_status VARCHAR(100),
    new_promised_date DATE,
    comments TEXT,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_po_followup_order ON purchase_order_followup (purchase_order_id, contact_at DESC);

CREATE TABLE goods_receipt (
    id UUID PRIMARY KEY,
    receipt_number VARCHAR(30) NOT NULL,
    purchase_order_id UUID NOT NULL REFERENCES purchase_order(id),
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    received_by UUID NOT NULL REFERENCES users(id),
    supplier_document_number VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_goods_receipt_number ON goods_receipt (receipt_number);

CREATE TABLE goods_receipt_item (
    id UUID PRIMARY KEY,
    goods_receipt_id UUID NOT NULL REFERENCES goods_receipt(id) ON DELETE CASCADE,
    purchase_order_item_id UUID NOT NULL REFERENCES purchase_order_item(id),
    received_qty NUMERIC(18,6) NOT NULL CHECK (received_qty > 0),
    lot_number VARCHAR(100),
    serial_number VARCHAR(150),
    item_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_goods_receipt_item_receipt ON goods_receipt_item (goods_receipt_id);

-- Seeds auxiliares basicos
INSERT INTO department (id, code, name) VALUES
('a1000000-0000-0000-0000-000000000001', 'COMPRAS', 'Compras'),
('a1000000-0000-0000-0000-000000000002', 'ENG', 'Engenharia'),
('a1000000-0000-0000-0000-000000000003', 'PROD', 'Producao');

INSERT INTO cost_center (id, code, name) VALUES
('a2000000-0000-0000-0000-000000000001', 'CC-001', 'Centro Operacional'),
('a2000000-0000-0000-0000-000000000002', 'CC-002', 'Centro Administrativo');

INSERT INTO project (id, code, name, status) VALUES
('a3000000-0000-0000-0000-000000000001', 'PRJ-001', 'Projeto Padrao', 'OPEN');
