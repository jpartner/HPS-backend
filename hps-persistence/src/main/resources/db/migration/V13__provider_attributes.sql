-- Attribute definitions: what attributes exist per service domain
CREATE TABLE attribute_definitions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain          VARCHAR(100) NOT NULL,
    key             VARCHAR(100) NOT NULL,
    data_type       VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    is_required     BOOLEAN NOT NULL DEFAULT false,
    options         JSONB,
    validation      JSONB,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (domain, key)
);

-- Translations for attribute labels, hints, option labels
CREATE TABLE attribute_definition_translations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attribute_id        UUID NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    lang                VARCHAR(5) NOT NULL,
    label               VARCHAR(255) NOT NULL,
    hint                VARCHAR(500),
    option_labels       JSONB,
    UNIQUE (attribute_id, lang)
);

-- Provider attribute values stored as JSONB
ALTER TABLE provider_profiles ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}';

CREATE INDEX idx_provider_attributes ON provider_profiles USING GIN (attributes);
CREATE INDEX idx_attribute_definitions_domain ON attribute_definitions(domain);
