-- ============================================================================
-- V29: Rework rate cards — meeting-type based with incall/outcall columns
-- ============================================================================

-- Pre-production: safe to truncate
TRUNCATE provider_rates;

-- Drop old unique index and columns
DROP INDEX IF EXISTS idx_provider_rates_unique;

ALTER TABLE provider_rates
  DROP COLUMN rate_type,
  DROP COLUMN primary_amount,
  DROP COLUMN secondary_amount,
  DROP COLUMN is_custom_duration,
  ADD COLUMN meeting_type_id UUID REFERENCES service_categories(id),
  ADD COLUMN label VARCHAR(100),
  ADD COLUMN incall_amount NUMERIC(10,2),
  ADD COLUMN outcall_amount NUMERIC(10,2),
  ADD COLUMN secondary_incall_amount NUMERIC(10,2),
  ADD COLUMN secondary_outcall_amount NUMERIC(10,2);

CREATE UNIQUE INDEX idx_provider_rates_unique
  ON provider_rates(provider_id, meeting_type_id, COALESCE(duration_minutes, -1));

-- Rate duration presets: drop rate_type, add label
TRUNCATE rate_duration_presets;
ALTER TABLE rate_duration_presets DROP CONSTRAINT IF EXISTS rate_duration_presets_tenant_id_rate_type_duration_minutes_key;
ALTER TABLE rate_duration_presets
  DROP COLUMN rate_type,
  ADD COLUMN label VARCHAR(100);
ALTER TABLE rate_duration_presets ADD CONSTRAINT rate_duration_presets_unique UNIQUE (tenant_id, duration_minutes);

-- Service extras: add min_duration_minutes
ALTER TABLE services ADD COLUMN min_duration_minutes INT;
