-- V11: Enhanced Audit Log + Branding settings

-- Enrich audit_logs with resource tracking
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource_type VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource_id BIGINT;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS geo_country VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS geo_city VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS severity VARCHAR(20) DEFAULT 'INFO';

CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Branding / branded share portal settings (per user)
CREATE TABLE IF NOT EXISTS user_branding (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    brand_name VARCHAR(100),
    logo_url VARCHAR(512),
    primary_color VARCHAR(10) DEFAULT '#6366f1',
    accent_color VARCHAR(10) DEFAULT '#8b5cf6',
    background_color VARCHAR(10) DEFAULT '#0f172a',
    welcome_message TEXT,
    support_email VARCHAR(100),
    show_powered_by BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
