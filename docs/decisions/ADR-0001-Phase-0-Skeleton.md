# ADR-0001: Phase 0 Project Skeleton, Database Migrations, and Infrastructure

- **Status**: Accepted
- **Date**: 2026-07-27
- **Context**: The Smart Distributed File Distribution System (SDFDS) - Project Atlas requires a modular, production-ready foundation with standardized backend layering, automated DB schema versioning, containerized dependencies (PostgreSQL + Redis), and a build/CI pipeline.

## Decision
1. **Tech Stack & Framework**: Spring Boot 3.3.x with Java 21 LTS. Use Maven as the build tool for dependency lifecycle management.
2. **Database & Migrations**: PostgreSQL 16 managed via Flyway versioned migration scripts located in `src/main/resources/db/migration/`. JPA/Hibernate ddl-auto will be set to `validate` in production/dev profiles to ensure schema consistency.
3. **Caching & In-Memory Storage**: Redis 7 for user session management, rate limiting, and chunk upload caching.
4. **API Standards & Security**: All API routes follow the `/api/v1/` prefix. Spring Security secures operational endpoints while exposing `/api/v1/health` and Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`). Standard JSON responses wrapped in `ApiResponse<T>`.
5. **Containerization**: `docker-compose.yml` configures PostgreSQL 16 (port 5432) and Redis 7 (port 6379) with persistent named volumes and health check declarations.

## Consequences
- **Positive**: Strict schema control via Flyway prevents JPA auto-ddl drift. Modular layout cleanly isolates controllers, DTOs, services, entity models, and security filters.
- **Negative**: Initial setup requires Maven and Docker Compose present on developer machines for integration testing.
