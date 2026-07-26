package com.example.booking.booking;

/**
 * Booking lifecycle.
 * PENDING → holds seats until {@code expiresAt}; PAID on success; EXPIRED if the hold
 * lapses unpaid; CANCELLED if the attendee backs out. Seats are released on EXPIRED/CANCELLED.
 */
public enum BookingStatus {
    PENDING,
    PAID,
    EXPIRED,
    CANCELLED
}

/*
 * ============================================================================
 * FILE ROLE: The lifecycle states of a booking.
 * WHAT IT DOES: PENDING (holds seats) -> PAID; or -> CANCELLED / EXPIRED (both
 *   release the held seats).
 * TECHNICAL CONCEPTS: State machine as an enum; transitions are enforced in
 *   BookingService (e.g. only PENDING can be paid or cancelled). Stored as a
 *   string on the Booking entity.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The stages of a booking:
 *   - PENDING   = seats are held for you, waiting for payment,
 *   - PAID      = payment done, tickets are yours,
 *   - CANCELLED = you backed out (seats returned),
 *   - EXPIRED   = you did not pay in time (seats returned automatically).
 * ============================================================================
 */
