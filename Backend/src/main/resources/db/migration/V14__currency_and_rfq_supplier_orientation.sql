-- Tabela de moedas + orientacoes por fornecedor na RFQ

CREATE TABLE IF NOT EXISTS moedas (
    id UUID PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO moedas (id, code, name, symbol, active, created_at, updated_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'BRL', 'Real', 'R$', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002', 'USD', 'Dólar americano', 'US$', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0003', 'EUR', 'Euro', '€', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0004', 'GBP', 'Libra esterlina', '£', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0005', 'JPY', 'Iene japonês', '¥', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0006', 'CAD', 'Dólar canadense', 'C$', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0007', 'CHF', 'Franco suíço', 'CHF', TRUE, NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0008', 'CNY', 'Yuan chinês', '¥', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

ALTER TABLE rfq_supplier
    ADD COLUMN IF NOT EXISTS orientation_comments TEXT;
