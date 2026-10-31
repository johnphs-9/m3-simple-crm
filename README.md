# m3-simple-crm

A Spring Boot 4.1.1 / Java 21 CRM project that **rolls up every lesson in Module 3** (excluding Spring AI), plus
Assignments **4.1 and 4.2** (secure software development & authentication, PT1 and PT2), built
on top of the earlier `simple-crm` REST API.

The main purpose of this repository is to **publish the work done for Assignments 3.19, 4.1 and 4.2**:

| Assignment | Work published here |
|------------|---------------------|
| **3.19 — Software Testing** | JUnit + Mockito unit tests for the service layer, and `@SpringBootTest` + MockMvc integration tests for the REST controller |
| **4.1 — Secure SD & Authentication PT1** | Spring Security with HTTP Basic Auth, BCrypt password encoding, in-memory users, and Role-Based Access Control (RBAC) on `/customers/**` |
| **4.2 — Secure SD & Authentication PT2** | JWT authentication: `POST /auth/login` issuing jjwt HS256 tokens, `JwtAuthFilter` on `/api/**`, and a protected `GET /api/hello` endpoint |

The automated test suite below covers all three assignments, run against the rolled-up project.

---

## Test Summary (Assignments 3.19, 4.1, 4.2)

Run with:

```bash
cd m3-simple-crm/m3-simple-crm
./mvnw test
```

**Result: 39 tests run, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**
(source: Maven Surefire plugin — the latest run of the rolled-up project).

### Per-class results (from `m3-simple-crm/target/surefire-reports/`)

| Test class | Tests | Failures | Errors | Skipped | Assignment covered |
|------------|------:|---------:|-------:|--------:|--------------------|
| `service.DemoServiceTest` | 10 | 0 | 0 | 0 | 3.19 — unit tests (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`) for the service layer (module 3 lessons baseline) |
| `service.CustomerServiceImplTest` | 3 | 0 | 0 | 0 | 3.19 — create / get-by-id / get-by-id-not-found (mock `save()`, `findById()`, `assertThrows` on the custom exception) |
| `controller.CustomerControllerTest` | 9 | 0 | 0 | 0 | 3.19 + 4.1 — MockMvc integration tests for `GET` (200 + JSON array), `POST` with valid data (201), `POST` with invalid data (400), plus HTTP Basic / RBAC coverage (USER allowed, forbidden for non-ADMIN writes) |
| `M3SimpleCrmApplicationTests` | 1 | 0 | 0 | 0 | Baseline — Spring context loads |
| `security.JwtServiceTest` | 7 | 0 | 0 | 0 | 4.2 — subject/`iat`/expiry round-trip, token extraction, valid → true, garbage → false, tampered signature → false, wrong key → false, expired → false |
| `controller.AuthControllerTest` | 4 | 0 | 0 | 0 | 4.2 — `POST /auth/login`: valid credentials → 200 + token; wrong password → 401; wrong username → 401; missing fields → 401 |
| `controller.HelloApiSecurityTest` | 5 | 0 | 0 | 0 | 4.1 + 4.2 — no token → 401; valid Bearer → 200 with expected body; garbage Bearer → 401; expired Bearer → 401; ROLE_USER mapping regression (`GET /customers` → 200) |
| **Total** | **39** | **0** | **0** | **0** | |

### What the suite exercises

- **Assignment 3.19** — unit tests mock the repository (`save`, `findById`) and verify
  create / get-by-id / not-found behaviour; integration tests run through a real MockMvc layer
  asserting status codes, JSON content type and payload validation (200 / 201 / 400).
- **Assignment 4.1 (PT1)** — the real Spring Security filter chain with HTTP Basic auth on the
  in-memory users (`user`/`user123` → ROLE_USER, `admin`/`admin123` → ROLE_ADMIN): public
  `GET /health` permitted, `GET /customers/**` for authenticated users, writes (`POST`/`DELETE` on
  `/customers`) restricted to ADMIN, with the corresponding 401/403/200/201 outcomes asserted.
- **Assignment 4.2 (PT2)** — JWT issuance and validation (`JwtService`, jjwt HS256) and the full
  login → Bearer-token flow through `POST /auth/login` → `GET /api/hello` under the `test`
  profile (H2 in-memory `crm-tests`), ending-to-end without a database.

In addition to the automated suite, a manual curl/Postman verification matrix (login, protected
endpoint with and without a token, and PT1 regression through Basic auth) was run against the live
dev-profile application — all steps passed. Details are recorded in [`m3-simple-crm/TESTING.md`](m3-simple-crm/TESTING.md).

---

## Credits (from TESTING.md — References / Credits, J16)

### References

- **Spring Security reference documentation** — `SecurityFilterChain`, `authorizeHttpRequests`,
  `SessionCreationPolicy.STATELESS`, `HttpStatusEntryPoint`, and filter ordering
  (`addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`).
  <https://docs.spring.io/spring-security/reference/servlet/api.html>
- **OWASP JWT Cheat Sheet** — HS256 key strength (≥ 32 bytes / 256 bits), signature and expiry
  validation, and the "validate on every request" principle applied in `JwtAuthFilter`.
  <https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html>
- **jjwt (Java JWT) documentation** — `Jwts.builder()`, `Jwts.parserBuilder()`,
  `SignatureAlgorithm.HS256`, and `Keys.hmacShaKeyFor(...)` used in `JwtService`.
  <https://github.com/jwtk/jjwt>
- **Spring Boot 4 MockMvc test package** — `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
  (the Boot 4 location of the annotation; the Boot 3 package was intentionally not used).
- **Spring Boot externalised configuration** — `spring.config.import=optional:file:.env[.properties]`
  used to source secrets from the gitignored `.env`.
  <https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config>
- **Classmates / URLs:** none — no classmates were consulted. Tools and documentation/URLs
  referenced are credited below (per assignment §Submission L56–57).

### AI tool credits — Assignments 4.1 & 4.2

| Phase | Scope | Tool |
| :--- | :--- | --- |
| Agents | Session orchestration, MCP/skill loading, document extraction and file management used to run the Plan→Build→Test→Review workflow | Opencode, OMLX |
| Plan | Make a persistent build plan in md file from the requirements (pt1 Basic Auth/RBAC plan + test-fix plan; pt2 JWT auth plan) | Qwen3.8-27B-MLX-4bit |
| Build | Implement the planning handoffs in source: pom/security config, controllers, JWT service & filter, DTOs, new test classes, secret externalisation to `.env` | Qwen3.8-27B-Coder390-oQ4e-MTP-Text |
| Test | Author/extend JUnit + MockMvc suites, run `./mvnw test`, capture surefire reports, live curl/Postman smoke matrix | Qwen3.8-27B-MLX-4bit, Qwen3.8-27B-Coder390, GLM-5.3-Flash-Free |
| Review | Requirements-to-code reviews: requirement coverage vs `assignment.md`, test-evidence verification, findings + verification-gap reports under `docs/reviews/` | GLM-5.3-Flash-Free |

Tool URLs credited:

- **Opencode** (open-source terminal coding agent) — <https://github.com/sst/opencode>
- **OMLX** (local MLX model serving for the agents) — <https://github.com/Isaac-tools/omlx>
- **Qwen3.8 model family** (plan/build/test models above, MLX 4-bit and MTP variants) — <https://github.com/QwenLM/Qwen3>
- **GLM-5.3-Flash-Free** (test/review model) — <https://github.com/zai-org/GLM>

The documentation URLs each piece of work referenced are listed above
(Spring Security, OWASP JWT Cheat Sheet, jjwt, Spring Boot externalised configuration).
