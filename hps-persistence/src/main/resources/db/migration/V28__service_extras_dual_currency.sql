ALTER TABLE services ADD COLUMN is_included BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE services ADD COLUMN primary_amount NUMERIC(10,2);
ALTER TABLE services ADD COLUMN secondary_amount NUMERIC(10,2);

-- Backfill existing rows
UPDATE services SET primary_amount = price_amount WHERE primary_amount IS NULL;
ALTER TABLE services ALTER COLUMN primary_amount SET NOT NULL;
