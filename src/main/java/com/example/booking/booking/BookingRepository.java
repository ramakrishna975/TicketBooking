package com.example.booking.booking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"items", "items.ticketType", "event"})
    Optional<Booking> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.ticketType", "event"})
    List<Booking> findByAttendeeId(Long attendeeId);

    /** Stale seat-holds the scheduler must expire. */
    @EntityGraph(attributePaths = {"items", "items.ticketType"})
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant cutoff);

    /** PAID bookings for events starting soon — reminder scheduler. */
    List<Booking> findByStatus(BookingStatus status);
}

/*
 * ============================================================================
 * FILE ROLE: Data-access for bookings.
 * WHAT IT DOES: findWithItemsById + findByAttendeeId (both @EntityGraph-fetch
 *   items/tier/event), findByStatusAndExpiresAtBefore (the stale-hold sweep),
 *   findByStatus (reminders).
 * TECHNICAL CONCEPTS: @EntityGraph avoids lazy-loading errors when items are
 *   mapped to DTOs after the transaction. Derived query names encode the WHERE
 *   clause the scheduler needs (status = PENDING AND expiresAt < now).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "librarian" for bookings. It can: fetch one booking together with its lines,
 * list all of your bookings, find holds that have expired (so we can clean them
 * up), and find paid bookings (so we can send reminders). Like the other
 * librarians, it also fetches the related lines in the same trip to avoid errors
 * later.
 * ============================================================================
 */
