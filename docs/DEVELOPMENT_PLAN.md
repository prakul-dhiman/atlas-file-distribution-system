# SDFDS — Project Atlas: Professional Development Plan

> **Status:** Active — Phase 5 Security Hardening
> **Date:** 2026-08-01

This document is the living engineering roadmap for the Smart Distributed File Distribution System (SDFDS).

---

## 1. Executive Summary

The Smart Distributed File Distribution System (SDFDS / *Project Atlas*) is a Spring Boot 3 distributed cloud-storage platform. The backend currently implements **Phases 0–4**: JWT authentication, core file/folder CRUD, chunked & replicated distributed storage, sharing with signed URLs, search, recycle bin, and file versioning. The database schema, Docker infrastructure, and CI pipeline are scaffolded.

The most critical gaps are:

1. **Security hardening** — email verification, rate limiting, request-ID logging, audit trails, role-based authorization, restricted CORS.
2. **Upload platform upgrade** — client-visible resumable/parallel chunked uploads with progress.
3. **The entire React frontend** — no UI exists today.
4. **Production-readiness documentation** — README, deployment guide, architecture/ER/sequence diagrams, S3 adapter, NGINX.

This plan defines a professional, incremental delivery strategy so the project becomes interview-ready, deployable, and demonstrably enterprise-grade. Every phase is gated by tests and documentation updates.

---

## 2. Current State Assessment

### 2.1 What Exists Today (Verified by Code Review)

| Area | Status | Details |
|---|---|---|
| **Backend skeleton** | ✅ Complete | Layered packages: controller, service, service/impl, repository, entity, dto, mapper, security, exception, storage, scheduler, config |
| **Authentication** | ✅ Core | Register, login, refresh-token rotation, logout, password-reset tokens, BCrypt(12), 15-min access / 7-day refresh tokens |
| **Authorization** | ⚠️ Partial | Roles exist; no @PreAuthorize method-level enforcement yet |
| **Email verification** | ❌ Missing | is_email_verified column exists; no flow |
| **File management** | ✅ Complete | Upload, download streaming, rename, move, copy, soft-delete |
| **Versioning** | ✅ Complete | Per-folder+name version history and restore |
| **Distributed storage** | ✅ Complete | 8 MB chunks, replication ×2, SHA-256 checksums, 3 simulated nodes, capacity-aware selection, failover read, recovery scheduler, DOWN detection |
| **Sharing** | ✅ Complete | Public links, HMAC-SHA256 signed URLs (5 min), password, expiry, access limits, revoke, folder scope validation |
| **Search** | ✅ Basic | By name for active files/folders |
| **Recycle bin** | ✅ Complete | Restore, purge, empty, 30-day scheduled cleanup |
| **Database schema** | ✅ Complete | Flyway V1: users, roles, permissions, tokens, folders, files, chunks, storage_nodes, replicas, shared_links, activity_logs, audit_logs, notifications |
| **Infrastructure** | ✅ Scaffolded | Docker Compose (PG16 + Redis 7), backend Dockerfile, GitHub Actions CI |
| **Tests** | ✅ Baseline | Auth, File, Folder, ChunkDistribution, NodeFailureRecovery, AuthController, HealthController |
| **Frontend** | ❌ Missing | No React application exists |

### 2.2 Architectural Strengths

- **Clean layered architecture** — thin controllers; business logic in services; storage behind `StorageNode` interface.
- **SOLID & DDD-aligned** — single-responsibility services, repository abstraction, domain entities.
- **Extensible storage** — adding AWS S3/GCS/new nodes requires no business-logic changes.
- **Security-conscious** — short-lived access tokens, rotated refresh tokens, HMAC-signed public downloads, BCrypt, stateless API.
- **Operationally aware** — Flyway migrations, health checks, schedulers, SLF4J logging.

---

## 3. Gap Analysis & Prioritized Backlog

### 3.1 Critical — Security & Compliance (Phase 5)

