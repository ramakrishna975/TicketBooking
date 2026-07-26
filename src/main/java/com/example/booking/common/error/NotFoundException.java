package com.example.booking.common.error;

/** Thrown when a requested aggregate/resource does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, Object id) {
        return new NotFoundException(what + " not found: " + id);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Domain exception for "requested resource does not exist" (-> 404).
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - A RuntimeException thrown by services when an id/email is not found; the
 *     of(what, id) factory builds a consistent message.
 *
 * TECHNICAL CONCEPTS
 *   - A CUSTOM DOMAIN EXCEPTION lets business code express intent ("not found")
 *     without knowing about HTTP. GlobalExceptionHandler maps it to a 404
 *     RFC 7807 ProblemDetail - clean separation of concerns.
 *   - Unchecked (extends RuntimeException) so it doesn't clutter method
 *     signatures and rolls back @Transactional work by default.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * Sometimes the code is asked for something that does not exist - like "give me
 * user number 500" when there is no such user.
 *
 * Instead of crashing in a messy way, the code raises a clear, named signal that
 * simply means "I could not find it." Somewhere else in the app, one helper sees
 * this signal and turns it into a proper "404 Not Found" reply for the user.
 *
 * Think of it as raising your hand and saying exactly what went wrong, so someone
 * can respond politely - rather than the whole program falling over.
 * ============================================================================
 */
