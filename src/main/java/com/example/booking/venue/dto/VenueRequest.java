package com.example.booking.venue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 250) String address,
        @NotBlank @Size(max = 100) String city,
        @Positive @Min(1) int capacity) {
}

/*
 * ============================================================================
 * FILE ROLE: Request DTO for creating a venue.
 * TECHNICAL CONCEPTS: Validated record (@NotBlank/@Positive/@Min); @Valid in the
 *   controller enforces it, producing a 400 with field errors on failure.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of the "add a venue" form, with simple checks: the name cannot be
 * blank and the capacity must be at least 1.
 * ============================================================================
 */
