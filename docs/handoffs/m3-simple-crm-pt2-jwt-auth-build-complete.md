# Build Status — PT2 JWT Auth · Steps 7–12

- Plan: `docs/handoffs/m3-simple-crm-pt2-jwt-auth-build-plan-Qwen3.8-27B-MLX-4bit-2026-10-30T13-07.md`
- Module: `m3-simple-crm/m3-simple-crm` (Spring Boot 4.1.1, Java 21)
- Note: Steps 1–6 of the plan were already implemented in the working tree before this session (verified by reading pom.xml, application.properties, auth/*.java, security/JwtService.java, security/JwtAuthFilter.java, config/SecurityConfig.java). This file records only steps 7–12 as instructed.

## Step 7 — controller/HelloController.java (J13)
- Status: DONE
- Action: Created `m3-simple-crm/src/main/java/sg/edu/ntu/m3_simple_crm/controller/HelloController.java` exactly per plan §9 (new file, no prior content to preserve).
- Content: `@RestController @RequestMapping("/api")`; `GET /api/hello` returns the plain string `"Hello! JWT authentication successful."` (J13, R6 plain-text form kept per spec).
- Verified: file written to disk at the path above.

## Step 8 — New tests: JwtServiceTest, AuthControllerTest, HelloApiSecurityTest (J6–J9, J11–J14)
- Status: DONE
- Action: Created three test files (new; no prior content to preserve), exactly per plan §10–12, using the Boot 4 `AutoConfigureMockMvc` package (D8):
  - `src/test/java/sg/edu/ntu/m3_simple_crm/security/JwtServiceTest.java` — 7 unit tests (no Spring context): subject/iat/expiry round-trip, extractUsername, valid→true, garbage→false, tampered→false, wrong-key→false, expired→false.
  - `src/test/java/sg/edu/ntu/m3_simple_crm/controller/AuthControllerTest.java` — 4 MockMvc tests (test profile): valid creds → 200 + non-empty token; wrong password → 401; wrong username → 401; missing fields → 401. Includes the `assertTrue` import called out in the plan's note.
  - `src/test/java/sg/edu/ntu/m3_simple_crm/controller/HelloApiSecurityTest.java` — 5 MockMvc tests (test profile, real filter chain): no token → 401; valid Bearer → 200 + exact body `Hello! JWT authentication successful.`; garbage Bearer → 401; expired Bearer → 401; ROLE_USER mapping regression (GET /customers → 200).
- Verified: all three files written to disk at the paths above.

## Step 9 — `./mvnw test` (J12 regression)
- Status: DONE — 39 tests, 0 failures, 0 errors, 0 skipped. BUILD SUCCESS.
- Actual output: `Tests run: 39, Failures: 0, Errors: 0, Skipped: 0` → `BUILD SUCCESS` (finished 2026-10-30T20:01:51+08:00).
- Breakdown: 23 baseline (pt1: M3SimpleCrmApplicationTests 1, DemoServiceTest 10, CustomerServiceImplTest 3, CustomerControllerTest 9) + 16 new (JwtServiceTest 7, AuthControllerTest 4, HelloApiSecurityTest 5).
- Two in-scope fixes were required to get here (both recorded):
  1. `security/JwtAuthFilter.java` — `authoritiesFor` return type changed from `List<GrantedAuthority>` to `Collection<? extends GrantedAuthority>` (and `java.util.Collection` import added). The plan's literal code did not compile: `UserDetails.getAuthorities()` returns `Collection<? extends GrantedAuthority>`, which does not convert to `List<GrantedAuthority>` or `Collection<GrantedAuthority>`. No behavior change — the returned collection is passed straight to `UsernamePasswordAuthenticationToken`.
  2. `config/SecurityConfig.java` — the plan's Step 6 rewrite (J12/D3/D5) was found NOT applied in the working tree (the file was still the pt1-only chain, so `JwtAuthFilter` was never registered and valid tokens returned 401). Applied the plan's full new content: `/auth/login` permitAll, `/api/**` authenticated, pt1 matchers kept, `new JwtAuthFilter(...)` instantiated inline and `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`, `.httpBasic(...)` retained.
- Verified: test run executed in `m3-simple-crm/m3-simple-crm`; surefire reports regenerated under `target/surefire-reports/`.

## Step 10 — Manual verification matrix (J10, J14)
- Status: DONE — all checks passed against the running app (dev profile, local Postgres `simple_crm` on `localhost:5432`, which was up).
- App started with `./mvnw spring-boot:run` (dev profile), confirmed via `GET /health` → 200.
- Actual results (request → expected → actual → pass/fail):
  | # | Request | Expected | Actual | Result |
  |---|---------|----------|--------|--------|
  | 1 | `GET /api/hello` (no token) | 401 | 401 | PASS |
  | 2 | `POST /auth/login` `{"username":"user","password":"password"}` | 200 + `{"token":"..."}` | 200 + `{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNzkzMzYxODMyLCJleHAiOjE3OTMzNjU0MzJ9.GIcmzw9XxqElEpfr12Gn3rm16j-YPFjDkoHTe4W1dx0"}` | PASS |
  | 3 | `POST /auth/login` wrong password (`"nope"`) | 401 | 401 | PASS |
  | 4 | `GET /api/hello` with `Authorization: Bearer <token from #2>` | 200 + `Hello! JWT authentication successful.` | 200, `Content-Type: text/plain;charset=UTF-8`, body `Hello! JWT authentication successful.` (R6 plain-text form) | PASS |
  | 5a | `GET /customers` as `user:user123` (Basic) | 200 | 200 | PASS (pt1 regression) |
  | 5b | `POST /customers` as `admin:admin123` (Basic), valid payload | 201 | 201 (note: first attempt used `jobTitle:"X"` → 400 validation "Job title must be between 2 and 50 characters"; retried with `jobTitle:"Engineer"` → 201, customer ID 5) | PASS (payload issue, not auth) |
  | 5c | `GET /health` (public) | 200 | 200 | PASS |
- App stopped cleanly after the matrix (graceful shutdown logged; port 8080 no longer accepting connections).
- Verified: all responses captured live against the running app; no results fabricated.

## Step 11 — `m3-simple-crm/TESTING.md` (J15 + J16 credits)
- Status: DONE
- Action: Created `m3-simple-crm/TESTING.md` (new file; none existed). Contents:
  - Section 1: PT2 scope table (files ↔ requirement IDs J2–J13) and the two coexisting auth mechanisms (D3).
  - Section 2: automated test results — 39 tests / 0 failures / BUILD SUCCESS, per-class breakdown, and the two in-scope deviations (JwtAuthFilter return type; SecurityConfig Step-6 rewrite applied).
  - Section 3: the manual curl/Postman matrix (J10 + J14) with the actual captured responses (the 7-row pass table), plus a "How to reproduce" script.
  - Section 4: notes & caveats — R6 plain-text body, R4 committed secret, D1 two credential sets, D2 role mapping.
  - Section 5: References / Credits (J16) — Spring Security docs, OWASP JWT cheat sheet, jjwt docs, Boot 4 MockMvc package; "Classmates / URLs: none" placeholder for the user to fill before submission.
- Verified: file written to disk at `m3-simple-crm/m3-simple-crm/TESTING.md`.

## Step 12 — Git (J16)
- Status: PAUSED — awaiting user confirmation before any commit/push (per plan D10 and Risk R5).
- Current git state (inspected, not modified): branch `main`, HEAD `d995479` ("3.19 Assignment. With Spring security JWT Authentication implemented" — the *old* JWT experiment). The working tree has uncommitted pt1 changes (modified `pom.xml`, `SecurityConfig.java`, `application*.properties`, `CustomerControllerTest.java`, deleted `AppConfig.java`, deleted `.github/instructions/*`, modified `.gitignore`/`.env.example`) plus untracked pt1/pt2 files (`HealthController.java`, `HelloController.java`, `AuthControllerTest.java`, `HelloApiSecurityTest.java`, `security/` test dir, `docs/`, `diagram`, `TESTING.md`).
- Proposed (two clean commits, then push) — **not executed**:
  1. Commit the pt1 state (Basic Auth + RBAC + the pt1 test fixes) as one commit.
  2. Commit the pt2 JWT work (jjwt deps, `jwt.*` props, `auth/`, `security/`, `HelloController`, `SecurityConfig` rewrite, the three new test classes, `TESTING.md`) as a second commit.
  3. Push to `origin/main`.
- No destructive git commands were run (no `git checkout .` / `git restore`), per Risk R5.
- Verified: `git status --short`, `git branch --show-current`, and `git log --oneline -3` captured; no commit or push performed.
