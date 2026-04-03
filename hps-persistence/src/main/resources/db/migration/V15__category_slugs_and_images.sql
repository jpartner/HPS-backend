ALTER TABLE service_categories ADD COLUMN slug VARCHAR(100);
ALTER TABLE service_categories ADD COLUMN image_url VARCHAR(500);
CREATE UNIQUE INDEX idx_category_slug ON service_categories(slug) WHERE slug IS NOT NULL;
