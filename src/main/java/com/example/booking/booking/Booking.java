package com.example.booking.booking;

import com.example.booking.event.Event;
import com.example.booking.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking aggregate root. Carries {@link Version} for optimistic locking: the same
 * booking must not be paid and expired concurrently (e.g. the scheduler expiring it
 * while the attendee pays). The seat-count race is guarded separately on TicketType.
 */
@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendee_id", nullable = false)
    private User attendee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When a PENDING hold lapses. Null once PAID/CANCELLED/EXPIRED. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Version
    private long version;

    public void addItem(BookingItem item) {
        item.setBooking(this);
        items.add(item);
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

/*
 * ============================================================================
 * FILE ROLE: The Booking aggregate root (table "booking").
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Links to an Event and an attendee (User), holds a status, its line items
 *     (cascade), total, currency, optional payment reference, createdAt, an
 *     expiresAt (while PENDING), and a @Version.
 *
 * TECHNICAL CONCEPTS
 *   - @Version here guards the "pay vs. scheduler-expire the SAME booking" race:
 *     both actions bump the version, so only one wins - no corrupt state.
 *   - expiresAt drives the seat-HOLD window; it is cleared once PAID/CANCELLED/
 *     EXPIRED. Total is a long of cents.
 *   - Aggregate root: items cascade with the booking.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The blueprint for a booking, and its "hub": who booked, for which event, its
 * status, its lines (the items), the total price, when it was made, and when the
 * seat-hold expires.
 *
 * The small "version" number keeps things safe if two actions happen at the same
 * moment - for example, you clicking "pay" at the exact instant the timer tries
 * to expire the same booking. Only one of them can win, so the booking never ends
 * up in a broken state.
 * ============================================================================
 */
