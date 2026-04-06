-- Allow multiple conversation threads between the same two users.
-- Previously a unique index forced only one conversation per participant pair + type.
DROP INDEX IF EXISTS idx_conversations_participants;

-- Add a regular index for lookup performance
CREATE INDEX idx_conversations_participant1 ON conversations(participant1_id);
CREATE INDEX idx_conversations_participant2 ON conversations(participant2_id);
