-- System-wide reference lists (nationalities, languages, etc.)
CREATE TABLE reference_lists (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key        VARCHAR(100) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reference_list_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_list_id UUID NOT NULL REFERENCES reference_lists(id) ON DELETE CASCADE,
    value             VARCHAR(100) NOT NULL,
    sort_order        INTEGER NOT NULL DEFAULT 0,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (reference_list_id, value)
);

CREATE TABLE reference_list_item_translations (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES reference_list_items(id) ON DELETE CASCADE,
    lang    VARCHAR(5) NOT NULL,
    label   VARCHAR(255) NOT NULL,
    UNIQUE (item_id, lang)
);

-- Allow attributes to link to a reference list instead of inline options
ALTER TABLE attribute_definitions ADD COLUMN reference_list_id UUID REFERENCES reference_lists(id);
