-- ============================================================================
-- V27: Country currencies, rate duration presets, and provider rate cards
-- ============================================================================

-- Country-to-currency mapping (1:1 extension of countries)
CREATE TABLE country_currencies (
    country_id         UUID PRIMARY KEY REFERENCES countries(id) ON DELETE CASCADE,
    primary_currency   VARCHAR(3) NOT NULL,
    secondary_currency VARCHAR(3)
);

-- Admin-managed duration options per tenant
CREATE TABLE rate_duration_presets (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    rate_type        VARCHAR(20) NOT NULL,
    duration_minutes INT NOT NULL,
    sort_order       INT NOT NULL DEFAULT 0,
    is_active        BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (tenant_id, rate_type, duration_minutes)
);

CREATE INDEX idx_rate_duration_presets_tenant ON rate_duration_presets(tenant_id);

-- Provider rate card entries
CREATE TABLE provider_rates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id      UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    rate_type        VARCHAR(20) NOT NULL,
    duration_minutes INT,
    primary_amount   NUMERIC(10,2) NOT NULL,
    secondary_amount NUMERIC(10,2),
    is_custom_duration BOOLEAN NOT NULL DEFAULT false,
    sort_order       INT NOT NULL DEFAULT 0,
    is_active        BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_provider_rates_unique
    ON provider_rates(provider_id, rate_type, COALESCE(duration_minutes, -1));

CREATE INDEX idx_provider_rates_provider ON provider_rates(provider_id);

-- ============================================================================
-- Seed: European country currencies (conditional on country existing)
-- ============================================================================

-- Eurozone countries: EUR primary, no secondary
INSERT INTO country_currencies (country_id, primary_currency)
SELECT id, 'EUR' FROM countries WHERE iso_code IN (
    'AT','BE','CY','DE','EE','ES','FI','FR','GR','HR',
    'IE','IT','LT','LU','LV','MT','NL','PT','SI','SK'
)
ON CONFLICT DO NOTHING;

-- Non-eurozone European countries: local primary, EUR secondary
INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'PLN', 'EUR' FROM countries WHERE iso_code = 'PL'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'CZK', 'EUR' FROM countries WHERE iso_code = 'CZ'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'GBP', 'EUR' FROM countries WHERE iso_code = 'GB'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'SEK', 'EUR' FROM countries WHERE iso_code = 'SE'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'DKK', 'EUR' FROM countries WHERE iso_code = 'DK'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'NOK', 'EUR' FROM countries WHERE iso_code = 'NO'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'HUF', 'EUR' FROM countries WHERE iso_code = 'HU'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'RON', 'EUR' FROM countries WHERE iso_code = 'RO'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'BGN', 'EUR' FROM countries WHERE iso_code = 'BG'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'CHF', 'EUR' FROM countries WHERE iso_code = 'CH'
ON CONFLICT DO NOTHING;

INSERT INTO country_currencies (country_id, primary_currency, secondary_currency)
SELECT id, 'UAH', 'EUR' FROM countries WHERE iso_code = 'UA'
ON CONFLICT DO NOTHING;
