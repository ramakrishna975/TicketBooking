package com.example.booking.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateBookingRequest(
        @NotNull Long eventId,
        @NotEmpty @Valid List<Line> items) {

    public record Line(
            @NotNull Long ticketTypeId,
            @Min(1) int quantity) {
    }
}

/*
 * ============================================================================
 * FILE ROLE: Request DTO to create a booking.
 * WHAT IT DOES: { eventId, items:[{ ticketTypeId, quantity>=1 }] }.
 * TECHNICAL CONCEPTS: Nested validated records; @NotEmpty + @Valid validate each
 *   line. The service enforces the harder rules (published event, availability,
 *   single currency).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of the "make a booking" request: which event, and a list of "this
 * tier, this many." It has basic checks (you must pick an event and at least one
 * tier, with a quantity of 1 or more). The harder rules are checked in the
 * service.
 * ============================================================================
 */
