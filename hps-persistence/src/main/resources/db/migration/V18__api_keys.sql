CREATE TABLE api_keys (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id           UUID NOT NULL UNIQUE,
    client_secret_hash  VARCHAR(255) NOT NULL,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    permissions         JSONB NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at        TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_client_id ON api_keys(client_id);
CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);

-- Create a default API key for the default tenant
-- client_id: 11111111-1111-1111-1111-111111111111
-- client_secret: hps-dev-secret-key (bcrypt hash below)
INSERT INTO api_keys (id, client_id, client_secret_hash, tenant_id, name)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-111111111111',
    '$2b$10$tq2j64izEp/Wlg4UZ5FSOeeUnJPggrwV9z0ga3LoKSUOUWD4MTKUS',
    '00000000-0000-0000-0000-000000000001',
    'Default Dev Key'
);
