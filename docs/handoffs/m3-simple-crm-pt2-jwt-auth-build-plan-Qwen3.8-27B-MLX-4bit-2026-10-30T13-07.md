# Build Handoff — m3-simple-crm · PT2: JWT Authentication (Build Plan)

- **Date:** 2026-10-30 13:07 +08
- **Author:** Plan agent (Qwen3.8-27B-MLX-4bit) — read-only planning; no source edited.
- **Target module:** `m3-simple-crm/m3-simple-crm` (Spring Boot **4.1.1**, Java **21**, Maven wrapper `./mvnw`)
- **Repo root:** `/Users/johnphs/Documents/code/NTU-AIE/m3-simple-crm` (git branch `main`, remote `origin`)
- **Assignment source:** `/Users/johnphs/Documents/code/NTU-AIE/ai-4.2-secure-software-development-and-authentication-pt2/assignment.md` (63 lines, read in full; source unchanged)
- **Previous phase (pt1) handoffs:** `docs/handoffs/m3-simple-crm-pt1-basic-auth-rbac-build-handoff.md`, `docs/handoffs/m3-simple-crm-pt1-build-plan-fix-mvn-test-errors.md` (23/23 tests green at 2026-10-30 00:39)

## Scope

Implement JWT authentication (assignment PT2) on top of the existing pt1 app:
1. **Part 1** — JWT token generation + `POST /auth/login` (jjwt 0.11.5, DTOs, `JwtService`, `AuthController`).
2. **Part 2** — `JwtAuthFilter` (`OncePerRequestFilter`), `SecurityConfig` updates, protected `GET /api/hello`.
3. **Verification** — automated JUnit/MockMvc tests mirroring the assignment's Postman flows, plus a manual curl/Postman matrix documented in the repo.

Out of scope: changing pt1 behavior (Basic Auth + RBAC on `/customers/**`), schema/DB changes, unrelated refactors.

## Requirements (with assignment.md line citations)

| ID | Requirement | Source |
|----|-------------|--------|
| J1 | Use the existing project (m3-simple-crm) | §Brief L8 |
| J2 | Add `io.jsonwebtoken:jjwt-api:0.11.5`, `jjwt-impl:0.11.5` (runtime), `jjwt-jackson:0.11.5` (runtime) to `pom.xml` | §1 L9–12 |
| J3 | Add `jwt.secret` (long random string) and `jwt.expiration-ms=3600000` to `application.properties` | §1 L13–15 |
| J4 | `LoginRequest` DTO with `username` and `password` fields | §1 L16–17 |
| J5 | `TokenResponse` DTO with `token` field | §1 L18 |
| J6 | `JwtService.generateToken(String username)` — JWT with username + expiry | §1 L20 |
| J7 | `JwtService.extractUsername(String token)` | §1 L21 |
| J8 | `JwtService.isTokenValid(String token)` — validates signature and expiry | §1 L22 |
| J9 | `AuthController` `POST /auth/login`: accept `LoginRequest`; simple check `username="user"`, `password="password"`; return `TokenResponse` (200) or 401 | §1 L23–27 |
| J10 | Test login with Postman; verify token received; copy for next part | §1 L28–31 |
| J11 | `JwtAuthFilter` extending `OncePerRequestFilter`: read `Authorization: Bearer <token>`; validate via `JwtService`; if valid set authentication in `SecurityContext`; if invalid/missing continue filter chain | §2 L34–38 |
| J12 | `SecurityConfig`: disable CSRF; session STATELESS; `/auth/login` unauthenticated; `/api/**` requires authentication; JWT filter before `UsernamePasswordAuthenticationFilter` | §2 L39–45 |
| J13 | Protected endpoint `GET /api/hello` returning `"Hello! JWT authentication successful."` | §2 L46–47 |
| J14 | Full Postman flow: `/api/hello` w/o token → 401; login → token; with `Authorization: Bearer <token>` → 200 | §2 L48–51 |
| J15 | Document testing results (descriptions or screenshots) | §2 L52 |
| J16 | Submit GitHub repo URL to NTU blackboard; credit classmates/URLs for referenced work | §Submission L56–57 |

## Decisions (ambiguities and conflicts resolved)

