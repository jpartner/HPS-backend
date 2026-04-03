-- Make conversations generic to support different party types
ALTER TABLE conversations
    DROP CONSTRAINT conversations_client_id_provider_id_key,
    ADD COLUMN conversation_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER_PROVIDER',
    ADD COLUMN topic VARCHAR(255);

ALTER TABLE conversations RENAME COLUMN client_id TO participant1_id;
ALTER TABLE conversations RENAME COLUMN provider_id TO participant2_id;

-- participant1 is always the initiator
CREATE UNIQUE INDEX idx_conversations_participants
    ON conversations(participant1_id, participant2_id, conversation_type);

-- User blocking
CREATE TABLE user_blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(blocker_id, blocked_id),
    CHECK(blocker_id != blocked_id)
);

CREATE INDEX idx_user_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_user_blocks_blocked ON user_blocks(blocked_id);
