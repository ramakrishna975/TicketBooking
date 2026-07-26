package com.example.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code booking.*} configuration tree.
 */
@ConfigurationProperties(prefix = "booking")
public record BookingProperties(Jwt jwt, Hold hold, Reminder reminder) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    /** Seat-hold window and how often stale holds are swept. */
    public record Hold(long windowMinutes, long sweepIntervalMs) {
    }

    /** How often unpaid-booking reminders are sent. */
    public record Reminder(long intervalMs) {
    }
}

/*
 * ============================================================================
 * FILE ROLE: Type-safe holder for all custom "booking.*" configuration.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Binds the booking.jwt / booking.hold / booking.reminder blocks from
 *     application.yml into immutable Java records (jwt secret + expiry, the
 *     seat-hold window + sweep interval, the reminder interval).
 *
 * TECHNICAL CONCEPTS
 *   - @ConfigurationProperties(prefix="booking") is Spring's "relaxed binding":
 *     booking.jwt.expiration-minutes (yaml) maps to Jwt.expirationMinutes (Java),
 *     and an env var BOOKING_JWT_SECRET maps to booking.jwt.secret. This is the
 *     preferred alternative to scattering @Value("${...}") strings around.
 *   - Using records makes the config immutable and validated at startup; it is
 *     activated by @EnableConfigurationProperties in BookingApplication.
 *   - Injected into JwtService and BookingService instead of raw strings, so
 *     configuration is centralised and typo-safe.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * Every app has SETTINGS you may want to change without touching the code - for
 * example, how long a login stays valid, or how many minutes seats are held.
 *
 * Those settings are written in a plain text file (application.yml). This file
 * is a tidy "settings box" in Java that mirrors those values, so the rest of the
 * code can just ask for them by name (like props.jwt().secret()) instead of
 * digging through text every time.
 *
 * The settings are grouped: login/token settings, seat-hold settings, and
 * reminder settings. The nice part: if you mistype a setting, you find out right
 * away, and everything about settings lives in ONE place.
 * ============================================================================
 */
