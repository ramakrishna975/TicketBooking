package com.example.booking.venue.dto;

import com.example.booking.venue.Venue;

public record VenueResponse(Long id, String name, String address, String city, int capacity) {

    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getAddress(), v.getCity(), v.getCapacity());
    }
}

/*
 * ============================================================================
 * FILE ROLE: Response DTO for a venue (from(Venue) mapper).
 * TECHNICAL CONCEPTS: DTO projection keeps the API contract separate from the
 *   entity/schema.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of venue information we send back to the client (id, name, address,
 * city, capacity).
 * ============================================================================
 */
