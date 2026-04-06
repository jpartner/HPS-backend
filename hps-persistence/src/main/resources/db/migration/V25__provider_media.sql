-- Evolve gallery images into full media system
ALTER TABLE provider_gallery_images RENAME TO provider_media;
ALTER INDEX idx_provider_gallery_provider RENAME TO idx_provider_media_provider;

-- Media type: GALLERY (profile pics), VERIFICATION (ID docs), AVATAR
ALTER TABLE provider_media ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'GALLERY';

-- MIME type for distinguishing image vs video
ALTER TABLE provider_media ADD COLUMN content_type VARCHAR(50);

-- Approval workflow (existing rows are already public, so default APPROVED)
ALTER TABLE provider_media ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE provider_media ADD COLUMN review_note TEXT;
ALTER TABLE provider_media ADD COLUMN reviewed_at TIMESTAMPTZ;
ALTER TABLE provider_media ADD COLUMN reviewed_by UUID REFERENCES users(id);

-- Privacy and blur flags
ALTER TABLE provider_media ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE provider_media ADD COLUMN blur_requested BOOLEAN NOT NULL DEFAULT false;

-- New uploads will default to PENDING
-- Existing rows keep APPROVED since they were already public

CREATE INDEX idx_provider_media_approval ON provider_media(approval_status);
CREATE INDEX idx_provider_media_type ON provider_media(provider_id, media_type);
