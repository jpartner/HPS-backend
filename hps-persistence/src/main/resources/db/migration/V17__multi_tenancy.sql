-- Tenants table
CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(255),
    default_lang    VARCHAR(5) NOT NULL DEFAULT 'en',
    supported_langs VARCHAR(100) NOT NULL DEFAULT 'en',
    default_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    settings        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tenant-admin assignments
CREATE TABLE tenant_admins (
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (tenant_id, user_id)
);

-- Insert default tenant for existing data
INSERT INTO tenants (id, slug, name, default_lang, supported_langs, default_currency)
VALUES ('00000000-0000-0000-0000-000000000001', 'default', 'Default Marketplace', 'en', 'en,pl,uk,de', 'EUR');

-- Add tenant_id to all tenant-scoped tables
ALTER TABLE users ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001';
-- tenant_id nullable for SUPER_ADMIN users
CREATE INDEX idx_users_tenant ON users(tenant_id);

-- Email unique per tenant (drop global unique, add tenant-scoped)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX idx_users_email_tenant ON users(tenant_id, email) WHERE tenant_id IS NOT NULL;
-- SUPER_ADMIN emails still globally unique
CREATE UNIQUE INDEX idx_users_email_superadmin ON users(email) WHERE tenant_id IS NULL;

ALTER TABLE service_categories ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE service_categories SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE service_categories ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_categories_tenant ON service_categories(tenant_id);
-- Slug unique per tenant
DROP INDEX IF EXISTS idx_category_slug;
CREATE UNIQUE INDEX idx_category_slug_tenant ON service_categories(tenant_id, slug) WHERE slug IS NOT NULL;

ALTER TABLE service_templates ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE service_templates SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE service_templates ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_templates_tenant ON service_templates(tenant_id);
ALTER TABLE service_templates DROP CONSTRAINT IF EXISTS service_templates_slug_key;
DROP INDEX IF EXISTS service_templates_slug_key;
CREATE UNIQUE INDEX idx_template_slug_tenant ON service_templates(tenant_id, slug);

ALTER TABLE attribute_definitions ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE attribute_definitions SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE attribute_definitions ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_attributes_tenant ON attribute_definitions(tenant_id);
ALTER TABLE attribute_definitions DROP CONSTRAINT IF EXISTS attribute_definitions_domain_key_key;
CREATE UNIQUE INDEX idx_attribute_domain_key_tenant ON attribute_definitions(tenant_id, domain, key);

ALTER TABLE provider_profiles ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE provider_profiles SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE provider_profiles ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_providers_tenant ON provider_profiles(tenant_id);

ALTER TABLE services ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE services SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE services ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_services_tenant ON services(tenant_id);

ALTER TABLE bookings ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE bookings SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE bookings ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_bookings_tenant ON bookings(tenant_id);

ALTER TABLE conversations ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE conversations SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE conversations ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE reviews ADD COLUMN tenant_id UUID REFERENCES tenants(id);
UPDATE reviews SET tenant_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE reviews ALTER COLUMN tenant_id SET NOT NULL;
