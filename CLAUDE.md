# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MCMIS (Marine Center Monitoring Information System) — a Spring Boot 3.5.16 **monolith** for marine center monitoring management. Front-end separated; this repo is the backend only. Java 17, IvorySQL 5.4 (PostgreSQL-compatible), Redis 7.x, MyBatis-Plus 3.5.12.

## Build & Run

```bash
# Start (dev profile active by default)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=GlobalExceptionHandlerTest

# Compile only (validates MapStruct/Lombok annotation processing)
./mvnw compile

# Package
./mvnw package -DskipTests
```

API docs at `http://localhost:8080/swagger-ui.html` after startup.

Dev database: `127.0.0.1:5432/lims`, user `ivorysql`, password `123456`. Init with `src/main/resources/db/init.sql`.
Dev Redis: `127.0.0.1:6380`, password `123456`.

## Architecture

**Package-by-feature under `com.shou.mcmis`:**

| Package | Purpose |
|---------|---------|
| `common/` | Shared config, BaseEntity, Result\<T\>, enums, exceptions, CacheService, validators |
| `security/` | JWT + Spring Security: AuthController, JwtTokenService, RefreshTokenService, RsaKeyService, JwtAuthFilter |
| `organize/` | RBAC system management: user, role, permission, dept, menu, log sub-modules |
| `sample/`, `project/`, `task/`, `instrument/`, `report/`, `customer/`, `contract/`, `workflow/`, `notification/`, `file/` | Business modules (empty skeletons for now) |

**Each sub-module** (e.g. `organize/user/`) follows a strict layered structure: `controller` → `service`/`impl` → `mapper` → `entity`, with separate `dto/`, `vo/`, `converter/` packages.

**Auth flow:** RSA-encrypted password transmission → BCrypt verification → JWT access token (15min, HS256) + refresh token (7 days, stored in Redis, rotation on use). Dev profile: if `keyId` is blank in login request, `cipherPwd` is treated as plaintext password (for Swagger debugging).

## Key Conventions (Non-Negotiable)

### Service Layer
- Custom Service interfaces — do **NOT** extend MyBatis-Plus `IService`
- Impl classes in `impl/` sub-package — do **NOT** extend `ServiceImpl`; hold Mapper references directly via constructor injection (`@RequiredArgsConstructor`)

### MyBatis-Plus Queries
- **Always** use `LambdaQueryWrapper` + method references: `.eq(User::getUsername, username)`
- **Never** use string column names: `.eq("username", username)` is forbidden

### Entities & Database
- All entities extend `BaseEntity` which provides: `createTime`, `updateTime`, `createBy`, `updateBy`, `isDelete` (logic delete), `version` (optimistic lock) — auto-filled via `MyMetaObjectHandler`
- Entity time fields use `LocalDateTime` (not `Date`)
- Database time columns use `TIMESTAMP` (not `TIMESTAMPTZ`) to match Java `LocalDateTime`

### API Layer
- `Result<T>` unified response wrapper: `Result.success(data)` / `Result.fail(code, message)`
- DTOs for input (`CreateDTO`, `UpdateDTO`, `QueryDTO`), VOs for output — never expose entities directly
- `@Valid` + Jakarta Validation on all controller request bodies
- Pagination: `PageHelper` offset-based for admin lists (<100k rows), cursor-based (`lastId`) for large/infinite-scroll datasets using `CursorPageVO`
- HTTP methods: GET (query), POST (create), PUT (update), DELETE (delete with JSON body `[ids]`)

### Other
- **Jackson** for JSON (not fastjson)
- Logging: `log.info("user {} logged in", username)` placeholder style only, never `+` concatenation
- `@PreAuthorize("hasAuthority('...')")` on every secured endpoint
- Permission naming: `{module}:{resource}:{action}` (e.g. `organize:user:add`)
- MapStruct converters: `@Mapper(componentModel = "spring")`, placed in each module's `converter/` package

## Development Workflow

New modules follow a 9-step process enforced by superpowers skills: brainstorming → writing-plans → TDD → executing-plans → verification → code-review → technical docs → memory → commit. Bug fixes follow a 7-step variant starting with systematic-debugging.

Technical decisions are documented in `docs/` (specs in `docs/superpowers/specs/`, plans in `docs/superpowers/plans/`).

## Important Files

- `src/main/resources/application.yml` — shared config (DB, Redis, Jackson, MyBatis-Plus, JWT)
- `src/main/resources/application-dev.yml` — dev profile overrides
- `src/main/resources/db/init.sql` — full DDL + seed data (4 departments, 4 roles, 4 users)
- `src/main/java/com/shou/mcmis/security/config/SecurityConfig.java` — security filter chain, public endpoints
- `src/main/java/com/shou/mcmis/common/exception/GlobalExceptionHandler.java` — unified exception → Result mapping
- `src/main/java/com/shou/mcmis/common/config/MyMetaObjectHandler.java` — auto-fills BaseEntity audit fields
