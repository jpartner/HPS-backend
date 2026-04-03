CREATE TABLE provider_categories (
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    category_id     UUID NOT NULL REFERENCES service_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (provider_id, category_id)
);

CREATE INDEX idx_provider_categories_category ON provider_categories(category_id);
