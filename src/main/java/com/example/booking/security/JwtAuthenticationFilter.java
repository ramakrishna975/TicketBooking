package com.example.booking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads a {@code Bearer} token, validates it, and populates the SecurityContext.
 * Stateless — no session is created. Invalid tokens are ignored (request proceeds
 * unauthenticated and is rejected downstream by authorization rules).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Malformed/expired token: leave the context empty.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Turns a Bearer token on the request into an authenticated user.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Runs once per request: if there's an "Authorization: Bearer <token>"
 *     header, it validates the token and puts an Authentication (email + role
 *     authority) into Spring's SecurityContext for the rest of that request.
 *   - Invalid/expired tokens are ignored (context left empty) so the request
 *     proceeds unauthenticated and is rejected later by the authorization rules.
 *
 * TECHNICAL CONCEPTS
 *   - A SERVLET FILTER intercepts every HTTP request before it reaches a
 *     controller. Extending OncePerRequestFilter guarantees it runs a single
 *     time per request even with forwards/includes.
 *   - The SecurityContextHolder is thread-local storage of "who is the current
 *     user"; @PreAuthorize and URL rules read from it downstream.
 *   - Registered before UsernamePasswordAuthenticationFilter in SecurityConfig,
 *     so JWT auth happens early in the chain. Being stateless, nothing is stored
 *     server-side between requests.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * Every request coming into the app passes through a SECURITY CHECKPOINT before
 * it reaches the real code. This file IS that checkpoint.
 *
 * It looks for your pass (the token) in the request. If it is there and genuine,
 * it notes "this request is from Ann, who is an Organizer" so the rest of the app
 * knows who is asking. If there is no pass, or it is fake/expired, it simply lets
 * the request continue as "anonymous" - and the app will refuse anything private
 * a moment later.
 *
 * It runs on EVERY request, automatically. You never call it yourself.
 * ============================================================================
 */
