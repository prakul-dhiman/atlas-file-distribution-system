-- V4: Share download tracking and QR code support

ALTER TABLE shared_links ADD COLUMN IF NOT EXISTS qr_code_url TEXT;
ALTER TABLE shared_links ADD COLUMN IF NOT EXISTS last_accessed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS share_download_events (
    id BIGSERIAL PRIMARY KEY,
    shared_link_id BIGINT NOT NULL REFERENCES shared_links(id) ON DELETE CASCADE,
    file_id BIGINT REFERENCES files(id) ON DELETE SET NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    downloaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sde_shared_link ON share_download_events(shared_link_id);
CREATE INDEX IF NOT EXISTS idx_sde_downloaded_at ON share_download_events(downloaded_at);
