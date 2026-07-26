package com.example.booking.common.error;

/**
 * Thrown when a request conflicts with current state — duplicate registration,
 * sold-out ticket type, illegal booking transition, etc. Maps to 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Domain exception for state conflicts (-> 409).
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Thrown when a request clashes with current state: duplicate email,
 *     sold-out ticket tier, illegal status transition, unowned resource, etc.
 *
 * TECHNICAL CONCEPTS
 *   - HTTP 409 CONFLICT is the correct status for "your request is well-formed
 *     but conflicts with the resource's current state" (versus 400 for malformed
 *     input or 404 for missing). Mapped centrally in GlobalExceptionHandler.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is another "raise your hand" signal, but for a different situation: the
 * request is understandable, yet it clashes with how things currently are.
 *
 * Examples: signing up with an email that is already taken, or trying to buy
 * more seats than are left. The app turns this signal into a "409 Conflict"
 * reply, which basically means "makes sense, but it does not fit the current
 * situation."
 *
 * Having clearly named signals like this keeps the code easy to read and keeps
 * the error messages users see consistent.
 * ============================================================================
 */
