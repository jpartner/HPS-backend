CREATE TABLE countries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    iso_code        VARCHAR(2) NOT NULL UNIQUE,
    phone_prefix    VARCHAR(5),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE country_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id      UUID NOT NULL REFERENCES countries(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    UNIQUE (country_id, lang)
);

CREATE TABLE regions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id      UUID NOT NULL REFERENCES countries(id),
    code            VARCHAR(20),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_regions_country ON regions(country_id);

CREATE TABLE region_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    region_id       UUID NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    UNIQUE (region_id, lang)
);

CREATE TABLE cities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    region_id       UUID NOT NULL REFERENCES regions(id),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    population      INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cities_region ON cities(region_id);

CREATE TABLE city_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id         UUID NOT NULL REFERENCES cities(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    UNIQUE (city_id, lang)
);

CREATE TABLE areas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id         UUID NOT NULL REFERENCES cities(id),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_areas_city ON areas(city_id);

CREATE TABLE area_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id         UUID NOT NULL REFERENCES areas(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    UNIQUE (area_id, lang)
);
