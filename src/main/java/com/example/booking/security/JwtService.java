package com.example.booking.security;

import com.example.booking.config.BookingProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and validates HS256 JWTs. The subject is the user email; role is a claim. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(BookingProperties props) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = props.jwt().expirationMinutes();
    }

    public String issue(String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(expirationMinutes));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return Duration.ofMinutes(expirationMinutes).toSeconds();
    }

    /** Parses and validates the token, returning its claims. Throws if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

/*
 * ============================================================================
 * FILE ROLE: Issues and validates JWT access tokens.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - issue(email, role): builds a signed JWT with the email as subject and the
 *     role as a claim, an issued-at and an expiry.
 *   - parse(token): verifies the signature/expiry and returns the claims (throws
 *     if tampered or expired).
 *
 * TECHNICAL CONCEPTS
 *   - A JWT (JSON Web Token) is a compact, URL-safe token of three parts
 *     (header.payload.signature). Here it is signed with HS256 (HMAC + a shared
 *     secret), so the server can verify it wasn't altered WITHOUT storing any
 *     session - this is what makes auth STATELESS.
 *   - The secret and expiry come from BookingProperties (booking.jwt.*). The key
 *     must be >= 256 bits for HS256.
 *   - "Subject" and "claims" are standard JWT fields; we put identity in subject
 *     and authorisation (role) in a custom claim.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * After you log in, the app gives you a special PASS (called a "token") that
 * proves who you are, so you do not have to type your password again on every
 * click.
 *
 * This file is the machine that MAKES that pass and CHECKS it:
 *   - MAKE: it stamps your email and your role inside the pass and "signs" it in
 *     a way that cannot be faked or edited.
 *   - CHECK: when a pass comes back, it verifies the signature is real and the
 *     pass has not expired.
 *
 * Picture a signed, tamper-proof wristband you get at a concert entrance: staff
 * can glance at it and instantly trust it, without calling the ticket office.
 * ============================================================================
 */
