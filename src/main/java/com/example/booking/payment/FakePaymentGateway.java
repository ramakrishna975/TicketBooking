package com.example.booking.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Deterministic fake: approves any positive charge and mints a reference. Declines
 * non-positive amounts so the failure path stays exercised in tests.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

    @Override
    public PaymentResult charge(PaymentRequest request) {
        if (request.amountCents() <= 0) {
            return PaymentResult.declined("amount must be positive");
        }
        String reference = "FAKE-" + UUID.randomUUID();
        log.info("Fake-charged booking {} for {} {} -> {}",
                request.bookingId(), request.amountCents(), request.currency(), reference);
        return PaymentResult.ok(reference);
    }
}

/*
 * ============================================================================
 * FILE ROLE: The stubbed payment implementation (no real bank).
 * WHAT IT DOES: Approves any positive charge and mints a "FAKE-<uuid>" reference;
 *   declines non-positive amounts so the failure path stays exercised.
 * TECHNICAL CONCEPTS: A @Component implementing PaymentGateway; Spring injects it
 *   wherever the interface is required. Deterministic behaviour makes tests
 *   reliable. Marked as the single implementation, so no @Qualifier is needed.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * A PRETEND payment system that fills the plug for now. It just says "approved"
 * for any real amount and makes up a fake receipt number. It says "declined" for
 * a zero amount, so we can also test what happens when a payment fails.
 *
 * When the app is ready for real money, a real payment class replaces this one -
 * and nothing else has to change.
 * ============================================================================
 */
