package com.example.booking.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TicketTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @PositiveOrZero long priceCents,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code") String currency,
        @Min(1) int quantityTotal) {
}

/*
 * ============================================================================
 * FILE ROLE: Request DTO for one ticket tier inside CreateEventRequest.
 * TECHNICAL CONCEPTS: Validated record; @Pattern enforces a 3-letter ISO currency
 *   code, @Min(1) a positive quantity. Nested validation is driven by @Valid on
 *   the parent's list.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of ONE ticket tier when creating an event: its name, price,
 * currency, and how many exist - with simple checks (for example, currency must
 * be a 3-letter code like USD, and quantity must be at least 1).
 * ============================================================================
 */
