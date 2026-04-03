CREATE TABLE service_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id       UUID REFERENCES service_categories(id),
    icon            VARCHAR(100),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_category_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id     UUID NOT NULL REFERENCES service_categories(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    UNIQUE (category_id, lang)
);

CREATE TABLE services (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id),
    category_id     UUID NOT NULL REFERENCES service_categories(id),
    pricing_type    VARCHAR(20) NOT NULL,
    price_amount    NUMERIC(10,2) NOT NULL,
    price_currency  VARCHAR(3) NOT NULL DEFAULT 'EUR',
    duration_minutes INTEGER,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_services_provider ON services(provider_id);
CREATE INDEX idx_services_category ON services(category_id);

CREATE TABLE service_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id      UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    UNIQUE (service_id, lang)
);

CREATE TABLE service_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id      UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
