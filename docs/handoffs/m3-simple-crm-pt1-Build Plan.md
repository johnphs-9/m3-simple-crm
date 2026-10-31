# Build Handoff — m3-simple-crm · Secure Software Development & Authentication (PT1)

- **Date:** 2026-10-29
- **Author:** Plan agent (Qwen3.8) — read-only planning; no source edited.
- **Target module:** `m3-simple-crm/m3-simple-crm` (Spring Boot **4.1.1**, Java **21**)
- **Repo root:** `/Users/johnphs/Documents/code/NTU-AIE/m3-simple-crm`
- **Assignment source:** `/Users/johnphs/Documents/code/NTU-AIE/ai-4.1-secure-software-development-and-authentication-pt1/assignment.md` (50 lines)
- **Chosen approach:** **A — Replace** (Basic Auth + RBAC + BCrypt becomes the sole auth mechanism; JWT stack removed). `PUT /customers/{id}` → **ADMIN** (assignment unspecified; defaulted to match POST).

---

## 1. Requirements (with `assignment.md` citations)

| ID | Requirement | Source line |
|----|-------------|-------------|
| R1 | Use existing project (simple-crm) | §Brief L8 |
| R2 | Add Spring Security starter to `pom.xml` | §1 L9 |
| R3 | `SecurityConfig` in `config` pkg, `@Configuration` + `@EnableWebSecurity` | §1 L10–11 |
| R4 | `PasswordEncoder` bean → `new BCryptPasswordEncoder()` | §1 L12 |
| R5 | `UserDetailsService` with **2** in-memory users: `user`/`user123` (USER), `admin`/`admin123` (ADMIN) | §1 L13–15 |
| R6 | Encode passwords via `passwordEncoder.encode()` | §1 L16 |
| R7 | Test: no-creds request blocked; Basic Auth login works for both users | §1 L17–21 |
| R8 | `SecurityFilterChain` bean in `SecurityConfig` | §2 L24 |
| R9 | Disable CSRF `.csrf(csrf -> csrf.disable())` | §2 L26 |
| R10 | Enable Basic Auth `.httpBasic(Customizer.withDefaults())` | §2 L27 |
| R11 | One public endpoint `.permitAll()` (e.g. `GET /health`) | §2 L29 |
| R12 | POST main resource → **ADMIN** only | §2 L30 |
| R13 | DELETE → **ADMIN** only | §2 L31 |
| R14 | GET → all authenticated (USER + ADMIN) | §2 L32 |
| R15 | user: GET 200 / POST·DELETE 403; admin: all 200/201 | §2 L33–38 |
| R16 | Document test results (screenshots/descriptions) | §2 L39 |
| R17 | Submit GitHub repo URL; credit references | §Submission L43–44 |

## 2. Current state (verified against source)

**Already satisfied — no change:**
- **R2** — `pom.xml` L80–88: `spring-boot-starter-security` + `spring-boot-starter-security-test` present.
- **R3** — `SecurityConfig.java` L24–25: `@Configuration` + `@EnableWebSecurity` present.
- **R4** — `SecurityConfig.java` L63–66: `PasswordEncoder` → `BCryptPasswordEncoder`.
- **R8/R9** — `SecurityConfig.java` L34–61: `SecurityFilterChain` present; CSRF disabled (L41).

