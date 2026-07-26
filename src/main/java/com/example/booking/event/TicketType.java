package com.example.booking.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A priced tier of tickets for an event (e.g. General, VIP). Holds the seat/capacity
 * count. {@code quantityAvailable} is decremented when a booking holds seats and
 * restored when a hold expires — guarded by {@link Version} optimistic locking, since
 * concurrent bookings race on exactly this field.
 */
@Entity
@Table(name = "ticket_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String name;

    /** Unit price in minor units (cents) to avoid floating point money. */
    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "quantity_total", nullable = false)
    private int quantityTotal;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Version
    private long version;
}

/*
 * ============================================================================
 * FILE ROLE: A priced ticket tier of an event (table "ticket_type"). Holds the
 *            SEAT COUNT.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Belongs to one Event; stores name, price (in cents), currency,
 *     quantityTotal and the live quantityAvailable, plus a @Version.
 *
 * TECHNICAL CONCEPTS
 *   - This is the entity at the centre of the OVERSELLING problem. quantityAvailable
 *     is decremented when a booking holds seats; @Version + an optimistic lock (see
 *     TicketTypeRepository) ensure two concurrent holds on the last seats cannot
 *     both succeed.
 *   - Money is stored as a long of MINOR UNITS (cents) to avoid floating-point
 *     rounding errors.
 *   - @ManyToOne(LAZY) to Event: the parent is loaded only when accessed.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The blueprint for a ticket "tier" - like "General" or "VIP". It stores the
 * price, the currency, how many exist in total, and how many are still AVAILABLE
 * right now.
 *
 * That "available" number is the important one: it goes down as people book, and
 * back up if a booking is cancelled or expires. The small "version" number keeps
 * things correct when two people try to book the last seats at the same instant.
 *
 * Note: prices are stored as whole numbers of cents (e.g. 5000 = 50.00) so we
 * never get rounding mistakes with money.
 * ============================================================================
 */