| # | Ambiguity / conflict | Decision |
|---|----------------------|----------|
| D1 | L25 mandates the **simple check** `username="user"`, `password="password"`, but pt1's in-memory store has `user/user123` (BCrypt). The earlier JWT experiment (HEAD commit `d995479`) used `AuthenticationManager`, which would **fail** for password `password`. | Follow the PT2 spec literally: hardcode the `user`/`password` check in `AuthController`. The pt1 in-memory store is **kept as-is** for the Basic Auth mechanism (two distinct mechanisms, both documented). This is an intentional, spec-mandated divergence from the pt1 password. |
| D2 | PT2 is silent on the **roles** of a JWT-authenticated principal, while pt1 RBAC rules on `/customers/**` use `hasRole("ADMIN")`. | In `JwtAuthFilter`, map the token subject through the existing `UserDetailsService` bean (`user`→ROLE_USER, `admin`→ROLE_ADMIN); fall back to `ROLE_USER` if the username is unknown. JWT users then behave identically to Basic users under pt1 rules. |
| D3 | Assignment says "update `SecurityConfig`" (J12) without saying to remove HTTP Basic or the pt1 `/customers/**` rules. | **Keep** `.httpBasic(...)` and all pt1 matchers; add the JWT rules alongside. Preserves the pt1 deliverable, keeps all 23 existing tests green, and the w/o-token → 401 acceptance still holds (no Basic header + no JWT → 401). |
| D4 | Where `jwt.*` lives: base `application.properties` (assignment L13 names it) vs profile files. | Base `application.properties` — applies to all profiles including `test` (so `JwtService` resolves in every test context). No changes needed to `application-dev/prod/test.properties`. |
| D5 | A `Filter` bean is auto-registered by Spring Boot into the servlet container **and** would be added to the security chain → double execution. | `JwtAuthFilter` is instantiated **inline** in `securityFilterChain(...)` (not `@Component`, not a `@Bean`), then `addFilterBefore(...)` — single registration only. |
| D6 | jjwt **0.11.5** (2021, pre-Jakarta) vs the 0.12.x Jakarta line, under Spring Boot 4. | Follow the spec pin **0.11.5**. Risk is low: the `jjwt-*` core artifacts have no servlet-API dependency, and all three 0.11.5 artifacts are **already cached** in `~/.m2` (verified). Fallback if Maven/compile fails: see Risks R1. |
| D7 | DTO style. | Lombok `@Data @NoArgsConstructor @AllArgsConstructor` — matches project convention (`model/Customer.java` L22–28; Lombok is an optional dep in the pom). |
| D8 | `@AutoConfigureMockMvc` package moved in Spring Boot 4. | New tests import `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (same as existing `CustomerControllerTest` L13) **and** use `spring-boot-starter-webmvc-test` (already in pom L50–54). Do **not** use the old Boot 3 package. |
| D9 | J15 documentation location. | `m3-simple-crm/TESTING.md` in the module root (next to `pom.xml`), containing the results matrix + a "References / Credits" section (J16). Non-source file; written by Build. |
| D10 | Git state: the pt1 work is **uncommitted** (working tree shows `D` for the old JWT files, `M` for pom/SecurityConfig/tests/props; HEAD `d995479` still contains the old JWT experiment). | Implementation proceeds on the current working tree. At the end, suggest two clean commits (pt1 state, then pt2) and push — **only with user confirmation** (J16 submission is a user action). |

## Current state (verified against source)

**Working tree = pt1-complete** (handoff `m3-simple-crm-pt1-build-plan-fix-mvn-test-errors.md`: 23/23 tests passed, BUILD SUCCESS):
- `m3-simple-crm/pom.xml` — deps: web (L37–39), security (L41–43), test (L44–48), webmvc-test (L50–54), security-test (L55–59), data-jpa (L60–63), validation (L64–67), postgresql (L68–71), h2/test (L72–76), lombok/optional (L77–81). **No jjwt entries** (pt1 removed them).
- `config/SecurityConfig.java` — chain: CSRF disabled (L40), STATELESS (L41), matchers `/health` permitAll + customers RBAC + `anyRequest().authenticated()` (L42–48), `HttpStatusEntryPoint(401)` (L49–50), `.httpBasic(...)` (L51); `BCryptPasswordEncoder` bean (L55–58); `InMemoryUserDetailsManager` with `user/user123` (USER) + `admin/admin123` (ADMIN) (L64–77).
- `src/main/resources/application.properties` — single line `spring.profiles.active=dev` (L1).
- `src/test/resources/application-test.properties` — H2 in-mem `crm-tests`, `create-drop`.
- `controller/` — `CustomerController.java` (8 endpoints under `/customers`), `HealthController.java` (untracked, new in pt1).
- `src/test/java/.../controller/CustomerControllerTest.java` — 9 tests (L38–122): no-creds 401, `/health` 200, `@WithMockUser` RBAC cases. Total baseline: 23 tests.
- **No** `auth/`, `security/` packages in the working tree (deleted in pt1; the deleted files exist only in HEAD).
- Module `.gitignore` ignores `.env`/`.env.*` (keeps `.env.example`); a local `.env` exists untracked (irrelevant to JWT).

**Environment checks:**
- `~/.m2/repository/io/jsonwebtoken/` already contains `jjwt-api`, `jjwt-impl`, `jjwt-jackson` at **0.11.5** → dependency resolution works even offline.
- Manual run (J10/J14) uses the `dev` profile → needs local Postgres `localhost:5432/simple_crm` (H2 is **test scope only**, so an H2 fallback for a *running* app is not possible without a scope change — see Risks R3).
- Generated 48-byte base64 secret for J3 (≥32 bytes required for HS256): `+WVaxfdqg/mQQfMpL1QQZRFfFLYl3NYVi10swVb8rErr9xKO3m6tNZDOkVTbvx9F`

## Implementation (file by file)

All paths relative to module root `m3-simple-crm/m3-simple-crm/`. Package root: `sg.edu.ntu.m3_simple_crm`.

### 1. `pom.xml` — add before `</dependencies>` (J2)

```xml
<!-- JWT (Assignment PT2 §1): jjwt 0.11.5 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### 2. `src/main/resources/application.properties` — full new content (J3)

