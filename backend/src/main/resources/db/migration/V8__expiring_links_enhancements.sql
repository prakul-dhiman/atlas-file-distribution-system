-- V8: Expiring Links Enhancements

ALTER TABLE shared_links ADD COLUMN IF NOT EXISTS expire_after_first_download BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_sl_expires_active ON shared_links(expires_at, is_active);
