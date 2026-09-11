-- V10: Granular RBAC and Direct User Item Sharing

CREATE TABLE IF NOT EXISTS direct_item_shares (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT REFERENCES files(id) ON DELETE CASCADE,
    folder_id BIGINT REFERENCES folders(id) ON DELETE CASCADE,
    shared_with_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    granted_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(20) NOT NULL DEFAULT 'VIEWER', -- VIEWER, EDITOR, ADMIN
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_direct_share_item CHECK (
        (file_id IS NOT NULL AND folder_id IS NULL) OR
        (file_id IS NULL AND folder_id IS NOT NULL)
    ),
    CONSTRAINT uq_direct_share_user_file UNIQUE (shared_with_user_id, file_id),
    CONSTRAINT uq_direct_share_user_folder UNIQUE (shared_with_user_id, folder_id)
);

CREATE INDEX IF NOT EXISTS idx_direct_shares_user ON direct_item_shares(shared_with_user_id);
CREATE INDEX IF NOT EXISTS idx_direct_shares_granted_by ON direct_item_shares(granted_by_user_id);