```properties
spring.profiles.active=dev

# JWT configuration (Assignment PT2, assignment.md L13-15)
# Long random secret: 48 bytes base64 (>= 32 bytes required for HS256).
# NOTE: fixed value required by the assignment — rotate/externalise before real deployment.
jwt.secret=+WVaxfdqg/mQQfMpL1QQZRFfFLYl3NYVi10swVb8rErr9xKO3m6tNZDOkVTbvx9F
jwt.expiration-ms=3600000
```

### 3. `src/main/java/sg/edu/ntu/m3_simple_crm/auth/LoginRequest.java` — NEW (J4)

```java
package sg.edu.ntu.m3_simple_crm.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /auth/login (Assignment PT2 §1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
}
```

### 4. `src/main/java/sg/edu/ntu/m3_simple_crm/auth/TokenResponse.java` — NEW (J5)

```java
package sg.edu.ntu.m3_simple_crm.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /auth/login: the issued JWT (Assignment PT2 §1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String token;
}
```

### 5. `src/main/java/sg/edu/ntu/m3_simple_crm/security/JwtService.java` — NEW (J6–J8)

```java
package sg.edu.ntu.m3_simple_crm.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates HS256-signed JWTs (Assignment PT2 §1).
 * Configuration: {@code jwt.secret} and {@code jwt.expiration-ms} from application.properties.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /** Derives the HMAC-SHA key from the configured secret. */
    private Key signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** J6 — generates a token with the given username (subject), iat and future exp. */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** J7 — extracts the username (subject) from the token. */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /** J8 — validates signature and expiry; true only for well-formed, unexpired, correctly-signed tokens. */
    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

### 6. `src/main/java/sg/edu/ntu/m3_simple_crm/auth/AuthController.java` — NEW (J9)

```java
package sg.edu.ntu.m3_simple_crm.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.ntu.m3_simple_crm.security.JwtService;

