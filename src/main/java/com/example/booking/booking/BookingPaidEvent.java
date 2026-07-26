package com.example.booking.booking;

/**
 * Published after a booking's payment transaction commits. Consumed by an
 * AFTER_COMMIT listener to issue tickets / notify — never inside the pay transaction,
 * so a post-commit failure can't roll back a captured payment.
 */
public record BookingPaidEvent(Long bookingId) {
}

/*
 * ============================================================================
 * FILE ROLE: The in-process domain event signalling "a booking was paid".
 * WHAT IT DOES: A tiny immutable record carrying the bookingId, published by
 *   BookingService.pay(...).
 * TECHNICAL CONCEPTS: DOMAIN EVENTS decouple "what happened" (paid) from "what to
 *   do about it" (issue tickets/notify). Publisher and listener don't know each
 *   other directly - they communicate via Spring's ApplicationEventPublisher.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * A tiny "announcement" that simply says: "booking number X was paid."
 *
 * The code that takes payment does not directly issue the tickets. Instead it
 * just SHOUTS this announcement, and another part of the app listens and reacts.
 * Keeping the two jobs separate makes the code cleaner and easier to change.
 * ============================================================================
 */
