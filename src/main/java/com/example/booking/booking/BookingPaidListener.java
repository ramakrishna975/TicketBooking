package com.example.booking.booking;

import com.example.booking.notification.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Issues tickets and notifies the attendee once payment is committed.
 * Runs AFTER_COMMIT so it can never see (or roll back) an uncommitted payment; a new
 * transaction is opened because the original one is already complete at this point.
 */
@Component
public class BookingPaidListener {

    private final BookingRepository bookings;
    private final NotificationService notifications;

    public BookingPaidListener(BookingRepository bookings, NotificationService notifications) {
        this.bookings = bookings;
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookingPaid(BookingPaidEvent event) {
        bookings.findWithItemsById(event.bookingId()).ifPresent(booking ->
                notifications.issueTickets(booking.getAttendee().getEmail(), booking.getId()));
    }
}

/*
 * ============================================================================
 * FILE ROLE: Handles BookingPaidEvent AFTER the payment transaction commits.
 * WHAT IT DOES: Reloads the booking and issues tickets / notifies the attendee.
 * ----------------------------------------------------------------------------
 * TECHNICAL CONCEPTS
 *   - @TransactionalEventListener(phase=AFTER_COMMIT) runs the handler ONLY once
 *     the pay() transaction has successfully committed. This guarantees you never
 *     issue tickets for a payment that later rolls back, and a post-commit failure
 *     here can't undo a captured payment.
 *   - @Transactional(REQUIRES_NEW) opens a fresh transaction because the original
 *     one is already complete at AFTER_COMMIT time.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "listener" that hears the "booking paid" announcement and then issues the
 * tickets and sends the confirmation.
 *
 * The important detail: it waits until the payment is FULLY and SAFELY saved
 * before doing anything. That timing matters - it means we never hand out tickets
 * for a payment that did not actually go through.
 * ============================================================================
 */