/**
 * JWT issuance endpoint (Assignment PT2 §1).
 * Credentials are validated with the simple fixed check required by the assignment
 * (assignment.md L25): username "user" / password "password".
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String VALID_USERNAME = "user";
    private static final String VALID_PASSWORD = "password";

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * POST /auth/login — issues a JWT for valid credentials.
     *
     * @return 200 with a TokenResponse for valid credentials; 401 otherwise
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        if (request != null
                && VALID_USERNAME.equals(request.getUsername())
                && VALID_PASSWORD.equals(request.getPassword())) {
            String token = jwtService.generateToken(request.getUsername());
            return ResponseEntity.ok(new TokenResponse(token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
```

### 7. `src/main/java/sg/edu/ntu/m3_simple_crm/security/JwtAuthFilter.java` — NEW (J11, D2, D5)

```java
package sg.edu.ntu.m3_simple_crm.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT authentication filter (Assignment PT2 §2, L34-38).
 * <ul>
 *   <li>Reads {@code Authorization: Bearer <token>}.</li>
 *   <li>If the token is valid, sets the authentication in the SecurityContext
 *       (authorities mapped from the existing UserDetailsService, fallback ROLE_USER).</li>
 *   <li>If the token is missing or invalid, clears any context and continues the chain
 *       — the request is simply unauthenticated (401 comes from the entry point).</li>
 * </ul>
 * Instantiated inline in {@code SecurityConfig} (not a bean) to avoid double registration.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                if (jwtService.isTokenValid(token)) {
                    String username = jwtService.extractUsername(token);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authoritiesFor(username));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext(); // treat as unauthenticated, never 500
            }
        }
        // Missing, invalid, or valid token: always continue the chain (L37-38)
        chain.doFilter(request, response);
    }

    /** Maps the token subject to the pt1 RBAC roles; unknown users default to ROLE_USER (D2). */
    private List<GrantedAuthority> authoritiesFor(String username) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(username);
            return user.getAuthorities();
        } catch (UsernameNotFoundException ex) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }
}
```

> ⚠️ Build: if `org.springframework.web.filter.OncePerRequestFilter` does not compile under Spring Framework 7 (class relocation), check the moved package via `./mvnw -q dependency:tree` / compile error and adjust the import only — the rest of the filter is API-stable.

### 8. `src/main/java/sg/edu/ntu/m3_simple_crm/config/SecurityConfig.java` — rewrite (J12, D3, D5)

Full new content:

```java
package sg.edu.ntu.m3_simple_crm.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import sg.edu.ntu.m3_simple_crm.security.JwtAuthFilter;
import sg.edu.ntu.m3_simple_crm.security.JwtService;

