# Requirements & Code Review — m3-simple-crm · PT2: JWT Authentication

- **Date/time:** 2026-10-30 20:19 +08
- **Reviewer:** GLM-5.3-Flash (requirements-to-code · Review mode — read-only for source; no source edited, no builds/tests executed)
- **Build plan reviewed:** `docs/handoffs/m3-simple-crm-pt2-jwt-auth-build-plan-Qwen3.8-27B-MLX-4bit-2026-10-30T13-07.md` (811 lines, read in full)
- **Build status report reviewed:** `docs/handoffs/m3-simple-crm-pt2-jwt-auth-steps7-12-status.md` (64 lines, read in full)
- **Requirements source (read in full):** `/Users/johnphs/Documents/code/NTU-AIE/ai-4.2-secure-software-development-and-authentication-pt2/assignment.md` (63 lines; line citations below are into this file)
- **Also cross-checked:** `docs/handoffs/m3-simple-crm-pt2-jwt-auth-steps7-12-status.md` content vs. actual source; prior incremental review `docs/reviews/m3-simple-crm-pt2-jwt-auth-code-review-2026-10-30.md` (steps 1–2 reviewed there)
- **Scanned:** the incremental review's key warning (step-6 `SecurityConfig` not applied). That state is now superseded — `SecurityConfig.java` currently contains the full PT2 rewrite (verified by read, lines 33–61).

## Scope reviewed

Full PT2 scope: JWT issuance + `POST /auth/login` (§1), `JwtAuthFilter`, `SecurityConfig`, protected `GET /api/hello` (§2), automated and manual testing artifacts (J1–J16). Implementation claims were checked line-by-line against the assignment and the actual working tree. **No Maven/test/curl execution was performed** — test-run and manual-matrix results are reviewed as Build claims, not reproduced.

## Requirement coverage (verified against source)

| ID | Requirement (source line) | Status | Evidence (verified by reading source) |
|----|---------------------------|--------|----------------------------------------|
| J1 | Use existing project (§Brief L8) | ✅ | PT2 added on top of the pt1 tree; pt1 classes/tests untouched by reading |
| J2 | jjwt-api/impl/jackson 0.11.5 in `pom.xml` (L9–12) | ✅ | `pom.xml` L83–99: api (compile), impl (runtime), jackson (runtime) — exactly as specified |
| J3 | `jwt.secret` + `jwt.expiration-ms=3600000` (L13–15) | ✅ | `application.properties` L6–7; secret is 64 base64 chars (≥ 32 bytes for HS256); caveat noted under F3 |
| J4 | `LoginRequest` with username/password (L16–17) | ✅ | `auth/LoginRequest.java` L13–16 |
| J5 | `TokenResponse` with token (L18) | ✅ | `auth/TokenResponse.java` L13–15 |
| J6 | `generateToken(String username)` — subject + expiry (L20) | ✅ | `security/JwtService.java` L35–44 (sub + iat + `now + jwtExpirationMs`, HS256) |
| J7 | `extractUsername(String token)` (L21) | ✅ | `JwtService.java` L47–49 |
| J8 | `isTokenValid(String token)` — signature + expiry (L22) | ✅ | `JwtService.java` L52–59: jjwt parse throws on bad signature / expired / malformed → `false`; catches `JwtException \| IllegalArgumentException` |
| J9 | `POST /auth/login`, simple check `user`/`password`, 200 `TokenResponse` else 401 (L23–27) | ✅ | `auth/AuthController.java` L21–22, L36–43 — matches the spec-mandated hard-coded check (plan D1) |
| J10 | Test login with Postman and record token (L28–31) | ✅ documented | `TESTING.md` §3 rows 2–3; step-status handoff records a live run with a real captured token |
| J11 | `JwtAuthFilter` extends `OncePerRequestFilter`; Bearer extraction; valid → auth in context; invalid/missing → continue chain (L34–38) | ✅ | `security/JwtAuthFilter.java` L32, L43–61 — always `chain.doFilter`, never writes an error response |
| J12 | CSRF disabled, STATELESS, `/auth/login` permitAll, `/api/**` authenticated, JWT filter before `UsernamePasswordAuthenticationFilter` (L39–45) | ✅ | `config/SecurityConfig.java` L45–46, L48–49, L58 (pt1 rules and `httpBasic` retained per plan D3) |
| J13 | `GET /api/hello` → “Hello! JWT authentication successful.” (L46–47) | ✅ | `controller/HelloController.java` L15–18. Returns plain text, not JSON — see F2/risk R6 |
| J14 | Full flow: no token → 401; login → token; Bearer → 200 (L48–51) | ✅ documented | Automated mirror: `HelloApiSecurityTest` (5 tests incl. garbage/expired-token 401s). Manual matrix: `TESTING.md` §3 (7 rows, all PASS) |
| J15 | Document testing results (L52) | ✅ | `TESTING.md` (162 lines, read in full) — automated table, manual matrix, how-to-reproduce, caveats |
| J16 | GitHub repo URL to Blackboard; credit classmates/URLs (L56–57) | ⏳ user action | Credits section present (`TESTING.md` §5); **code is uncommitted** (step 12 paused awaiting user confirmation) — submission cannot happen until commit/push |