| # | Gap | Impact |
|---|---|---|
| 5.1 | No request-ID correlation in logs | Hard to trace failures across requests |
| 5.2 | No audit-log service | Security events unrecorded despite audit_logs table |
| 5.3 | No activity/notification services | activity_logs & notifications tables unused |
| 5.4 | No rate limiting | Login/refresh/reset/share-password brute force |
| 5.5 | No email verification flow | Accounts usable without verified email |
| 5.6 | Password reset never emails the user | Feature non-functional beyond token storage |
| 5.7 | No method-level authorization | Admin node endpoints unprotected |
| 5.8 | CORS wildcard `*` + credentials | Insecure for production |
| 5.9 | No upload validation | Arbitrary MIME types/extensions/sizes |
| 5.10 | No malware-scan extension point | No AV hook |

### 3.2 High — Upload Platform (Phase 6)

| # | Gap | Impact |
|---|---|---|
| 6.1 | No session-based resumable upload | Large-file UX is single-request only |
| 6.2 | No parallel chunk upload | Slow for multi-GB files |
| 6.3 | No upload progress tracking | Users cannot see progress |
| 6.4 | No idempotency keys | Retry-safe uploads impossible |
| 6.5 | Small-file direct path bypasses chunk metadata | Inconsistent distributed invariants |

### 3.3 Medium — Cloud Drive Features (Phase 7)

- Recent files, favorites, shared-with-me, storage analytics, file previews (image/PDF/video/audio), advanced search filters (type/size/date), duplicate detection, notification generation.

### 3.4 High — Frontend (Phase 8)

- Entire React + TypeScript + Vite + Tailwind SPA: auth screens, dashboard, folder explorer, upload manager, search, recycle bin, shares, admin nodes dashboard, profile/settings.

### 3.5 Production Readiness (Phase 9)

- README, architecture diagram, ER diagram, sequence diagrams, API examples, deployment + Docker guides, environment-variable reference, NGINX, S3-compatible storage adapter, frontend Dockerfile, CD workflow.

---

## 4. Delivery Roadmap

### Phase 5 — Security Hardening & Observability *(in progress)*

**Goal:** Production-safe authentication, diagnosable system, enforced RBAC.

**Scope:**
1. `RequestIdFilter` — generate/carry request ID in MDC (`X-Request-ID` header).
2. `AuditLog` entity + repository + `AuditLogService` — write login, logout, password-reset, share, download, failed-access events.
3. `ActivityLog` / `Notification` entities + services — record user activity; emit notifications (upload complete, share created, storage full).
4. Redis-backed rate limiting — `/auth/login`, `/auth/refresh`, `/auth/password-reset/*`, share-password attempts.
5. Email verification — `EmailVerificationToken` entity, `EmailService` interface + console (dev) implementation, verify endpoint.
6. Method security — `@PreAuthorize("hasRole('ROLE_ADMIN')")` on admin endpoints.
7. Environment-driven CORS — restrict origins via `app.cors.allowed-origins`.
8. Upload validation — MIME/extension allowlist + size guard in `UploadPolicy`.
9. Malware-scan extension point — `MalwareScanner` interface (stub).
10. Tests — auth failures, refresh rotation, share-password brute force, request-ID propagation, audit writing.

**Definition of Done (Phase 5):**
- [ ] Security events produce audit rows.
- [ ] Login/refresh/reset reject >N attempts/min via Redis.
- [ ] New users are unverified until email confirmation.
- [ ] Admin endpoints return 403 for non-admin tokens.
- [ ] CORS origins configurable per environment; wildcard removed in prod.
- [ ] `mvn test` green including new security cases.

### Phase 6 — Upload Platform Upgrade

**Goal:** Real client-visible large-file workflows with progress and resilience.

**Scope:**
1. `upload_sessions` table (id, user, file metadata, chunk size, total chunks, uploaded chunks, status, checksum).
2. Endpoints: `POST /uploads` (init), `PUT /uploads/{id}/chunks/{index}`, `GET /uploads/{id}` (progress), `POST /uploads/{id}/complete`, `DELETE /uploads/{id}` (cancel).
3. Per-chunk checksum validation + final assembled-file verification.
4. Parallel chunk uploads with idempotency keys.
5. Stream chunk reads (avoid full-file byte buffer); storage-node selection behind `StorageNode` interface.

### Phase 7 — Cloud Drive Features

- Favorites, recent files, shared-with-me, storage analytics, advanced search filters, file-preview metadata, duplicate detection, notification generation on events.

