package com.example.booking.event.dto;

import com.example.booking.event.Event;
import com.example.booking.event.EventStatus;

import java.time.Instant;
import java.util.List;

public record EventResponse(
        Long id,
        String title,
        String description,
        Long venueId,
        String venueName,
        Long organizerId,
        Instant startsAt,
        Instant endsAt,
        EventStatus status,
        List<TicketTypeResponse> ticketTypes) {

    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getVenue().getId(),
                e.getVenue().getName(),
                e.getOrganizer().getId(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getStatus(),
                e.getTicketTypes().stream().map(TicketTypeResponse::from).toList());
    }
}

/*
 * ============================================================================
 * FILE ROLE: Response DTO for an event (with venue name + ticket tiers).
 * TECHNICAL CONCEPTS: from(Event) flattens the aggregate into a client-friendly
 *   shape. It reads venue/ticketTypes, which is why the repository queries
 *   @EntityGraph-fetch them - so this mapping never hits a lazy-load error.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of an event we send back to the client, including the venue's name
 * and all of its ticket tiers. It is built by copying the needed fields out of
 * the event - we never hand the raw database object to the outside world.
 * ============================================================================
 */
