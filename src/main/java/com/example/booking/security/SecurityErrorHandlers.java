package com.example.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.net.URI;

/**
 * Renders authentication (401) and authorization (403) failures raised inside the
 * security filter chain as RFC 7807 ProblemDetail JSON, matching the controller advice.
 */
@Configuration
public class SecurityErrorHandlers {

    private static final String PROBLEM_BASE = "https://booking.example.com/problems/";

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        return (request, response, authException) -> write(mapper, response,
                HttpStatus.UNAUTHORIZED, "Authentication failed",
                "Authentication is required to access this resource", "authentication");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (request, response, ex) -> write(mapper, response,
                HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action", "access-denied");
    }

    private void write(ObjectMapper mapper, HttpServletResponse response,
                       HttpStatus status, String title, String detail, String type) throws java.io.IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(PROBLEM_BASE + type));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getWriter(), pd);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Renders security-layer 401/403 failures as RFC 7807 ProblemDetail.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Provides an AuthenticationEntryPoint (401, when no/invalid credentials) and
 *     an AccessDeniedHandler (403, when authenticated but lacking permission),
 *     both writing ProblemDetail JSON.
 *
 * TECHNICAL CONCEPTS
 *   - Errors thrown INSIDE the security filter chain never reach the
 *     @RestControllerAdvice (that only covers exceptions from controllers). So
 *     without these handlers, auth failures would return Spring's default HTML/
 *     empty responses. These handlers make the error shape CONSISTENT with the
 *     controller advice across the whole API.
 *   - They serialise ProblemDetail with Jackson to application/problem+json.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * When the security checkpoint blocks a request, we still want to reply with a
 * friendly, consistent error - not a blank screen or an ugly default page.
 *
 * This file makes sure two common blocks come back cleanly and in the SAME format
 * as every other error in the app:
 *   - 401 = "you need to log in first"
 *   - 403 = "you are logged in, but you are not allowed to do this"
 *
 * Small thing, but it makes the API feel polished and predictable.
 * ============================================================================
 */
