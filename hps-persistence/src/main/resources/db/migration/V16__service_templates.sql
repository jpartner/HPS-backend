-- System-level service templates that providers select from
CREATE TABLE service_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id     UUID NOT NULL REFERENCES service_categories(id),
    slug            VARCHAR(100) NOT NULL UNIQUE,
    default_duration_minutes INTEGER,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_template_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID NOT NULL REFERENCES service_templates(id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    UNIQUE (template_id, lang)
);

-- Provider services now reference a template
ALTER TABLE services ADD COLUMN template_id UUID REFERENCES service_templates(id);

CREATE INDEX idx_service_templates_category ON service_templates(category_id);
