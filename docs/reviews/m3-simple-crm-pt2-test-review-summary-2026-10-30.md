# Test Review Summary — m3-simple-crm · PT2: JWT Authentication

- **Date/time:** 2026-10-30 20:42 +08
- **Reviewer:** GLM-5.3-Flash (requirements-to-code · Review mode — read-only for source; no source edited, no builds/tests executed)
- **Requirements source:** `/Users/johnphs/Documents/code/NTU-AIE/ai-4.2-secure-software-development-and-authentication-pt2/assignment.md` (63 lines; requirement IDs J1–J16 cited per build-plan mapping)
- **Build plan reviewed:** `docs/handoffs/m3-simple-crm-pt2-jwt-auth-build-plan-Qwen3.8-27B-MLX-4bit-2026-10-30T13-07.md` (811 lines, full)
- **Build status reviewed:** `docs/handoffs/m3-simple-crm-pt2-jwt-auth-steps7-12-status.md` (64 lines, full); `TESTING.md` (162 lines, full)
- **Prior review superseded partially here:** `docs/reviews/m3-simple-crm-pt2-jwt-auth-review-glm-5.3-flash-free-2026-10-30T20-19.md`
- **Scope:** Full PT2 scope — JWT issuance + `POST /auth/login` (§1), `JwtAuthFilter`, `SecurityConfig`, protected `GET /api/hello` (§2), automated + manual testing artifacts (J1–J16). Focus of this pass: **test evidence**.

## Verdict

No functional defect was established. All code requirements J1–J15 are implemented as specified, and automated test evidence now exists as recorded artifacts (39/39 passing). Remaining exposure is process/submission, not correctness: dependency-resolution evidence (V3), the uncommitted working tree (V4), minor doc drift, and untranscribed manual-matrix results (V2).

## Requirement coverage (verified against source)

| ID | Requirement (source line) | Status | Evidence (verified by reading source) |
|----|---------------------------|--------|----------------------------------------|
| J1 | Use existing project (L8) | ✅ | PT2 built on the pt1 tree; pt1 classes/tests untouched |
| J2 | jjwt-api/impl/jackson 0.11.5 in `pom.xml` (L9–12) | ✅ | `pom.xml` L83–99: api (compile), impl (runtime), jackson (runtime) |
| J3 | `jwt.secret` + `jwt.expiration-ms=3600000` (L13–15) | ✅ | `application.properties` L6–7; 64-char base64 secret (≥ 32 bytes for HS256) |
| J4 | `LoginRequest` DTO (L16–17) | ✅ | `auth/LoginRequest.java` L13–16 |
| J5 | `TokenResponse` DTO (L18) | ✅ | `auth/TokenResponse.java` L13–15 |
| J6 | `generateToken(String)` — subject + expiry (L20) | ✅ | `security/JwtService.java` L35–44 (sub + iat + exp, HS256) |
| J7 | `extractUsername(String)` (L21) | ✅ | `JwtService.java` L47–49 |
| J8 | `isTokenValid(String)` — signature + expiry (L22) | ✅ | `JwtService.java` L52–59; `JwtException | IllegalArgumentException` → false; expired/malformed/tampered all fail parse |
| J9 | `POST /auth/login`; simple check `user`/`password`; 200 token else 401 (L23–27) | ✅ | `auth/AuthController.java` L21–22, L36–43 (spec-mandated hard-coded check, plan D1) |
| J10 | Postman login test, record token (L28–31) | ✅ documented | `TESTING.md` §3 rows 2–3 (live run recorded) |
| J11 | `JwtAuthFilter` extends `OncePerRequestFilter`; Bearer extract; valid → set auth; invalid/missing → continue chain (L34–38) | ✅ | `security/JwtAuthFilter.java` L34, L44–62 — always `chain.doFilter`, never writes error response |
| J12 | CSRF disabled; STATELESS; `/auth/login` permitAll; `/api/**` authenticated; filter before `UsernamePasswordAuthenticationFilter` (L39–45) | ✅ | `config/SecurityConfig.java` L45–46, L48–49, L58; pt1 rules + `httpBasic` retained per plan D3 |
| J13 | `GET /api/hello` → "Hello! JWT authentication successful." (L46–47) | ✅ | `controller/HelloController.java` L15–18; plain text accepted as literal spec reading (prior F2) |
| J14 | Full flow: no token → 401; login → token; Bearer → 200 (L48–51) | ✅ documented + automated | `HelloApiSecurityTest` (5 tests, real filter chain) mirrors the flow; manual matrix in `TESTING.md` §3 all PASS |
| J15 | Document testing results (L52) | ✅ | `m3-simple-crm/TESTING.md` — automated table, manual matrix, how-to-reproduce, caveats |
| J16 | Submit repo URL; credit sources (L56–57) | ⏳ user action | Credits section present (`TESTING.md` §5); code is **uncommitted** — see V4 |

## Test evidence (this pass' new verification)

