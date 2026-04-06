CREATE TABLE provider_approval_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL,
    notes           TEXT,
    changed_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_approval_history_provider ON provider_approval_history(provider_id, created_at DESC);
