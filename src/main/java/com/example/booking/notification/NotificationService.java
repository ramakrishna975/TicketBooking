package com.example.booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stubbed notifications. A real implementation would send email/SMS/push; here we log,
 * which is enough to demonstrate ticket issuance and booking reminders end-to-end.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void issueTickets(String toEmail, Long bookingId) {
        log.info("[TICKETS] Issued tickets for booking {} to {}", bookingId, toEmail);
    }

    public void sendBookingReminder(String toEmail, Long bookingId) {
        log.info("[REMINDER] Reminding {} about upcoming booking {}", toEmail, bookingId);
    }
}

/*
 * ============================================================================
 * FILE ROLE: The stubbed notification channel.
 * WHAT IT DOES: issueTickets(...) and sendBookingReminder(...) currently LOG the
 *   action (a stand-in for email/SMS/push).
 * TECHNICAL CONCEPTS: A single seam where a real provider would be integrated.
 *   Called by the after-commit listener (ticket issuance) and the scheduler
 *   (reminders), keeping "how we notify" separate from "when we notify".
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The part that "tells people things" - issue your tickets, or remind you about
 * an event. For now it just writes a note in the app's log instead of really
 * sending an email or text. It is the single spot where you would later connect a
 * real email or SMS service.
 * ============================================================================
 */
