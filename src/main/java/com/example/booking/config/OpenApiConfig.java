package com.example.booking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger metadata. Declares a single {@code bearerAuth} JWT scheme so the
 * Swagger UI "Authorize" button lets you paste a token from {@code POST /api/auth/login}
 * and call the secured endpoints. The document is served at {@code /v3/api-docs} (JSON),
 * {@code /v3/api-docs.yaml} (YAML), and rendered at {@code /swagger-ui.html}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Booking Platform API",
                version = "0.0.1",
                description = "Event/ticket booking platform — users, events, venues, bookings, "
                        + "seat holds, stubbed payment, and admin moderation.",
                contact = @Contact(name = "Booking Platform"),
                license = @License(name = "Proprietary")),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local (dev)")
        },
        // Applied globally; public endpoints simply ignore the header.
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT obtained from POST /api/auth/login. Send as: Authorization: Bearer <token>",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}

/*
 * ============================================================================
 * FILE ROLE: Swagger / OpenAPI metadata and JWT "Authorize" support.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Supplies the API title/description/version and declares a single security
 *     scheme "bearerAuth" (HTTP bearer, format JWT) applied globally, so the
 *     Swagger UI shows an "Authorize" button and sends the token on calls.
 *
 * TECHNICAL CONCEPTS
 *   - OpenAPI is a machine-readable description of a REST API; Swagger UI renders
 *     it interactively. The springdoc-openapi dependency GENERATES the document
 *     automatically at runtime by scanning the @RestControllers and DTOs via
 *     reflection - this class only adds human-friendly metadata + the auth scheme.
 *   - Served at /v3/api-docs (JSON), /v3/api-docs.yaml (YAML), /swagger-ui.html.
 *   - These paths are opened without authentication in SecurityConfig.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * When you build an API, it is really helpful to have (1) an always-up-to-date
 * instruction manual of every endpoint, and (2) a web page where you can click
 * buttons to try them out. A tool we added builds both AUTOMATICALLY by reading
 * our code.
 *
 * This file does not build that manual - it just adds the "cover page" details
 * (the API's name and description) and an "Authorize" button so you can paste
 * your login token and then test the protected actions from the browser.
 *
 * Open http://localhost:8080/swagger-ui.html when the app is running to see it.
 * ============================================================================
 */
