package com.example.booking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security. URL rules give a coarse first line of defence; fine-grained
 * ownership checks live on service/controller methods via {@code @PreAuthorize}
 * (method security is enabled here).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuthenticationEntryPoint authEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          AuthenticationEntryPoint authEntryPoint,
                          AccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public probes and auth endpoints.
                        .requestMatchers("/api/ping", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // API docs (OpenAPI JSON/YAML) and Swagger UI.
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Public browse of events; writes are guarded below and by method security.
                        .requestMatchers(HttpMethod.GET, "/api/events/**", "/api/venues/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider::authenticate;
    }
}

/*
 * ============================================================================
 * FILE ROLE: The security configuration - the app's authentication/authorization
 *            backbone.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Builds the SecurityFilterChain: disables CSRF (we're a stateless token API),
 *     forces SessionCreationPolicy.STATELESS, declares which URLs are public vs
 *     role-restricted, plugs in the JWT filter, and wires the 401/403 handlers.
 *   - Enables method security (@EnableMethodSecurity) so @PreAuthorize works.
 *   - Defines the PasswordEncoder (BCrypt) and the AuthenticationManager
 *     (DaoAuthenticationProvider + our UserDetailsService) used at login.
 *
 * TECHNICAL CONCEPTS
 *   - TWO LAYERS OF AUTHORIZATION: coarse URL rules here (e.g. /api/admin/** ->
 *     ADMIN) plus fine-grained @PreAuthorize on methods, plus ownership checks in
 *     services. Defence in depth.
 *   - STATELESS: no HTTP session/cookie; identity comes from the JWT each request.
 *     CSRF protection is therefore unnecessary and disabled.
 *   - BCrypt is a deliberately slow, salted password hash - the right choice for
 *     storing credentials.
 *   - Public paths include the probes, auth endpoints, public GETs, and the
 *     Swagger/OpenAPI paths.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is the app's RULEBOOK for "who is allowed to go where."
 *
 * It lists:
 *   - pages open to everyone (browsing events, logging in, the API docs),
 *   - pages that need you to be logged in,
 *   - pages that need a specific role (for example, only Admins for admin pages).
 *
 * It also plugs in the security checkpoint (the token filter), says "we do not
 * use old-style server memory of who is logged in - the pass proves it each
 * time," and sets up the tool that scrambles passwords.
 *
 * If you are ever surprised that a request was blocked or allowed, the reason is
 * almost always written in this file.
 * ============================================================================
 */