### Phase 8 — Frontend Application

- **Stack:** React 18, TypeScript, Vite, Tailwind CSS, React Query, Axios, React Router.
- **Screens:** Login/Register/Reset; Dashboard (storage usage, recent, shared, activity); Folder Explorer (breadcrumbs, context menus, drag-drop); Upload Manager (progress, pause/resume, parallel); Search; Recycle Bin; Shares; Admin Nodes; Profile/Settings.
- **Design:** minimal, professional, responsive, dark mode, glassmorphism (sparingly), loading skeletons, toast notifications.
- **Auth flow:** persist access+refresh tokens; axios interceptor auto-refresh on 401.

### Phase 9 — Production Readiness

- README.md, `docs/ARCHITECTURE.md`, `docs/ER_DIAGRAM.md`, `docs/SEQUENCE_DIAGRAMS.md`, `docs/API.md`, `docs/DEPLOYMENT.md`, `docs/ENV_VARS.md`.
- NGINX reverse-proxy config (gzip, caching, client_max_body_size).
- S3-compatible `StorageNode` adapter.
- Frontend Dockerfile + multi-service `docker-compose.yml`.
- CD workflow (build image, push registry, deploy).

---

## 5. Engineering Standards

1. **Clean Architecture** — controller → service → repository/storage; no business logic in controllers.
2. **SOLID** — single responsibility per class; depend on interfaces (`StorageNode`, `EmailService`, `AuditLogService`).
3. **DTOs never leak entities** — MapStruct mappers between entity ↔ DTO.
4. **Consistent API contract** — `ApiResponse<T>` envelope, proper HTTP status codes, API versioning `/api/v1`.
5. **Security by default** — BCrypt, JWT, RBAC, signed URLs, rate limits, audit events, no secrets in code.
6. **Testability** — services unit-tested with Mockito; security paths integration-tested; storage failover tested.
7. **No hardcoded values** — constants + enums + externalized config.
8. **Observability** — SLF4J structured logs with request IDs; never log passwords/credentials.
9. **Git hygiene** — conventional commits: `feat(auth): …`, `fix(upload): …`, `refactor(storage): …`.
10. **Documentation alongside code** — every feature updates README/API docs.

---

## 6. Verification Strategy

| Level | Method |
|---|---|
| Unit | Mockito service tests (auth, file, folder, sharing, upload sessions) |
| Integration | `@SpringBootTest` + H2/Testcontainers for security filter chain, JWT flow |
| Storage failover | Simulate node DOWN → recovery scheduler re-replicates chunk |
| Security | Rate-limit exhaustion, brute-force share password, expired/rotated tokens, RBAC 403s |
| CI | GitHub Actions: `mvn clean test` on PostgreSQL 16 + Redis 7 services |
| Manual/E2E | Swagger UI (`/swagger-ui.html`) + React frontend against local stack |

---

## 7. Immediate Next Actions

1. **Phase 5 implementation** (current sprint) — request-ID logging, audit/notification services, rate limiting, email verification, RBAC, env CORS, upload validation.
2. **Verify build** with `mvn clean test` — requires a local JDK 21 (see §8) or use the bundled maven wrapper with a Docker JDK image.
3. **Phase 6 upload sessions** — resumable/parallel chunk upload API.
4. **Phase 8 frontend scaffold** — React + Vite + Tailwind SPA against the local backend.
5. **Phase 9 documentation & deployment** — README, diagrams, NGINX, S3 adapter, CD workflow.

---

## 8. Environment Note

Current machine audit: **no JDK was detected** on PATH, in `C:\Program Files\Java`, Eclipse Adoptium, `.jdks`, scoop, or `LOCALAPPDATA\Programs`.

To run tests locally:
1. Install Temurin JDK 21 (or set `JAVA_HOME` to an existing JDK 17+/21).
2. `docker compose up -d postgres redis` starts the runtime dependencies.
3. Run `backend\maven\apache-maven-3.9.16\bin\mvn.cmd clean test`.

Docker Desktop and kubectl are present; Git is available. Without a local JDK, the GitHub Actions pipeline remains the authoritative test gate.

---

*Living document — updated as each phase lands.*
