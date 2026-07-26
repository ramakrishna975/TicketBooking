package com.example.booking.event;

/** Lifecycle of an event. Only PUBLISHED events accept bookings. */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED
}

/*
 * ============================================================================
 * FILE ROLE: The lifecycle states of an event.
 * WHAT IT DOES: DRAFT -> PUBLISHED -> CANCELLED. Only PUBLISHED events accept
 *   bookings and appear in the public listing.
 * TECHNICAL CONCEPTS: A STATE MACHINE expressed as an enum; the allowed
 *   transitions are enforced in EventService (e.g. cannot publish a cancelled
 *   event). Stored as a string on the Event entity.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The three stages of an event's life:
 *   - DRAFT     = being prepared, still private,
 *   - PUBLISHED = live, people can buy tickets,
 *   - CANCELLED = called off.
 * Only a PUBLISHED event can be booked.
 * ============================================================================
 */
