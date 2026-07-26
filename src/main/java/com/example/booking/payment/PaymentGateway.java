package com.example.booking.payment;

/**
 * Payment abstraction. The real gateway is intentionally out of scope — bookings
 * are charged through this interface so the flow is testable without a provider.
 */
public interface PaymentGateway {

    PaymentResult charge(PaymentRequest request);

    record PaymentRequest(Long bookingId, long amountCents, String currency, String customerReference) {
    }

    record PaymentResult(boolean success, String reference, String message) {

        public static PaymentResult ok(String reference) {
            return new PaymentResult(true, reference, "approved");
        }

        public static PaymentResult declined(String message) {
            return new PaymentResult(false, null, message);
        }
    }
}

/*
 * ============================================================================
 * FILE ROLE: The payment ABSTRACTION - a contract, not an implementation.
 * WHAT IT DOES: charge(PaymentRequest) -> PaymentResult (success/reference/message),
 *   with ok()/declined() factories and record request/result types.
 * ----------------------------------------------------------------------------
 * TECHNICAL CONCEPTS
 *   - PROGRAM TO AN INTERFACE: BookingService depends on this interface, not a
 *     concrete provider. Swapping the fake for a real gateway (Stripe/Razorpay)
 *     means adding one class that implements this - no booking code changes.
 *     This is the Dependency Inversion / Strategy pattern in practice.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is a "promise" (a contract) of what ANY payment system must be able to do:
 * "charge this amount and tell me whether it worked."
 *
 * It is not the real payment system - it is the shape of the PLUG. Because the
 * rest of the app only talks to this plug, we can later plug in a real payment
 * company (like Stripe) without changing any of the booking code. That is the
 * whole point of doing it this way.
 * ============================================================================
 */
