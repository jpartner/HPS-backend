CREATE TABLE bookings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id      UUID NOT NULL REFERENCES services(id),
    client_id       UUID NOT NULL REFERENCES users(id),
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id),
    status          VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    scheduled_at    TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER,
    price_amount    NUMERIC(10,2) NOT NULL,
    price_currency  VARCHAR(3) NOT NULL,
    location_lat    DOUBLE PRECISION,
    location_lng    DOUBLE PRECISION,
    address_text    VARCHAR(500),
    client_notes    TEXT,
    provider_notes  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bookings_client ON bookings(client_id);
CREATE INDEX idx_bookings_provider ON bookings(provider_id);
CREATE INDEX idx_bookings_status ON bookings(status);

CREATE TABLE booking_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    old_status      VARCHAR(30),
    new_status      VARCHAR(30) NOT NULL,
    changed_by      UUID NOT NULL REFERENCES users(id),
    reason          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
