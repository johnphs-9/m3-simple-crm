# Build Handoff — m3-simple-crm · PT2: Secret & Credential Externalisation

- **Date/time:** 2026-10-30 21:25 +08
- **Agent:** Build (GLM-5.3-Flash)
- **Trigger:** two explicit user requests (no new requirements source): (1) "move `jwt.secret` and `jwt.expiration-ms` to `application-dev.properties` and secret key value to `.env`"; (2) "AuthController VALID_USERNAME and VALID_PASSWORD to be taken from application.properties and the values from .env".
- **Build plan of record:** `docs/handoffs/m3-simple-crm-pt2-jwt-auth-build-plan-Qwen3.8-27B-MLX-4bit-2026-10-30T13-07.md` (PT2 J1–J16); this handoff **amends** its implementation decisions D1/D4/R4.
- **Prior review:** `docs/reviews/m3-simple-crm-pt2-jwt-auth-review-glm-5.3-flash-free-2026-10-30T20-19.md` + `docs/reviews/2026-10-30_test-review-summary-m3-simple-crm-pt2.md` (39/39 evidence confirmed).

## What changed (all paths relative to repo root `m3-simple-crm/`)

### 1. `m3-simple-crm/src/main/resources/application.properties`
- Removed the assignment-pt2 `jwt.secret` / `jwt.expiration-ms` lines (and their comments). The now-former literal secret `+WVaxfdqg/...` is **no longer in any tracked file**.
- Added `spring.config.import=optional:file:.env[.properties]` — loads the module-root `.env` (gitignored) as properties for **all** profiles (dev + test), with a comment mapping where each JWT setting now lives.

### 2. `m3-simple-crm/src/main/resources/application-dev.properties` (appended)
```properties
jwt.secret=${APP_JWT_SECRET}
jwt.expiration-ms=3600000
app.auth.username=${APP_USER_USERNAME}
app.auth.password=${APP_USER_PASSWORD}
```
- `APP_JWT_SECRET`, `APP_USER_*` **already existed** in the user's `.env` (and in `.env.example` placeholders). The user's pre-existing `.env` values were **not overwritten**; the app now sources the secret from `APP_JWT_SECRET` (41 chars, ≥32 bytes for HS256) and the login creds from `APP_USER_USERNAME=user` / `APP_USER_PASSWORD=password` — preserving the assignment's "user"/"password" simple-check behavior.

### 3. `m3-simple-crm/src/test/resources/application-test.properties` (appended)
```properties
jwt.secret=${TEST_JWT_SECRET:1234567890abcdef...64chars}
jwt.expiration-ms=3600000
app.auth.username=${APP_USER_USERNAME:user}
app.auth.password=${APP_USER_PASSWORD:password}
```
- Deterministic defaults so CI/test runs don't require `.env`; when `.env` is present, `TEST_JWT_SECRET` / `APP_USER_*` (which exist there) override. Keeps `HelloApiSecurityTest` (which reflects `jwtSecret` and forges an expired token against the app's own secret) self-consistent.

### 4. `m3-simple-crm/src/main/java/sg/edu/ntu/m3_simple_crm/auth/AuthController.java`
- `static final VALID_USERNAME/VALID_PASSWORD` removed → constructor-injected `@Value("${app.auth.username}")` / `@Value("${app.auth.password}")` fields `validUsername` / `validPassword`. The **simple fixed-check mechanism is unchanged** (assignment L25); only the literal values were externalised. Javadoc updated.

### 5. `JwtService` (no code change)
- Still `@Value("${jwt.secret}")` / `@Value("${jwt.expiration-ms}")`; its class Javadoc still says "from application.properties" — **stale comment, one-line doc fix pending** (values now come from profile files + `.env`).

## Checks actually run (both green)

1. **Full suite** — `./mvnw test` from module root, 21:17 +08: aggregate from fresh surefire artifacts: **Tests run: 39, Failures: 0, Errors: 0, Skipped: 0** (7 test classes; 23 pt1 baseline + 16 pt2). Verified by parsing `target/surefire-reports/*.txt` (all timestamps 21:17).
2. **Live dev-profile smoke test** (the profile tests don't cover; proves `.env` wiring works end-to-end):
   - app started via `./mvnw spring-boot:run`; `GET /health` → **200**
   - `POST /auth/login` `user`/`password` → **200 + token** (creds resolved from `.env` via `APP_USER_*`)
   - `GET /api/hello` with `Authorization: Bearer <token>` → **200** + `Hello! JWT authentication successful.`
   - `GET /api/hello` no token → **401**; `POST /auth/login` wrong password → **401**
   - App killed afterwards; port 8080 verified free.

## Outstanding items

1. **TESTING.md (J15 artifact) — RESOLVED 21:30 +08.** It had been deleted from the working tree (untracked); it was restored verbatim from the reviewer's copy at `m3-simple-crm/TESTING.md`, with the config-location sections updated for the externalisation (J3/J9 rows point to the profile files + `.env`, deviation 3 added, R4/D1 notes rewritten, `.env` prerequisite added to the reproduce script, 21:17 re-confirmation recorded).
2. **`JwtService` Javadoc one-liner** (L18) — RESOLVED: updated to reference the profile property files and the gitignored `.env` (comment-only change made after this handoff section was drafted).
3. **`git status` cleanup note:** `application.properties`, `application-dev.properties`, `application-test.properties`, `AuthController.java`, `.env.example` are modified; `.env` itself is properly ignored (my `git status` check confirms it is not listed). Commit/push (J16) still awaiting user confirmation; the working tree also contains the uncommitted pt1 changes described in prior handoffs (D10/R5) — preserve them in the commit order previously proposed (pt1 first, then pt2 + this refactor).
4. **Fresh-clone UX:** without a `.env`, the dev profile fails with unresolved-placeholder errors — intentional (strict `${APP_JWT_SECRET}` / `${APP_USER_*}`). `.env.example` documents every required variable; consider adding one sentence to a README if one exists.

## Requirement-coverage impact

J3 and J9 behavior and acceptance outcomes are **unchanged** (same property keys `jwt.secret`/`jwt.expiration-ms`, same 3600000 expiry, same `user`/`password` check, same 200/401 contract) — only storage location moved. Assignment L13–15 names `application.properties` literally; this is a **user-approved deviation** from a literal reading, trading grading-literalism for secret hygiene (and resolving the R4 security smell the reviews had flagged). Test evidence: 39/39 + live smoke (see above).
