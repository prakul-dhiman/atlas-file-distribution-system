-- V9: OTP table for email-protected share links and brute-force tracking

ALTER TABLE shared_links ADD COLUMN IF NOT EXISTS require_email_otp BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS share_otp_codes (
    id BIGSERIAL PRIMARY KEY,
    shared_link_id BIGINT NOT NULL REFERENCES shared_links(id) ON DELETE CASCADE,
    email VARCHAR(100) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_otp_link_email ON share_otp_codes(shared_link_id, email);
CREATE INDEX IF NOT EXISTS idx_otp_expires ON share_otp_codes(expires_at);
