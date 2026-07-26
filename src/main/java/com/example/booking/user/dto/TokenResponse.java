package com.example.booking.user.dto;

/** JWT bearer token returned on successful register/login. */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse bearer(String token, long expiresInSeconds) {
        return new TokenResponse(token, "Bearer", expiresInSeconds);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Response DTO returning the JWT after login.
 * WHAT IT DOES: { accessToken, tokenType="Bearer", expiresInSeconds }.
 * TECHNICAL CONCEPTS: The client stores accessToken and sends it as
 *   "Authorization: Bearer <token>" on subsequent requests (OAuth2 bearer style).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The shape of what you get back after a successful login:
 *   - accessToken     = your pass,
 *   - tokenType       = the word "Bearer" (how to send the pass back),
 *   - expiresInSeconds= how long until the pass stops working.
 *
 * Your app stores the pass and sends it on future requests to stay logged in.
 * ============================================================================
 */
