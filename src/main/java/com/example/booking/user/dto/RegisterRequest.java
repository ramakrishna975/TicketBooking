package com.example.booking.user.dto;

import com.example.booking.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. {@code role} lets a caller self-register as ATTENDEE or
 * ORGANIZER; creating ADMINs is restricted in the service layer.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotNull Role role) {
}

/*
 * ============================================================================
 * FILE ROLE: Request DTO for registration.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES: Carries email/password/displayName/role from the client, with
 *   validation annotations (@Email, @NotBlank, @Size, @NotNull).
 * TECHNICAL CONCEPTS: A Java RECORD = immutable data carrier. Bean Validation
 *   annotations are checked by @Valid in the controller; entities are never bound
 *   directly to HTTP input (DTO pattern) - safer and decoupled from the schema.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is the exact SHAPE of the sign-up form data: email, password, display
 * name, and role. It carries built-in checks - the email must look like an
 * email, the password must be long enough, nothing can be blank.
 *
 * If any check fails, the request is politely rejected with a clear message
 * BEFORE any real work happens. Think of it as the form validation on a website.
 * ============================================================================
 */
