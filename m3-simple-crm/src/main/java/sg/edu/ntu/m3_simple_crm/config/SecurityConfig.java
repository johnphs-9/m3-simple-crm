package sg.edu.ntu.m3_simple_crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import sg.edu.ntu.m3_simple_crm.security.JwtAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthFilter jwtAuthFilter;

        SecurityConfig(JwtAuthFilter jwtAuthFilter) {
                this.jwtAuthFilter = jwtAuthFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                // REST APIs using stateless JWT auth do not need CSRF protection.
                                // CSRF is designed for browser-based session flows; since we never issue
                                // a session cookie, there is nothing for a cross-site request to hijack.
                                .csrf(csrf -> csrf.disable())

                                // Stateless: Spring Security will not create or use sessions.
                                // Every request must carry a valid JWT token.
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // /auth/login is public; everything else needs a valid token
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/auth/login").permitAll()
                                                .anyRequest().authenticated())

                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                                // Register JWT filter to run before username/password authentication
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService(
                        PasswordEncoder passwordEncoder,
                        @Value("${app.auth.user.username}") String userUsername,
                        @Value("${app.auth.user.password}") String userPassword,
                        @Value("${app.auth.admin.username}") String adminUsername,
                        @Value("${app.auth.admin.password}") String adminPassword,
                        @Value("${app.auth.manager.username}") String managerUsername,
                        @Value("${app.auth.manager.password}") String managerPassword) {

                UserDetails user = User.builder()
                                .username(userUsername)
                                .password(passwordEncoder.encode(userPassword))
                                .roles("USER")
                                .build();

                UserDetails admin = User.builder()
                                .username(adminUsername)
                                .password(passwordEncoder.encode(adminPassword))
                                .roles("ADMIN")
                                .build();

                UserDetails manager = User.builder()
                                .username(managerUsername)
                                .password(passwordEncoder.encode(managerPassword))
                                .roles("MANAGER")
                                .build();

                return new InMemoryUserDetailsManager(user, admin, manager);
        }

}
