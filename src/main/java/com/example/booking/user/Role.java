package com.example.booking.user;

/** Application roles. Stored as strings; used as Spring Security authorities {@code ROLE_*}. */
public enum Role {
    ATTENDEE,
    ORGANIZER,
    ADMIN
}

/*
 * ============================================================================
 * FILE ROLE: The set of user roles.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Enumerates ATTENDEE, ORGANIZER, ADMIN.
 *
 * TECHNICAL CONCEPTS
 *   - A Java ENUM gives a fixed, type-safe set of values (you can't store an
 *     invalid role). Stored as a string on the User entity, and surfaced to
 *     Spring Security as authority "ROLE_" + name (e.g. ROLE_ORGANIZER) for
 *     hasRole(...) / hasAnyRole(...) checks.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * A short, fixed list of the only three kinds of users the app allows:
 *   - ATTENDEE  = a fan who books tickets,
 *   - ORGANIZER = a promoter who creates events,
 *   - ADMIN     = staff who moderate.
 *
 * Using a fixed list (instead of free text) means nobody can accidentally invent
 * an invalid role like "manager" - only these three are possible.
 * ============================================================================
 */
