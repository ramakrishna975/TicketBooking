package com.example.booking.booking;

import com.example.booking.event.TicketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A line in a booking: N tickets of one ticket type, priced at hold time. */
@Entity
@Table(name = "booking_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }
}

/*
 * ============================================================================
 * FILE ROLE: A line item within a booking (table "booking_item").
 * WHAT IT DOES: N tickets of one TicketType, with the unit price CAPTURED at hold
 *   time; lineTotalCents() = unitPrice * quantity.
 * TECHNICAL CONCEPTS: Capturing the price on the item (rather than reading the
 *   tier later) means a subsequent price change does not rewrite booking history.
 *   @ManyToOne(LAZY) to both Booking and TicketType.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * One LINE of a booking - for example "2 x VIP". It also remembers the price at
 * the moment you booked. So if the organizer changes prices later, your receipt
 * does not change - you pay what you saw when you booked.
 * ============================================================================
 */
