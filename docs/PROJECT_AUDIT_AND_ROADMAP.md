# Project Atlas: Audit and Professional Roadmap

Smart Distributed File Distribution System (SDFDS) is intended to be a production-grade cloud-storage style platform: authenticated users upload files, files are chunked and replicated across storage nodes, data can be searched/shared/restored, and the architecture remains extensible for future S3-backed storage, frontend workflows, and deployment automation.

This document records the current implementation status, gaps, and the recommended build plan.

## Current Status

### Implemented

- Spring Boot backend skeleton with layered packages for controllers, services, repositories, storage, security, schedulers, DTOs, mappers, and exceptions.
- JWT authentication with registration, login, refresh-token rotation, logout, and password reset token storage.
- BCrypt password hashing and role-based authority mapping.
- User profile endpoints and user DTO mapping.
- Core folder and file operations: upload, download, metadata, rename, move, copy, soft delete.
- Folder hierarchy support and recursive folder soft delete.
- Distributed storage abstraction with chunk metadata, replicas, storage node registry, simulated local nodes, replication, checksum tracking, and node recovery scheduler.
- Sharing module with public links, optional password protection, expiry, access limits, HMAC-signed download URLs, and share revocation.
- Search module for active files and folders.
- Recycle bin module with restore, purge, empty, and scheduled 30-day cleanup.
- File versioning for duplicate uploads, latest-version listing/search, version history, and version restore.
- Flyway schema for core domain tables, storage tables, share links, logs, notifications, and roles.
- ADR documents for earlier phases.
- Docker Compose scaffold.

### Partially Implemented

- Password reset exists as token generation, but no email delivery integration.
- Email verification is represented in the user model, but no verification-token flow exists yet.
- Roles exist, but fine-grained permission enforcement is not yet applied to admin/user endpoints.
- Activity, audit, and notification tables exist, but services are not yet writing events.
- Redis is configured, but caching/rate-limiting/session features are not yet implemented.
- OpenAPI annotations exist on many endpoints, but not all request/response flows are documented deeply.
- Tests exist for several modules, but Phase 4 and security edge cases need broader coverage.

### Not Yet Implemented

- Frontend React/Vite/Tailwind application.
- Chunk upload session API, resumable uploads, parallel multipart upload, and upload progress tracking.
- Advanced search filters by type, size, date, owner, and shared status.
- Favorites, recent files, file previews, shared-with-me, storage analytics, and user activity timeline.
- Request ID logging, structured audit logging, rate limiting, malware-scan extension point, and production CORS policy.
- CI/CD pipeline.
- Complete README, deployment guide, API guide, ER diagram, sequence diagrams, and environment documentation.
- Production object-storage adapter such as AWS S3.

## Authentication and Security Audit

### Strengths

- Passwords are stored with BCrypt.
- Access tokens are short-lived by default.
- Refresh tokens are persisted and rotated.
- Security filter chain is stateless.
- Controllers are thin and delegate business logic to services.
- Public share download URLs are short-lived and HMAC-signed.
- Public share routes are explicitly permitted, while the rest of the API requires authentication.

### Required Improvements

- Add email verification flow before marking `isEmailVerified=true`.
- Replace password reset log-only behavior with an email provider abstraction.
- Move JWT secret and all credentials fully to environment variables in deployment.
- Add authentication rate limiting for login, refresh, password reset, and public share password attempts.
- Add request correlation IDs and structured security logs.
- Add audit events for login, logout, password reset, share creation, signed-download creation, and failed access attempts.
- Restrict CORS origins per environment instead of using wildcard patterns.
- Add method-level role checks for admin endpoints.
- Add file upload validation: allowed mime types, max size enforcement, extension policy, and malware-scan hook.

## Professional Build Plan

### Phase 5: Security Hardening and Observability

Goal: make the backend safer and easier to debug.

Files/modules:
- `security/`
- `config/`
- `exception/`
- `service/AuditLogService`
- `service/NotificationService`
- `repository/AuditLogRepository`
- `repository/NotificationRepository`

Work:
- Add request ID filter and include IDs in logs.
- Add audit-log service and write security/file/share events.
- Add rate limiting using Redis.
- Add role checks for admin node endpoints.
- Add environment-driven CORS configuration.
- Add email verification and email sender abstraction.
- Add tests for auth failures, disabled users, refresh-token rotation, and share password failures.

### Phase 6: Upload Platform Upgrade

Goal: move from basic upload plus internal chunking to real client-visible large-file upload workflows.

Work:
- Add upload sessions table/entity.
- Add endpoints to initiate upload, upload chunk, resume upload, complete upload, and cancel upload.
- Validate chunk checksums and final checksum.
- Support parallel chunk upload.
- Add upload progress and retry-safe idempotency keys.
- Keep storage-node selection behind the existing storage abstraction.

### Phase 7: Cloud Drive Features

Goal: complete the feature set expected from a cloud file manager.

Work:
- Recent files.
- Favorites.
- Shared-with-me.
- Storage analytics.
- Advanced search filters.
- File preview metadata for images, PDFs, video, and audio.
- Notification generation for upload/share/version/storage events.

### Phase 8: Frontend Application

Goal: build the actual user-facing dashboard.

Recommended screens:
- Login/register/password reset.
- Dashboard overview.
- Folder explorer with breadcrumbs and context menu.
- Upload manager with progress states.
- Search results.
- Recycle bin.
- Shared links.
- Storage nodes admin dashboard.
- Profile and settings.

Tech:
- React, TypeScript, Vite, Tailwind CSS, React Query, Axios, React Router.

### Phase 9: Production Readiness

Goal: make the project interview-ready and deployment-ready.

Work:
- Add GitHub Actions for test/build.
- Add Dockerfile for backend and frontend.
- Add NGINX reverse proxy config.
- Add environment variable documentation.
- Add database indexes for frequent queries.
- Add API examples.
- Add ER diagram, sequence diagrams, and architecture diagram.
- Add S3-compatible storage adapter behind `StorageNode`.

## Immediate Next Sprint

Recommended order:

1. Fix build environment by installing JDK 17 or JDK 21 and setting `JAVA_HOME`.
2. Run `backend\maven\apache-maven-3.9.16\bin\mvn.cmd test`.
3. Add Phase 4 tests for share links, recycle bin, search, and version history.
4. Implement request ID logging and audit logging.
5. Implement email verification and proper password reset email delivery.
6. Start frontend skeleton only after backend security and file APIs are stable.

## Definition of Done

A feature is complete only when:

- Controller is thin and delegates to a service.
- Business logic is covered by service tests.
- Security behavior is tested.
- API response format is consistent.
- Swagger annotations exist.
- Errors return correct HTTP status codes.
- Storage/database side effects are verified.
- Documentation is updated.

