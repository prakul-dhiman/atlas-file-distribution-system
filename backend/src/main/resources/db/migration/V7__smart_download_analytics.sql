-- V7: Smart Download Analytics extension for share_download_events

ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS user_name VARCHAR(100);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS country_code VARCHAR(10);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS device VARCHAR(50);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS browser VARCHAR(50);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS operating_system VARCHAR(50);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS download_speed_bps BIGINT;
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS download_duration_ms BIGINT;
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS referer VARCHAR(512);
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'SUCCESS';
ALTER TABLE share_download_events ADD COLUMN IF NOT EXISTS is_unique BOOLEAN DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_sde_country ON share_download_events(country_code);
CREATE INDEX IF NOT EXISTS idx_sde_status ON share_download_events(status);
CREATE INDEX IF NOT EXISTS idx_sde_device ON share_download_events(device);
