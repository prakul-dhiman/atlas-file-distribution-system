# ADR-0005: Phase 4 Sharing, Search, Recycle Bin, and Versioning Design

- **Status**: Accepted
- **Date**: 2026-07-30
- **Context**: The Smart Distributed File Distribution System (SDFDS) requires secure sharing (with password protection and signed download links), name-based search, recycle bin operations with 30-day auto-purge, and file versioning support.

## Design Rationale

### 1. Recycle Bin & Soft Deletion
- Items (Files/Folders) are soft-deleted by setting `is_trashed = true` and `trashed_at = CURRENT_TIMESTAMP`.
- Trashed items are excluded from normal file listings and search results.
- A daily scheduled job (`RecycleBinCleanupScheduler`) scans for items where `is_trashed = true` and `trashed_at` is older than 30 days. Trashed folders are permanently purged, recursively deleting subfolders and files from both the DB and physical local/chunk storage.
- A user can list, restore, or permanently purge items. Restoring a folder restores all contained items. If a parent folder of a restored item is still trashed, the item is restored to the root folder.

### 2. File Versioning
- File versioning is supported without schema additions by leveraging the existing `files` table columns (`name`, `folder_id`, `version`).
- When a file with the same name is uploaded to the same folder, the system finds the highest existing version $V$, and saves the new file with version $V+1$.
- Standard file listings (`GET /api/v1/folders`) and search results only return the latest version of each file.
- Endpoints allow listing version history (`GET /api/v1/files/{id}/versions`), downloading a specific version, and restoring an older version to be the latest version (by copying it as version $V_{max}+1$).

### 3. Share Links & Signed URLs
- Users can share files/folders via `shared_links` with configurable expiry (default 7 days, max 90 days) and optional password protection.
- To access a shared file/folder, the client requests a signed download link. The system generates a short-lived (5-minute TTL) signed download URL containing an HMAC-SHA256 signature calculated from the link token, expiration time, and JWT secret key.
- Shared link downloads increment `access_count` and respect `max_access_count` limit if set.

### 4. Unified Search
- A search API (`GET /api/v1/search`) allows case-insensitive keyword searching across the user's active (non-trashed) files and folders.

## Files Touched/Created
- `backend/src/main/java/com/sdfds/entity/SharedLink.java`
- `backend/src/main/java/com/sdfds/repository/SharedLinkRepository.java`
- `backend/src/main/java/com/sdfds/dto/SharedLinkDto.java`
- `backend/src/main/java/com/sdfds/dto/CreateSharedLinkRequest.java`
- `backend/src/main/java/com/sdfds/dto/VerifySharePasswordRequest.java`
- `backend/src/main/java/com/sdfds/service/ShareService.java`
- `backend/src/main/java/com/sdfds/service/impl/ShareServiceImpl.java`
- `backend/src/main/java/com/sdfds/controller/ShareController.java`
- `backend/src/main/java/com/sdfds/service/SearchService.java`
- `backend/src/main/java/com/sdfds/service/impl/SearchServiceImpl.java`
- `backend/src/main/java/com/sdfds/controller/SearchController.java`
- `backend/src/main/java/com/sdfds/controller/RecycleBinController.java`
- `backend/src/main/java/com/sdfds/scheduler/RecycleBinCleanupScheduler.java`
- `backend/src/main/java/com/sdfds/service/impl/FileServiceImpl.java` (updated for versioning and latest version queries)
- `backend/src/main/java/com/sdfds/service/impl/FolderServiceImpl.java` (updated for restore/purge actions)
