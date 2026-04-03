-- service_id on bookings is no longer required; services are tracked via booking_services
ALTER TABLE bookings ALTER COLUMN service_id DROP NOT NULL;
