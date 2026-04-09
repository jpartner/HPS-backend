-- ============================================================================
-- V30: Token wallet system — accounts, purchases, transactions
-- ============================================================================

CREATE TABLE token_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    balance         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    account_type    VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (account_type IN ('USER', 'SYSTEM')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One system account per tenant
CREATE UNIQUE INDEX idx_token_accounts_system_per_tenant
    ON token_accounts(tenant_id) WHERE account_type = 'SYSTEM';

CREATE INDEX idx_token_accounts_tenant ON token_accounts(tenant_id);

CREATE TABLE token_purchases (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id          UUID NOT NULL REFERENCES token_accounts(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    payment_provider    VARCHAR(50),
    payment_reference   VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_purchases_account ON token_purchases(account_id);

CREATE TABLE token_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES token_accounts(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL CHECK (type IN ('PURCHASE_CREDIT', 'SPEND', 'REFUND', 'ADMIN_GRANT', 'TRANSFER_IN', 'TRANSFER_OUT')),
    amount          NUMERIC(12,2) NOT NULL,
    balance_after   NUMERIC(12,2) NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_transactions_account_time ON token_transactions(account_id, created_at DESC);
