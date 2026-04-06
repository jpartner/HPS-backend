-- Add approval workflow status to provider profiles
ALTER TABLE provider_profiles
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL';

-- Set existing verified providers to APPROVED
UPDATE provider_profiles SET approval_status = 'APPROVED' WHERE is_verified = true;

-- Index for filtering by approval status
CREATE INDEX idx_provider_profiles_approval ON provider_profiles(tenant_id, approval_status);