1. **Surefire artifacts verified, not just claimed.** `m3-simple-crm/target/surefire-reports/` contains reports for all 7 test classes. Per-class results (from the `.txt`/XML reports, all `failures=0 errors=0 skipped=0`):

   | Test class | Tests |
   |------------|-------|
   | `controller/AuthControllerTest` | 4 |
   | `controller/CustomerControllerTest` | 9 |
   | `controller/HelloApiSecurityTest` | 5 |
   | `M3SimpleCrmApplicationTests` | 1 |
   | `security/JwtServiceTest` | 7 |
   | `service/CustomerServiceImplTest` | 3 |
   | `service/DemoServiceTest` | 10 |
   | **Total** | **39** |

   **Result: Tests run: 39, Failures: 0, Errors: 0, Skipped: 0** — matches the claim in `TESTING.md` §2 and the steps 7–12 status handoff.

2. **Artifact timestamps close the prior review's V1/V5 gaps.** Reports are timestamped **20:29 +08** — *after* the prior review (20:20) and *after* the F1 Javadoc fix to `JwtAuthFilter.java` (20:24). The passing evidence therefore covers the **current source state**, including the mid-build SecurityConfig rewrite.

3. **F1 fix verified applied.** The `JwtAuthFilter` class Javadoc (L26–29) now reads "invalid or unhandled token: no authentication set; context cleared only if validation threw — always continues the chain", matching actual behavior (clearContext only in the exception path, L57–58). No behavior change was made or claimed.

4. **Test content matches the claims.** All three new test classes match the plan's code (§10–12) with the documented deviations: the Boot 4 `AutoConfigureMockMvc` import (D8), the `assertTrue` import note, and the `Collection<? extends GrantedAuthority>` return type in `authoritiesFor` (compilation fix, recorded in `TESTING.md` §2 deviations).

## Residual informational findings (carried from prior review, re-confirmed)

- **F2/Informatory — `/api/hello` returns `text/plain`**: literal reading of L47; documented in `TESTING.md` §4 (R6). Accepted.
- **F3/Security smell — committed `jwt.secret` + hard-coded `user`/`password`** (`application.properties` L6; `AuthController.java` L21–22): spec-mandated (L14, L25), documented in `TESTING.md` §4. Must be flagged as a known vulnerability before any real deployment.
- **F4/Informatory — filter ordering**: `addFilterBefore(UsernamePasswordAuthenticationFilter)` places `JwtAuthFilter` after `BasicAuthenticationFilter`; mixed Basic+Bearer headers resolve in favor of Basic first. Coherent with D3.
- **F5/Informatory — weak assertion**: `AuthControllerTest` L35 `assertTrue(json.split("\"").length > 3, ...)` is redundant behind the real `jsonPath("$.token").isNotEmpty()` check (L33). Cosmetic.

## Remaining verification gaps (updated status)

1. **V3 — dependency-resolution evidence still missing.** Plan acceptance for J2/R1 asked for `./mvnw dependency:tree -Dincludes=io.jsonwebtoken` (and Jackson resolved versions). No handoff records this output. Expected benign (jjwt 0.11.5 core is servlet/Jakarta-independent; artifacts cached in `~/.m2`), but it remains unrecorded. *Still open.*
2. **V4 — code is uncommitted (confirmed by `git status` inspection).** All pt1+pt2 work is uncommitted on `main` at HEAD `d995479` ("3.19 Assignment…", the earlier JWT experiment). Proposed two-commit plan (pt1 state, then pt2) not executed; J16 submission blocked. *Still open.*
3. **V2 — manual matrix transcript not stored in repo.** `TESTING.md` §3 records a 7-row all-PASS live matrix with a captured JWT (internally consistent: `sub=user`, HS256, iat/exp 3,600,000 ms apart), but no raw transcript/Postman export exists in the repo. Corroborated, not reproducible from artifacts. *Still open.*
4. **Minor doc drift — `TESTING.md`/status handoff cite the 20:01 run**; the current passing evidence is the 20:29 surefire run (same totals). No handoff references the 20:29 artifacts. *New, trivial.*

## Build / user handoff (next actions)

1. **Record dependency evidence (V3):** run `./mvnw dependency:tree -Dincludes=io.jsonwebtoken,com.fasterxml.jackson.core` once and paste the summaries into a short pt2 completion handoff under `docs/handoffs/`.
2. **Doc drift (trivial):** add one line to `TESTING.md` §2 citing the 20:29 surefire run as the current passing evidence (timestamps updated after reviewer).
3. **Commit & push (V4/J16), with user confirmation only:** commit pt1 state, then pt2, push to `origin/main`; complete the Blackboard submission and fill the credits placeholder in `TESTING.md` §5.
4. **Optional (V2):** store the manual-matrix transcript or a Postman collection export in the repo so J15 evidence travels with the submission.

## Limits of this review

Static review of source vs. the extracted assignment; surefire reports and git state were inspected read-only via shell listing/extraction (no Maven build, test run, or HTTP execution performed by the reviewer this pass). Test outcomes are evidenced by recorded artifacts (item 1–2 above) rather than re-executed. No rendering/inspection tools were needed; all evidence was textual.
