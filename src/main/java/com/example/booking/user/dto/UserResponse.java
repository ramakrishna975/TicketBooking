package com.example.booking.user.dto;

import com.example.booking.user.Role;
import com.example.booking.user.User;

import java.time.Instant;

/** Public projection of a user — never expose the entity or password hash. */
public record UserResponse(Long id, String email, String displayName, Role role,
                           boolean enabled, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(),
                u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}

/*
 * ============================================================================
 * FILE ROLE: Response DTO = the public projection of a User.
 * WHAT IT DOES: from(User) copies safe fields (id, email, name, role, enabled,
 *   createdAt) - deliberately NO password hash.
 * TECHNICAL CONCEPTS: The DTO boundary prevents leaking sensitive/internal entity
 *   fields onto the wire and decouples the API contract from the database model.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of user info we send BACK to the client. The most important thing to
 * notice: it never includes the password (not even the scrambled one) - only
 * safe fields like name, email, and role. This is how we avoid accidentally
 * leaking private data.
 * ============================================================================
 */