**Coverage verdict:** all code requirements J1–J13 are implemented as specified; no missing requirement, and no unauthorized feature beyond documented decisions D1–D3/D5 (role mapping, coexisting Basic Auth — both recorded in `TESTING.md`).

## Findings (prioritized; defects vs. informational)

**F1 — Low / documentation-accuracy defect · not a behavior risk**
`security/JwtAuthFilter.java` L27 (class Javadoc) states "If the token is missing or invalid, **clears any context** and continues the chain." The code only calls `SecurityContextHolder.clearContext()` in the exception path (L55–56), and never clears context when the token is present-but-invalid or when there is no Bearer header. Practical impact: none under the current chain — STATELESS session policy means each request enters with an empty context, and a Basic-authenticated request only co-occurs with a Bearer header in the mixed-header edge case (see F4). This is a misleading comment, not a defect; cheap to correct when convenient.

**F2 — Low / informational (accepted spec divergence, documented)**
`HelloController.java` L17 returns a raw `String` → `Content-Type: text/plain;charset=UTF-8`. A grader expecting JSON would see plain text. The plan (R6) and `TESTING.md` §4 both document this as the literal reading of assignment L47. Acceptance L51 only requires 200 OK, which the automated test `HelloApiSecurityTest.apiHello_withValidToken_returnsHello` asserts with the exact body. No change required; flagging so the user can consciously accept it before submission.

**F3 — Low / security smell (spec-mandated, accepted)**
`application.properties` L6 commits a fixed `jwt.secret` (and the repo is intended to be pushed publicly per J16). Also, `POST /auth/login` accepts the permanent hard-coded `user`/`password` (`AuthController.java` L21–22). Both are literal requirements of the assignment (L14, L25), already flagged in the plan (R4) and in `TESTING.md` §4. Accepted for this coursework; must be noted as a known vulnerability in any real deployment.

**F4 — Informational (mixed-header edge case, consistent by design)**
`JwtAuthFilter` is registered `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` (`SecurityConfig.java` L58), which places it *after* Spring's `BasicAuthenticationFilter` position in the standard ordering. With concurrent valid Basic + invalid Bearer headers, Basic wins; with valid Bearer + no Basic, JWT wins. Given decision D3 (two coexisting mechanisms, documented), behavior is coherent; recording it so it is not mistaken for a filter-ordering bug. No change.

**F5 — Informational (weak assertion)**
`AuthControllerTest.java` L35: `assertTrue(json.split("\"").length > 3, ...)` — a fragile surrogate for "token present". The real check is the preceding `jsonPath("$.token").isNotEmpty()` (L33). Cosmetic; no change required.

