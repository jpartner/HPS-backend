CREATE TABLE provider_schedule_settings (
    provider_id         UUID PRIMARY KEY REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    timezone            VARCHAR(50) NOT NULL DEFAULT 'Europe/Berlin',
    incall_gap_minutes  INTEGER NOT NULL DEFAULT 15,
    outcall_gap_minutes INTEGER NOT NULL DEFAULT 60,
    min_lead_time_hours INTEGER NOT NULL DEFAULT 2,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE provider_weekly_slots (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_weekly_time_range CHECK (start_time < end_time),
    CONSTRAINT chk_weekly_30min_start CHECK (EXTRACT(MINUTE FROM start_time) IN (0, 30)),
    CONSTRAINT chk_weekly_30min_end CHECK (EXTRACT(MINUTE FROM end_time) IN (0, 30))
);

CREATE INDEX idx_weekly_slots_provider ON provider_weekly_slots(provider_id);
CREATE UNIQUE INDEX idx_weekly_slots_unique ON provider_weekly_slots(provider_id, day_of_week, start_time);

CREATE TABLE provider_date_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    override_date   DATE NOT NULL,
    is_unavailable  BOOLEAN NOT NULL DEFAULT false,
    start_time      TIME,
    end_time        TIME,
    reason          VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_override_times CHECK (
        (is_unavailable = true AND start_time IS NULL AND end_time IS NULL) OR
        (is_unavailable = false AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    ),
    CONSTRAINT chk_override_30min_start CHECK (start_time IS NULL OR EXTRACT(MINUTE FROM start_time) IN (0, 30)),
    CONSTRAINT chk_override_30min_end CHECK (end_time IS NULL OR EXTRACT(MINUTE FROM end_time) IN (0, 30))
);

CREATE INDEX idx_date_overrides_lookup ON provider_date_overrides(provider_id, override_date);

ALTER TABLE bookings ADD COLUMN booking_type VARCHAR(10) NOT NULL DEFAULT 'INCALL';

CREATE INDEX idx_bookings_provider_schedule
    ON bookings(provider_id, scheduled_at)
    WHERE status IN ('CONFIRMED', 'IN_PROGRESS');
