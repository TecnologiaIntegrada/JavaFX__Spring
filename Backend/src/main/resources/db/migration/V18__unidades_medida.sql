-- Unidades de medida do cadastro de produtos

CREATE TABLE IF NOT EXISTS unidades_medida (
    id UUID PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO unidades_medida (id, code, name, active, created_at, updated_at) VALUES
-- Quantidade / embalagem
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001', 'UN', 'Unidade', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0002', 'PC', 'Peça', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0003', 'CJ', 'Conjunto', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0004', 'CX', 'Caixa', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0005', 'PCT', 'Pacote', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0006', 'FD', 'Fardo', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0007', 'DZ', 'Dúzia', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0008', 'PAR', 'Par', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0009', 'KIT', 'Kit', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0010', 'RL', 'Rolo', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0011', 'BD', 'Balde', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0012', 'SC', 'Saco', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0013', 'LT', 'Lata', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0014', 'FR', 'Frasco', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0015', 'TB', 'Tubo', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0016', 'BL', 'Blister', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0017', 'AM', 'Ampola', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0018', 'JG', 'Jogo', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0019', 'GL', 'Galão', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0020', 'SERV', 'Serviço', TRUE, NOW(), NOW()),
-- Massa
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0021', 'KG', 'Quilograma', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0022', 'GR', 'Grama', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0023', 'MG', 'Miligrama', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0024', 'T', 'Tonelada', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0025', 'LB', 'Libra', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0026', 'OZ', 'Onça', TRUE, NOW(), NOW()),
-- Comprimento
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0027', 'M', 'Metro', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0028', 'ML', 'Metro linear', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0029', 'CM', 'Centímetro', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0030', 'MM', 'Milímetro', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0031', 'KM', 'Quilômetro', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0032', 'IN', 'Polegada', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0033', 'FT', 'Pé', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0034', 'YD', 'Jarda', TRUE, NOW(), NOW()),
-- Área
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0035', 'M2', 'Metro quadrado', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0036', 'CM2', 'Centímetro quadrado', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0037', 'KM2', 'Quilômetro quadrado', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0038', 'HA', 'Hectare', TRUE, NOW(), NOW()),
-- Volume
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0039', 'M3', 'Metro cúbico', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0040', 'L', 'Litro', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0041', 'MLT', 'Mililitro', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0042', 'CM3', 'Centímetro cúbico', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0043', 'GAL', 'Galão (volume)', TRUE, NOW(), NOW()),
-- Tempo
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0044', 'HR', 'Hora', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0045', 'MIN', 'Minuto', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0046', 'DIA', 'Dia', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0047', 'MES', 'Mês', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0048', 'ANO', 'Ano', TRUE, NOW(), NOW()),
-- Energia / outros
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0049', 'KW', 'Quilowatt', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0050', 'KWH', 'Quilowatt-hora', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0051', 'CV', 'Cavalo-vapor', TRUE, NOW(), NOW()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0052', 'VB', 'Verba', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

UPDATE product SET unit_of_measure = UPPER(TRIM(unit_of_measure))
WHERE unit_of_measure IS NOT NULL;

UPDATE product SET unit_of_measure = 'KG'
WHERE UPPER(TRIM(unit_of_measure)) IN ('KILO', 'KILOS', 'QUILO', 'QUILOS');

UPDATE product SET unit_of_measure = 'GR'
WHERE UPPER(TRIM(unit_of_measure)) IN ('G', 'GRAMA', 'GRAMAS');

UPDATE product SET unit_of_measure = 'UN'
WHERE unit_of_measure IS NULL
   OR TRIM(unit_of_measure) = ''
   OR UPPER(TRIM(unit_of_measure)) NOT IN (SELECT code FROM unidades_medida);
