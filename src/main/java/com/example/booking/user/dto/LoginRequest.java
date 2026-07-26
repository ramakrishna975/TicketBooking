package com.example.booking.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password) {
}

/*
 * ============================================================================
 * FILE ROLE: Request DTO for login (email + password).
 * TECHNICAL CONCEPTS: Immutable record; @NotBlank ensures both fields are
 *   present before the AuthenticationManager is invoked.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of the login form: just an email and a password, both required.
 * Nothing fancy - it simply carries what you typed to the login logic.
 * ============================================================================
 */
