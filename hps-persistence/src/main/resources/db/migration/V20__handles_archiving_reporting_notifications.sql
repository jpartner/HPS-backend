-- User handles (unique per tenant)
ALTER TABLE users ADD COLUMN handle VARCHAR(30);
CREATE UNIQUE INDEX idx_users_handle_tenant ON users(tenant_id, handle)
    WHERE handle IS NOT NULL AND tenant_id IS NOT NULL;

-- Conversation archives (per-user soft archive)
CREATE TABLE conversation_archives (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    archived_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, conversation_id)
);

-- Message reports
CREATE TABLE message_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    reporter_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    admin_notes     TEXT,
    tenant_id       UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(message_id, reporter_id)
);
CREATE INDEX idx_message_reports_tenant_status ON message_reports(tenant_id, status);

-- Notification preferences (infra for Telegram etc.)
CREATE TABLE notification_preferences (
    user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    telegram_chat_id    VARCHAR(100),
    push_enabled        BOOLEAN NOT NULL DEFAULT false,
    email_enabled       BOOLEAN NOT NULL DEFAULT true,
    message_notify      BOOLEAN NOT NULL DEFAULT true,
    booking_notify      BOOLEAN NOT NULL DEFAULT true,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