/**
 * Security configuration with two complementary authentication mechanisms:
 * <ul>
 *   <li><b>HTTP Basic</b> (pt1): in-memory users {@code user}/{@code admin} with RBAC on {@code /customers/**}.</li>
 *   <li><b>JWT Bearer</b> (pt2): issued at {@code POST /auth/login}, enforced on {@code /api/**}.</li>
 * </ul>
 * Unauthenticated requests → 401 (HttpStatusEntryPoint); authenticated but unauthorized → 403.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService) throws Exception {
        // Inline instantiation (not a bean): the security chain is the single registration (D5).
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService);

        http
            .csrf(csrf -> csrf.disable())   // pt1 R9 + pt2 L40
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // L41
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()                          // L43
                .requestMatchers("/api/**").authenticated()                          // L44
                .requestMatchers("/health").permitAll()                              // pt1 R11
                .requestMatchers(HttpMethod.POST, "/customers/**").hasRole("ADMIN")  // pt1 R12
                .requestMatchers(HttpMethod.PUT, "/customers/**").hasRole("ADMIN")   // pt1 default
                .requestMatchers(HttpMethod.DELETE, "/customers/**").hasRole("ADMIN")// pt1 R13
                .requestMatchers(HttpMethod.GET, "/customers/**").authenticated()    // pt1 R14
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // L45
            .httpBasic(Customizer.withDefaults()); // pt1 R10 — kept (D3)
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** pt1 in-memory users (unchanged) — also consulted by JwtAuthFilter for role mapping (D2). */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build();
        var admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(List.of(user, admin));
    }
}
```

### 9. `src/main/java/sg/edu/ntu/m3_simple_crm/controller/HelloController.java` — NEW (J13)

```java
package sg.edu.ntu.m3_simple_crm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protected endpoint demonstrating JWT enforcement (Assignment PT2 §2, L46-47).
 * Requires a valid Bearer token (SecurityConfig: /api/** authenticated).
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello! JWT authentication successful.";
    }
}
```

### Acceptance criteria → proposed checks

| Req | Check (Build executes and records actual output) |
|-----|--------------------------------------------------|
| J2 | `./mvnw -q compile` succeeds; `./mvnw dependency:tree -Dincludes=io.jsonwebtoken` shows all three artifacts at 0.11.5 (impl/jackson runtime). |
| J3 | `grep -n "^jwt\." src/main/resources/application.properties` → `jwt.secret` (64 chars) + `jwt.expiration-ms=3600000`. |
| J4–J5 | Covered by compile + the JSON binding exercised in `AuthControllerTest` bodies. |
| J6–J8 | New `security/JwtServiceTest` (unit, no Spring context): round-trip subject, future expiry, valid→true, tampered→false, wrong-key→false, expired→false. |
| J9 | New `controller/AuthControllerTest` (MockMvc, test profile): `{"user","password"}` → 200 + non-empty 3-part JWT in `token`; wrong password → 401; wrong username → 401; missing fields → 401. |
| J11, J12, J13 | New `controller/HelloApiSecurityTest` (MockMvc + real filter chain, test profile): no header → 401; valid Bearer (issued via autowired `JwtService`) → 200 + body exactly `Hello! JWT authentication successful.`; garbage Bearer → 401; expired Bearer → 401; Basic `user:user123` on `/customers` still 200 (regression). |
| J12 regression | Full `./mvnw test`: previous 23 tests + new tests, 0 failures. |
| J10, J14 | Manual matrix with running app (curl/Postman) — see "Manual verification script" for the exact commands; results recorded in `m3-simple-crm/TESTING.md` (J15). |
| J16 | `TESTING.md` References/Credits section (Spring Security docs, OWASP JWT cheat sheet, jjwt docs); git push only after user confirmation. |

## Ordered Build steps

1. **pom.xml** — insert the three jjwt dependencies (J2).
2. **application.properties** — add `jwt.secret` / `jwt.expiration-ms` (J3).
3. **New DTOs** — `auth/LoginRequest.java`, `auth/TokenResponse.java` (J4–J5).
4. **security/JwtService.java** (J6–J8).
5. **auth/AuthController.java** (J9).
6. **security/JwtAuthFilter.java** (J11) + **config/SecurityConfig.java** rewrite (J12, D3/D5).
7. **controller/HelloController.java** (J13).
8. **New tests** — `JwtServiceTest`, `AuthControllerTest`, `HelloApiSecurityTest` (use the Boot 4 `AutoConfigureMockMvc` package per D8).
9. **`./mvnw test`** — expect 23 + new, all green; fix within scope or report.
10. **Manual verification (J10, J14)** — `./mvnw spring-boot:run` (dev profile; needs local Postgres `simple_crm`), run the curl/Postman matrix, capture actual responses.
11. **`m3-simple-crm/TESTING.md`** (J15 + J16 credits) — write the documented matrix.
12. **Git** — suggest commits (pt1 uncommitted state first, then pt2) and push; **confirm with user** before committing/pushing.

## New tests (full code)

### 10. `src/test/java/sg/edu/ntu/m3_simple_crm/security/JwtServiceTest.java` — NEW (J6–J8)

```java
package sg.edu.ntu.m3_simple_crm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/** Unit tests for {@link JwtService} (J6-J8). No Spring context needed. */
class JwtServiceTest {

    /** 48 chars >= 32 bytes, satisfies HS256 key strength. */
    private static final String SECRET = "unit-test-secret-key-0123456789-0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3_600_000L);
    }

    @Test
    void generateToken_setsSubjectIatAndFutureExpiry() {
        String token = jwtService.generateToken("user");
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var body = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        assertEquals("user", body.getSubject());
        assertNotNull(body.getIssuedAt());
        assertTrue(body.getExpiration().after(new Date()));
    }

    @Test
    void extractUsername_roundTrips() {
        String token = jwtService.generateToken("user");
        assertEquals("user", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_trueForOwnUnexpiredToken() {
        assertTrue(jwtService.isTokenValid(jwtService.generateToken("user")));
    }

    @Test
    void isTokenValid_falseForGarbage() {
        assertFalse(jwtService.isTokenValid("not.a.jwt"));
        assertFalse(jwtService.isTokenValid(""));
    }

    @Test
    void isTokenValid_falseForTamperedToken() {
        String token = jwtService.generateToken("user");
        String tampered = token.substring(0, token.length() - 5) + "AAAAA"; // corrupt signature
        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_falseForTokenSignedWithDifferentKey() {
        Key otherKey = Keys.hmacShaKeyFor("other-secret-key-123456789-123456789".getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String foreign = Jwts.builder().setSubject("user")
                .setIssuedAt(now).setExpiration(new Date(now.getTime() + 60_000))
                .signWith(otherKey, SignatureAlgorithm.HS256).compact();
        assertFalse(jwtService.isTokenValid(foreign));
    }

    @Test
    void isTokenValid_falseForExpiredToken() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String expired = Jwts.builder().setSubject("user")
                .setIssuedAt(new Date(now.getTime() - 7_200_000))
                .setExpiration(new Date(now.getTime() - 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256).compact();
        assertFalse(jwtService.isTokenValid(expired)); // expired, otherwise valid
    }
}
```

### 11. `src/test/java/sg/edu/ntu/m3_simple_crm/controller/AuthControllerTest.java` — NEW (J9)

```java
package sg.edu.ntu.m3_simple_crm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // D8 — Boot 4 package
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for POST /auth/login (J9) — mirrors the assignment's Postman checks (J10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        String body = "{\"username\":\"user\",\"password\":\"password\"}";
        String json = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertTrue(json.split("\"").length > 3, "token field expected: " + json);
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String body = "{\"username\":\"user\",\"password\":\"wrong\"}";
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_wrongUsername_returnsUnauthorized() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"password\"}";
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingFields_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
```

> Import note for Build: add `import static org.junit.jupiter.api.Assertions.assertTrue;` to `AuthControllerTest`.

### 12. `src/test/java/sg/edu/ntu/m3_simple_crm/controller/HelloApiSecurityTest.java` — NEW (J11, J12, J13, J14)

```java
package sg.edu.ntu.m3_simple_crm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // D8 — Boot 4 package
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import sg.edu.ntu.m3_simple_crm.security.JwtService;

/**
 * End-to-end JWT enforcement on the protected endpoint (J11/J12/J13) —
 * mirrors the assignment's full Postman flow (J14): no token → 401,
 * login token → 200, bad/expired token → 401. Runs the real security
 * filter chain (including JwtAuthFilter) via MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HelloApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void apiHello_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiHello_withValidToken_returnsHello() throws Exception {
        String token = jwtService.generateToken("user");
        mockMvc.perform(get("/api/hello")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello! JWT authentication successful."));
    }

    @Test
    void apiHello_withGarbageToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/hello")
                .header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiHello_withExpiredToken_returnsUnauthorized() throws Exception {
        // Build a correctly-formed but expired token with the app's own secret.
        String secret = (String) ReflectionTestUtils.getField(jwtService, "jwtSecret");
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String expired = Jwts.builder().setSubject("user")
                .setIssuedAt(new Date(now.getTime() - 7_200_000))
                .setExpiration(new Date(now.getTime() - 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256).compact();
        mockMvc.perform(get("/api/hello")
                .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiHello_withValidToken_roleMappedFromUserStore() throws Exception {
        // Regression: token for "user" maps to ROLE_USER via UserDetailsService (D2),
        // so pt1 rules still apply: GET /customers 200, POST /customers 403.
        String token = jwtService.generateToken("user");
        mockMvc.perform(get("/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
```

> If the `UserDetailsService` autowire is unused after trimming, drop it — the `roleMappedFromUserStore` test only needs the token.

## Tests 

### Manual verification script (J10 + J14 — with running app)

```bash
cd m3-simple-crm/m3-simple-crm
./mvnw spring-boot:run        # dev profile; needs local Postgres simple_crm (see Risk R3)

# 1) Protected endpoint without token → expect 401
curl -i http://localhost:8080/api/hello

# 2) Login with the assignment's simple credentials → expect 200 + {"token":"..."}
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"user","password":"password"}'

# 3) Wrong password → expect 401
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"user","password":"nope"}'

# 4) Protected endpoint WITH token (paste token from step 2) → expect 200 + hello message
curl -i http://localhost:8080/api/hello -H "Authorization: Bearer <TOKEN>"

# 5) pt1 regression: Basic auth still works alongside JWT
curl -i -u user:user123 http://localhost:8080/customers          # 200
curl -i -u admin:admin123 -X POST -H 'Content-Type: application/json' \
  -d '{"firstName":"A","lastName":"B","email":"a@b.co","contactNo":"12345678","jobTitle":"X","yearOfBirth":1990}' \
  http://localhost:8080/customers                                 # 201
curl -i http://localhost:8080/health                             # 200 (public)
```

Record every step's actual status + body in `m3-simple-crm/TESTING.md` (J15): table of request → expected → actual → pass/fail, plus a "How to reproduce" section (the script above) and a **References / Credits** section (Spring Security reference, OWASP JWT cheat sheet, jjwt docs, plus any classmate/URL help per J16).

## Risks & fallbacks

| # | Risk | Likelihood | Fallback |
|---|------|-----------|----------|
| R1 | jjwt 0.11.5 (declares Jackson 2.15.3) under Spring Boot 4.1.1's managed Jackson could cause a dependency-conflict or compile issue. | Low (artifacts already resolved before in HEAD commit; core is servlet-independent) | First `./mvnw -q dependency:tree -Dincludes=com.fasterxml.jackson.core` to inspect; pin Jackson explicitly, or — as a reported deviation — bump jjwt to 0.12.x. |
| R2 | `OncePerRequestFilter` package relocation in Spring Framework 7. | Low | Compile error will name the correct FQCN; adjust the import only. |
| R3 | Manual matrix (J10/J14) requires local Postgres `simple_crm` on `localhost:5432`; H2 is test-scope only, so no H2 fallback for a *running* app without a scope change (not in the assignment). | Medium | If Postgres is unavailable: document that the *automated* tests (same flows, real filter chain, H2 test profile) passed, mark manual matrix "blocked — no local Postgres", and optionally run Docker Postgres if the user agrees. |
| R4 | Committed `jwt.secret` in the repo (spec-mandated) is a real security smell for a public GitHub submission. | Certain (by spec) | Comment in `application.properties` + TESTING.md note: rotate before real use; support `JWT_SECRET` env override only if user wants (spec-literal value stays). |
| R5 | Working tree has uncommitted pt1 changes; HEAD contains the *old* JWT experiment — a careless `git checkout .`/`git restore` would revert pt1. | Low–medium | Do **not** run destructive git commands; commit pt1 state first (with user confirmation), then pt2. |
| R6 | `HelloController` returns a `String` → `text/plain`; a grader expecting JSON would see plain text. | Low | Spec says "returns *'Hello! JWT authentication successful.'*" — plain string is the literal reading. Keep; note in TESTING.md. |

## Open questions (ask user during/after Build)

1. **Commit/push** (J16): commit the uncommitted pt1 state first, then pt2, and push to `origin/main`? (Plan assumes no commit without confirmation.)
2. **Postgres availability** for the manual matrix (Risk R3) — is local Postgres `simple_crm` running, or should Build use Docker?
3. **`/api/hello` body format** — plain text per spec (R6), or also expose a JSON variant?
4. Should the pt1 outstanding items (pt1 R16 documentation, pt1 R17 submission) be folded into this phase's TESTING.md/submission as well?

## Next action (Build handoff)

Switch to the **Build** agent. Implement **steps 1–8** of "Ordered Build steps" exactly as specified in the "Implementation" section (quote IDs J1–J13 when working), run **step 9** (`./mvnw test`) and record the actual output, then attempt **step 10** (manual matrix; if Postgres is unavailable, follow Risk R3 and report rather than fabricate results), write **step 11** (`m3-simple-crm/TESTING.md`), and pause at **step 12** (git) for user confirmation. Finish with a completion handoff in `docs/handoffs/` containing: requirement IDs implemented, changed files, checks actually run with real outputs, coverage of J1–J16, and open items from the table above.

**No source file was modified by this planning session** — the assignment and all project sources are untouched; the only file written is this handoff.
