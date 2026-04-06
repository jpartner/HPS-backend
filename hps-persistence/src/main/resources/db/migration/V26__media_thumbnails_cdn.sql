-- Thumbnails
ALTER TABLE provider_media ADD COLUMN thumbnail_url VARCHAR(500);
ALTER TABLE provider_media ADD COLUMN thumbnail_storage_key VARCHAR(500);

-- CDN publishing
ALTER TABLE provider_media ADD COLUMN cdn_url VARCHAR(500);
ALTER TABLE provider_media ADD COLUMN cdn_status VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- File metadata
ALTER TABLE provider_media ADD COLUMN file_size_bytes BIGINT;

CREATE INDEX idx_provider_media_cdn ON provider_media(cdn_status) WHERE cdn_status != 'LOCAL';
