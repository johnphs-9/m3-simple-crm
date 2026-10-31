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
