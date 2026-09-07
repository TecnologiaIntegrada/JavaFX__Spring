-- Tabelas de dominio: tipo e status do fornecedor

CREATE TABLE IF NOT EXISTS tipos_fornecedor (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS status_fornecedor (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO tipos_fornecedor (id, code, name, active, created_at, updated_at) VALUES
('cccccccc-cccc-cccc-cccc-cccccccc0001', 'DISTRIBUIDOR', 'Distribuidor', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0002', 'FABRICANTE', 'Fabricante', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0003', 'ATACADISTA', 'Atacadista', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0004', 'VAREJISTA', 'Varejista', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0005', 'IMPORTADOR', 'Importador', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0006', 'PRESTADOR', 'Prestador de serviço', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0007', 'TRANSPORTADORA', 'Transportadora', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0008', 'LOCAL', 'Local', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0009', 'NACIONAL', 'Nacional', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0010', 'INTERNACIONAL', 'Internacional', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0011', 'MATERIA_PRIMA', 'Matéria-prima', TRUE, NOW(), NOW()),
('cccccccc-cccc-cccc-cccc-cccccccc0012', 'REVENDA', 'Revenda', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO status_fornecedor (id, code, name, active, created_at, updated_at) VALUES
('dddddddd-dddd-dddd-dddd-dddddddd0001', 'ATIVO', 'Ativo', TRUE, NOW(), NOW()),
('dddddddd-dddd-dddd-dddd-dddddddd0002', 'INATIVO', 'Inativo', TRUE, NOW(), NOW()),
('dddddddd-dddd-dddd-dddd-dddddddd0003', 'BLOQUEADO', 'Bloqueado', TRUE, NOW(), NOW()),
('dddddddd-dddd-dddd-dddd-dddddddd0004', 'PENDENTE', 'Pendente', TRUE, NOW(), NOW()),
('dddddddd-dddd-dddd-dddd-dddddddd0005', 'SUSPENSO', 'Suspenso', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

UPDATE supplier
SET supplier_type = UPPER(TRIM(supplier_type))
WHERE supplier_type IS NOT NULL;

INSERT INTO tipos_fornecedor (id, code, name, active, created_at, updated_at)
SELECT gen_random_uuid(), UPPER(TRIM(s.supplier_type)), INITCAP(REPLACE(LOWER(TRIM(s.supplier_type)), '_', ' ')), TRUE, NOW(), NOW()
FROM (SELECT DISTINCT supplier_type FROM supplier WHERE supplier_type IS NOT NULL AND TRIM(supplier_type) <> '') s
ON CONFLICT (code) DO NOTHING;

UPDATE supplier
SET status = CASE
    WHEN UPPER(TRIM(status)) IN ('ACTIVE', 'ATIVO') THEN 'ATIVO'
    WHEN UPPER(TRIM(status)) IN ('INACTIVE', 'INATIVO') THEN 'INATIVO'
    WHEN UPPER(TRIM(status)) IN ('BLOCKED', 'BLOQUEADO') THEN 'BLOQUEADO'
    WHEN UPPER(TRIM(status)) IN ('PENDING', 'PENDENTE') THEN 'PENDENTE'
    WHEN UPPER(TRIM(status)) IN ('SUSPENDED', 'SUSPENSO') THEN 'SUSPENSO'
    ELSE UPPER(TRIM(status))
END
WHERE status IS NOT NULL;

INSERT INTO status_fornecedor (id, code, name, active, created_at, updated_at)
SELECT gen_random_uuid(), UPPER(TRIM(s.status)), INITCAP(REPLACE(LOWER(TRIM(s.status)), '_', ' ')), TRUE, NOW(), NOW()
FROM (SELECT DISTINCT status FROM supplier WHERE status IS NOT NULL AND TRIM(status) <> '') s
ON CONFLICT (code) DO NOTHING;
