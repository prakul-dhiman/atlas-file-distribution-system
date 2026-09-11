# ADR-0003: Phase 2 Core File CRUD and Local Single-Node Storage Design

- **Status**: Accepted
- **Date**: 2026-07-27
- **Context**: The Smart Distributed File Distribution System (SDFDS) requires core file management capabilities (upload, download, rename, move, copy, delete) and nested folder hierarchy operations using single-node local disk storage before layering Phase 3 distributed chunking.

## Design Rationale
1. **Local Disk Storage Abstraction**: Files are saved to a configurable disk directory (`storage.local-dir`, defaulting to `./storage-data/`). Pathing follows standard isolation per user: `{localDir}/{userId}/{fileId}`.
2. **Streaming IO**: Direct file upload and download operations stream data directly via `InputStream` / `Resource` / `StreamingResponseBody` with digest calculation (`DigestInputStream` for SHA-256) to ensure low heap consumption and high throughput for files up to 5GB.
3. **Storage Quotas**: Each upload checks user storage quota (`storage_quota_bytes`) against current consumption (`used_storage_bytes`). Quota is updated atomically upon successful file write.
4. **Soft Deletion & Cascade Policy**: Deleting a folder or file marks `is_trashed = true` and records `trashed_at`. Cascading soft-deletion traverses the folder sub-tree without deleting physical storage bytes (permanent purge is deferred to recycle bin garbage collection).

## Acceptance Criteria Satisfied
1. Authenticated user can create, list, rename, move, and soft-delete nested folder hierarchies.
2. Authenticated user can upload, stream download, copy, move, rename, and soft-delete single files up to 5GB.
3. SHA-256 checksum is calculated and stored during stream upload.
4. File download responses support standard content-disposition and content-length HTTP headers.
