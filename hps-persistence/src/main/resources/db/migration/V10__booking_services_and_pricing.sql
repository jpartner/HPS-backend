-- Line items for each booking (which services were selected)
CREATE TABLE booking_services (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    service_id      UUID NOT NULL REFERENCES services(id),
    service_title   VARCHAR(255) NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 1,
    unit_price      NUMERIC(10,2) NOT NULL,
    line_total      NUMERIC(10,2) NOT NULL,
    duration_minutes INTEGER
);

CREATE INDEX idx_booking_services_booking ON booking_services(booking_id);

-- Track original calculated price vs provider's quoted price
ALTER TABLE bookings
    ADD COLUMN original_amount NUMERIC(10,2),
    ADD COLUMN total_duration_minutes INTEGER;

-- Update status to support QUOTED state
-- (status is already VARCHAR so no migration needed for enum values)
