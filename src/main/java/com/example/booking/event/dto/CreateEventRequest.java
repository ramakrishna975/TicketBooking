package com.example.booking.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @jakarta.validation.constraints.NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull Long venueId,
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        @NotEmpty @Valid List<TicketTypeRequest> ticketTypes) {
}

/*
 * ============================================================================
 * FILE ROLE: Request DTO for creating an event with its ticket tiers.
 * TECHNICAL CONCEPTS: @Future ensures dates are upcoming; @NotEmpty + @Valid on
 *   the ticketTypes list triggers NESTED validation of each tier. Cross-field
 *   rules that annotations can't express (end>start, capacity) live in the service.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of the whole "create event" form: the event details PLUS at least
 * one ticket tier. It checks the start and end dates are in the future and that
 * each tier is valid. The trickier rules (end after start, tickets fit the venue)
 * are checked in the service.
 * ============================================================================
 */
