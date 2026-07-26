package com.example.booking.security;

import com.example.booking.config.BookingProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService service(long expiryMinutes) {
        var props = new BookingProperties(
                new BookingProperties.Jwt(
                        "test-secret-key-that-is-at-least-32-bytes-long!!", expiryMinutes),
                new BookingProperties.Hold(15, 60_000),
                new BookingProperties.Reminder(300_000));
        return new JwtService(props);
    }

    @Test
    void issue_thenParse_roundTripsSubjectAndRole() {
        JwtService jwt = service(120);
        String token = jwt.issue("user@x.com", "ATTENDEE");

        Claims claims = jwt.parse(token);
        assertThat(claims.getSubject()).isEqualTo("user@x.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ATTENDEE");
    }

    @Test
    void parse_expiredToken_throws() {
        JwtService jwt = service(-1); // already expired
        String token = jwt.issue("user@x.com", "ATTENDEE");

        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }
}
