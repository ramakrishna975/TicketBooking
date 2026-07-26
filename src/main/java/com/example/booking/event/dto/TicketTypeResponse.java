package com.example.booking.event.dto;

import com.example.booking.event.TicketType;

public record TicketTypeResponse(Long id, String name, long priceCents, String currency,
                                 int quantityTotal, int quantityAvailable) {

    public static TicketTypeResponse from(TicketType tt) {
        return new TicketTypeResponse(tt.getId(), tt.getName(), tt.getPriceCents(),
                tt.getCurrency(), tt.getQuantityTotal(), tt.getQuantityAvailable());
    }
}

/*
 * ============================================================================
 * FILE ROLE: Response DTO for a ticket tier (includes live availability).
 * TECHNICAL CONCEPTS: from(TicketType) projection; exposes quantityAvailable so
 *   clients can see remaining seats.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of a ticket tier we send back to the client, including how many seats
 * are still left - so a buyer can see availability.
 * ============================================================================
 */
