CREATE TABLE provider_gallery_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    storage_key     VARCHAR(500),
    caption         VARCHAR(500),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_provider_gallery_provider ON provider_gallery_images(provider_id);
