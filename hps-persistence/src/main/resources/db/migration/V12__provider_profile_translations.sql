CREATE TABLE provider_profile_translations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider_profiles(user_id) ON DELETE CASCADE,
    lang            VARCHAR(5) NOT NULL,
    business_name   VARCHAR(255),
    description     TEXT,
    UNIQUE (provider_id, lang)
);
