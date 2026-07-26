package com.example.booking.booking;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Periodic maintenance for the booking lifecycle:
 *  - expire stale seat-holds so held-but-unpaid seats return to inventory;
 *  - remind attendees of upcoming (soon-to-start) paid bookings.
 * Intervals are configured under {@code booking.hold.*} / {@code booking.reminder.*}.
 */
@Component
public class BookingScheduler {

    private static final Duration REMINDER_HORIZON = Duration.ofHours(24);

    private final BookingService bookingService;

    public BookingScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${booking.hold.sweep-interval-ms}")
    public void expireStaleHolds() {
        bookingService.expireStaleHolds();
    }

    @Scheduled(fixedDelayString = "${booking.reminder.interval-ms}")
    public void sendReminders() {
        bookingService.sendUpcomingReminders(REMINDER_HORIZON);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Background jobs for the booking lifecycle.
 * WHAT IT DOES: Periodically (a) expires stale seat-holds so unpaid seats return
 *   to inventory, and (b) sends reminders for soon-starting paid bookings.
 * ----------------------------------------------------------------------------
 * TECHNICAL CONCEPTS
 *   - @Scheduled(fixedDelayString="${booking.hold.sweep-interval-ms}") runs a
 *     method on a timer; enabled app-wide by @EnableScheduling. Intervals are
 *     externalised to config (booking.hold.* / booking.reminder.*).
 *   - This is what makes "a hold EXPIRES if not paid in time" actually happen -
 *     the expiry timestamp alone does nothing without a sweeper.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The app's "alarm clock." On a repeating timer it does two chores all by itself,
 * with nobody clicking anything:
 *   1) free up seats from bookings that were never paid in time, and
 *   2) send reminders for events that are coming up soon.
 *
 * Without this, the "seats expire if you do not pay" promise would never actually
 * happen - a timestamp alone does nothing until something checks it.
 * ============================================================================
 */
