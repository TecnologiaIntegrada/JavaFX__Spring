-- Controle de autenticacao: chave longa por usuario com data/hora de emissao
CREATE TABLE auth_sessions (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    api_key     VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_auth_sessions_api_key UNIQUE (api_key)
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX idx_auth_sessions_created_at ON auth_sessions (created_at);