No high- or medium-severity defect was established. Every functional requirement maps to passing source-verified logic; the substantive review exposure is in the verification gaps below.

## Verification gaps (claims Build must evidence; Review cannot regenerate them)

**V1 — Test-run results not re-verifiable from artifacts.** The status handoff and `TESTING.md` claim **"Tests run: 39, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS"** (2026-10-30 20:01). `target/` does not exist in the module tree (no `surefire-reports/`, no compiled classes available for inspection), so this claim has no remaining artifact evidence. Review mode does not execute Maven; Build (or the user) should record surefire output in handoff/reports before submission. *What I did verify:* all three new test classes exist with the content the status report claims (7 + 4 + 5 = 16 new tests; bytecode-compile evidence and pt1 baseline of 23 tests were verified by the prior incremental review and earlier handoffs).

**V2 — Manual curl/Postman matrix outputs not retrievable.** `TESTING.md` §3 (J10/J14) records a 7-row live matrix all-PASS against a running dev-profile app, including a captured JWT. No raw curl transcripts, terminal capture, or Postman export are retained in the repo. The recorded JWT header/body claims are internally consistent with the issuer (`sub=user`, HS256, iat/exp 3600000s apart), but_MATRIX execution cannot be independently reproduced from evidence — treat as a Build claim, corroborated (not proven) by that consistency and by the automated tests covering the same flows.

**V3 — jjwt/Jackson-2 resolution version never recorded.** The plan's J2/R1 acceptance check (`./mvnw dependency:tree -Dincludes=io.jsonwebtoken` and the resolved `jackson-databind` version) has no recorded output in any handoff (`TESTING.md` §4 does not mention it either). jjwt 0.11.5's Jackson-2 dependency under Spring Boot 4's BOM is expected to be benign (the prior review's F1 analysis, corroborated by a successful test run claim), but the actual resolved versions should be recorded to close plan Risk R1.

**V4 — Git/submission state not independently verified by this review.** Step 12 is paused (per plan D10/R5): all pt1+pt2 work is claimed uncommitted on `main` at HEAD `d995479` (the earlier JWT experiment). This review did not run git commands; the user should confirm the two-commit plan and perform the J16 submission.

**V5 — Fresh state consistency check.** After Build applied the step-6 `SecurityConfig` rewrite mid-build (per the status handoff, needed because the tree still held the pt1-only chain), no subsequent full-suite re-run record exists covering *only* the final state (V1's 20:01 run is claimed *after* that fix — corroborated by the run timestamp versus the rewrite, but without a preserved transcript). Any future change should re-run `./mvnw test` and capture the surefire summary.

## Build handoff (actionable next steps)

No code fix is required for correctness against J1–J15. To close this phase:

1. **Evidence (V1/V3):** run `./mvnw test` and `./mvnw dependency:tree -Dincludes=io.jsonwebtoken,com.fasterxml.jackson.core` once more, and paste the actual summaries into a pt2 completion handoff under `docs/handoffs/`.
2. **Comment fix (F1):** adjust the `JwtAuthFilter` class Javadoc bullet to "invalid or unhandled token: no authentication set; context cleared only if validation threw — always continues the chain" (one-line change, no behavior change).
3. **User confirmation (J16/V4):** commit pt1 state, then pt2, push to `origin/main`; complete the Blackboard submission and fill the credits placeholder in `TESTING.md` §5.
4. Optionally record the V2 manual-matrix transcript or a Postman collection export in the repo before pushing, so J15 evidence travels with the submission.

## Limits of this review

Reviewed static source against the extracted assignment text; did not compile, run tests, or execute the HTTP matrix (Review mode). Test/matrix outcomes remain Build claims until the artifacts in V1–V3 are supplied. No rendering/inspection tools were needed (all evidence was textual). The incremental review `docs/reviews/m3-simple-crm-pt2-jwt-auth-code-review-2026-10-30.md` remains authoritative for per-step evidence collected before this report.