**Gaps / conflicts to build:**
- **R5** — `userDetailsService()` (L68–97) has **3 env-driven** users (user/admin/**manager**, USER/ADMIN/**MANAGER**) read from `@Value("${app.auth.*}")` — not the 2 fixed users required.
- **R10** — no `.httpBasic(...)`; chain is STATELESS JWT with `addFilterBefore(jwtAuthFilter, ...)` (L58).
- **R11–R14** — no RBAC; chain is `/auth/login` permitAll + `anyRequest().authenticated()` only.
- **Conflict** — `security/JwtAuthFilter.java` L48–51 sets **empty authorities** ("TRAINING ONLY"); a JWT user has no roles → any `hasRole(...)` rule would 403 them. Resolved by removing the JWT stack (Option A).

**Full main-source file inventory** (`src/main/java/sg/edu/ntu/m3_simple_crm/`):
- `M3SimpleCrmApplication.java`
- `auth/`: `AuthController.java`, `LoginRequest.java`, `TokenResponse.java`
- `config/`: `AppConfig.java` (exports `AuthenticationManager`), `SecurityConfig.java`
- `controller/`: `CustomerController.java`
- `exceptions/`: `ErrorResponse.java`, `CustomerNotFoundException.java`, `GlobalExceptionHandler.java`
- `model/`: `Customer.java`, `Interaction.java`, `Course.java`, `Student.java`
- `repository/`: `CustomerRepository.java`, `InteractionRepository.java`, `CourseRepository.java`
- `security/`: `JwtAuthFilter.java`, `JwtService.java`
- `service/`: `CustomerService.java`, `CustomerServiceImpl.java`, `DemoService.java`, `DataLoader.java`

**Test inventory** (`src/test/java/...`): `M3SimpleCrmApplicationTests.java`, `controller/CustomerControllerTest.java` (4 tests), `service/CustomerServiceImplTest.java`, `service/DemoServiceTest.java`.

**`CustomerController` endpoints:** `POST /customers` (201), `GET /customers` (200), `GET /customers/{id}` (200), `PUT /customers/{id}` (200), `DELETE /customers/{id}` (204), `POST /customers/{id}/interactions` (201), `GET /customers/search?firstName` (200), `GET /customers/search/job?jobTitle` (200).

**`CustomerControllerTest` existing tests (all remain compatible with Option A RBAC):** no-creds `GET /customers/5` → 401; `@WithMockUser(USER)` `GET /customers` → 200; `@WithMockUser(ADMIN)` `POST /customers` valid → 201; `@WithMockUser(ADMIN)` `POST /customers` invalid → 400.

**Profiles:** `application.properties` → `spring.profiles.active=dev`. `dev` (Postgres `localhost:5432/simple_crm`, port 8080, `ddl-auto=create`), `prod` (port 8081), `test` (H2 `mem:crm-tests`, `create-drop`). `.env` is gitignored; `.env.example` is the tracked template.

## 3. Core decision: Option A — Replace

**Why:** the assignment mandates **Basic Auth + 2 fixed users + RBAC**; the app runs **JWT (stateless) + 3 env users + empty-authority filter**. Adding RBAC `hasRole` rules would 403 every JWT user (empty authorities). Option A makes Basic Auth the sole mechanism — exactly matches the assignment and is the simplest. Trade-off accepted: the prior JWT work is removed. (Chosen by the user; Option C profile-gate was the fallback if JWT were needed for a later PT.)

## 4. Concrete file changes

### 4a. Delete (JWT stack)
- `security/JwtAuthFilter.java`
- `security/JwtService.java`
- `auth/AuthController.java`
- `auth/LoginRequest.java`
- `auth/TokenResponse.java`
- `config/AppConfig.java` — remove the `authenticationManager(...)` bean (its only consumer was `AuthController`). *If `AppConfig` holds nothing else, deleting the file is fine; if it holds other beans, keep the class and drop only that bean.*

### 4b. `config/SecurityConfig.java` — rewrite
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                                  // R9
            .sessionManagement(s -> s.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll()                    // R11
                .requestMatchers(HttpMethod.POST,   "/customers/**").hasRole("ADMIN")   // R12
                .requestMatchers(HttpMethod.PUT,    "/customers/**").hasRole("ADMIN")   // PUT → ADMIN (default)
                .requestMatchers(HttpMethod.DELETE, "/customers/**").hasRole("ADMIN")   // R13
                .requestMatchers(HttpMethod.GET,    "/customers/**").authenticated()    // R14
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))     // R7 → 401
            .httpBasic(Customizer.withDefaults());                          // R10
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {                              // R4
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder pe) {       // R5 + R6
        var user  = User.builder().username("user")
                .password(pe.encode("user123")).roles("USER").build();
        var admin = User.builder().username("admin")
                .password(pe.encode("admin123")).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(List.of(user, admin));
    }
}
```
Notes: drop the `JwtAuthFilter` field + constructor; remove the `@Value` params; use `InMemoryUserDetailsManager(Iterable)` (non-deprecated). Rules are first-match-wins; non-GET/POST/PUT/DELETE on `/customers` (e.g. PATCH) fall through to `.anyRequest().authenticated()`. `POST /customers/{id}/interactions` matches `POST /customers/**` → ADMIN.

### 4c. Add `controller/HealthController.java` (public endpoint, R11)
```java
package sg.edu.ntu.m3_simple_crm.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
```

### 4d. `pom.xml`
- Remove the three `jjwt-*` dependencies (L89–105: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
- Keep `spring-boot-starter-security` (L80–83) and `spring-boot-starter-security-test` (L84–88).

### 4e. Properties / env (JWT + `app.auth.*` now unused — remove dangling refs)
- `src/main/resources/application-dev.properties` — drop L25–37: `spring.security.user.*`, `app.auth.*`, `jwt.*`.
- `src/test/resources/application-test.properties` — drop `jwt.expiration-ms`, `jwt.secret`, and the `app.auth.*` lines.
- `.env.example` — drop `SPRING_SECURITY_USER_PASSWORD`, `APP_JWT_SECRET`, `APP_USER_*`, `APP_ADMIN_*`, `APP_MANAGER_*`.
- ⚠️ Do the **deletes (4a) and property trims (4e) in the same commit** so the app never starts in a state where `jwt.secret`/`app.auth.*` are referenced but the bean reading them is gone (or vice versa).

### 4f. `src/test/java/.../controller/CustomerControllerTest.java`
Keep the 4 existing tests. **Add:**
- `@WithMockUser(roles = "USER")` → `POST /customers` = **403**
- `@WithMockUser(roles = "USER")` → `PUT /customers/{id}` = **403**
- `@WithMockUser(roles = "USER")` → `DELETE /customers/{id}` = **403** (no data needed — rejected by security before the controller)
- `@WithMockUser(roles = "ADMIN")` → `PUT /customers/{id}` = **200**
- `@WithMockUser(roles = "ADMIN")` → `DELETE /customers/{id}` = **204** (create a customer first via `customerRepository.save(...)` to satisfy the "exists" path, then delete)
- `GET /health` with **no** credentials = **200**

## 5. Acceptance matrix (manual verification, R7 + R15)

| Request (HTTP Basic) | Expected |
|---|---|
| no creds → `GET /customers` | **401** |
| no creds → `GET /health` | **200** |
| `user:user123` → `GET /customers` | **200** |
| `user:user123` → `POST /customers` | **403** |
| `user:user123` → `PUT /customers/{id}` | **403** |
| `user:user123` → `DELETE /customers/{id}` | **403** |
| `admin:admin123` → `GET /customers` | **200** |
| `admin:admin123` → `POST /customers` | **201** |
| `admin:admin123` → `PUT /customers/{id}` | **200** |
| `admin:admin123` → `DELETE /customers/{id}` | **204** |

curl examples:
```bash
curl -i http://localhost:8080/customers                 # 401
curl -i http://localhost:8080/health                    # 200
curl -i -u user:user123   http://localhost:8080/customers        # 200
curl -i -u user:user123 -X POST -H 'Content-Type: application/json' -d '{"firstName":"A","lastName":"B","email":"a@b.co","contactNo":"1","jobTitle":"X","yearOfBirth":1990}' http://localhost:8080/customers  # 403
curl -i -u admin:admin123 -X POST -H 'Content-Type: application/json' -d '{...valid...}' http://localhost:8080/customers  # 201
curl -i -u admin:admin123 -X DELETE http://localhost:8080/customers/1   # 204
```

## 6. Ordered Build steps
1. **Delete** `security/JwtAuthFilter.java`, `security/JwtService.java`, `auth/AuthController.java`, `auth/LoginRequest.java`, `auth/TokenResponse.java`; remove `AppConfig`'s `AuthenticationManager` bean.
2. **Rewrite** `config/SecurityConfig.java` (section 4b).
3. **Add** `controller/HealthController.java` (section 4c).
4. **Trim** `pom.xml` — remove the three `jjwt-*` deps.
5. **Trim** `application-dev.properties`, `application-test.properties`, `.env.example` (section 4e). *Combine with step 1.*
6. **Extend** `controller/CustomerControllerTest.java` with the RBAC + `/health` cases (section 4f).
7. **`./mvnw test`** — all tests green (old 4 + new).
8. **`./mvnw spring-boot:run`** (dev profile); run the section-5 acceptance matrix via curl/Postman.
9. **Document** results (R16) — screenshots or written matrix.
10. **Submit** (R17) — push to the NTU GitHub repo; add reference/credit notes.

## 7. Pending / open items
- **R16 documentation** — not yet produced; Build to capture the section-5 matrix (screenshots recommended per the assignment).
- **R17 submission** — not yet done; push + credits.
- **`PUT` policy** — defaulted to ADMIN. If the instructor expects "any authenticated" for PUT, change `SecurityConfig` L (PUT line) from `.hasRole("ADMIN")` to `.authenticated()`.
- **`/health` vs actuator** — a tiny custom `HealthController` is used (Actuator is not in the pom). If Actuator is preferred, add `spring-boot-starter-actuator` and expose `/actuator/health` instead, and permit `/actuator/health` in the chain.
- **DB for manual run** — dev profile uses local Postgres (`simple_crm`). If unavailable, run manual verification with a profile pointed at H2, or seed via the existing `DataLoader`.

## 8. Next action
Switch to the **Build** agent and execute section 6 in order. No source has been modified by planning. After step 7 (`./mvnw test`) is green and step 8's matrix matches section 5, the code is complete; then do R16 (document) and R17 (submit).
