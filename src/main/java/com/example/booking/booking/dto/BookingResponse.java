package com.example.booking.booking.dto;

import com.example.booking.booking.Booking;
import com.example.booking.booking.BookingStatus;

import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        Long eventId,
        Long attendeeId,
        BookingStatus status,
        long totalCents,
        String currency,
        String paymentReference,
        Instant createdAt,
        Instant expiresAt,
        List<Item> items) {

    public record Item(Long ticketTypeId, String ticketTypeName, int quantity, long unitPriceCents) {
    }

    public static BookingResponse from(Booking b) {
        List<Item> items = b.getItems().stream()
                .map(i -> new Item(i.getTicketType().getId(), i.getTicketType().getName(),
                        i.getQuantity(), i.getUnitPriceCents()))
                .toList();
        return new BookingResponse(
                b.getId(),
                b.getEvent().getId(),
                b.getAttendee().getId(),
                b.getStatus(),
                b.getTotalCents(),
                b.getCurrency(),
                b.getPaymentReference(),
                b.getCreatedAt(),
                b.getExpiresAt(),
                items);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Response DTO for a booking (with its line items).
 * TECHNICAL CONCEPTS: from(Booking) projects the aggregate (status, total,
 *   currency, paymentReference, expiresAt, items) for the client - never the
 *   entity. Items are available because the repository @EntityGraph-fetches them.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of a booking we send back to the client: its lines, total price,
 * status, and payment reference. As always, we copy out the needed fields rather
 * than exposing the raw database object.
 * ============================================================================
 */
