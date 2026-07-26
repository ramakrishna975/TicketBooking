package com.example.booking.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Listing read path — eagerly fetch ticket types and venue to avoid N+1. */
    @EntityGraph(attributePaths = {"venue", "ticketTypes"})
    List<Event> findByStatus(EventStatus status);

    @EntityGraph(attributePaths = {"venue", "ticketTypes", "organizer"})
    Optional<Event> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"venue", "ticketTypes"})
    List<Event> findByOrganizerId(Long organizerId);
}

/*
 * ============================================================================
 * FILE ROLE: Data-access for events, tuned to avoid N+1 queries.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - findByStatus (listing), findWithDetailsById (detail), findByOrganizerId
 *     (my events) - all annotated with @EntityGraph to eagerly fetch venue and
 *     ticketTypes in one query.
 *
 * TECHNICAL CONCEPTS
 *   - THE N+1 PROBLEM: lazily loading each event's venue/tiers one-by-one causes
 *     many queries. @EntityGraph tells JPA to JOIN-fetch those associations up
 *     front, so the DTO mapping (done after the transaction) doesn't trigger a
 *     LazyInitializationException. (This file's findByOrganizerId graph is the fix
 *     for the /api/events/mine 500 we found during API testing.)
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "librarian" for events, with one clever habit: when it fetches events, it
 * also grabs each event's venue and ticket tiers IN THE SAME TRIP to the
 * database.
 *
 * Why? Because a moment later we turn those events into the reply we send back,
 * and that reply needs the venue name and tiers. If we had not fetched them
 * together, the app would error out when trying to read them afterwards. (This is
 * exactly the bug we found and fixed on the "my events" screen.)
 * ============================================================================
 */
