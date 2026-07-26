package com.example.booking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end flow against a real PostgreSQL (Testcontainers), driving the actual REST
 * API over HTTP: register → login → create venue/event → publish → book → pay.
 * Runs under the {@code postgres} profile, so Flyway builds the schema and Hibernate
 * validates against it. Skipped by the default build (no Docker); run with
 * {@code mvn verify -DskipITs=false}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
@Testcontainers
class BookingFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void fullBookingFlow_holdsSeats_thenPays() {
        // Organizer registers and logs in.
        register("org@x.com", "password1", "Organizer", "ORGANIZER");
        String orgToken = login("org@x.com", "password1");

        // Organizer creates a venue.
        Long venueId = post("/api/venues", orgToken, Map.of(
                "name", "Main Hall", "address", "1 Road", "city", "Metropolis", "capacity", 100))
                .get("id").asLong();

        // Organizer creates an event with one ticket type.
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        JsonNode event = post("/api/events", orgToken, Map.of(
                "title", "Concert",
                "description", "Live show",
                "venueId", venueId,
                "startsAt", start.toString(),
                "endsAt", start.plus(3, ChronoUnit.HOURS).toString(),
                "ticketTypes", new Object[]{
                        Map.of("name", "General", "priceCents", 5000, "currency", "USD", "quantityTotal", 10)
                }));
        Long eventId = event.get("id").asLong();
        Long ticketTypeId = event.get("ticketTypes").get(0).get("id").asLong();

        // Publish it.
        JsonNode published = post("/api/events/" + eventId + "/publish", orgToken, null);
        assertThat(published.get("status").asText()).isEqualTo("PUBLISHED");

        // Organizer's own events must load (regression: /mine once 500'd on lazy fetch).
        ResponseEntity<JsonNode> mine = rest.exchange("/api/events/mine", HttpMethod.GET,
                new HttpEntity<>(bearer(orgToken)), JsonNode.class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody().isArray()).isTrue();
        assertThat(mine.getBody().size()).isEqualTo(1);

        // It now appears in the public listing.
        ResponseEntity<JsonNode> listing = rest.getForEntity("/api/events", JsonNode.class);
        assertThat(listing.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listing.getBody().isArray()).isTrue();

        // Attendee registers, logs in, and books 2 seats.
        register("att@x.com", "password1", "Attendee", "ATTENDEE");
        String attToken = login("att@x.com", "password1");

        JsonNode booking = post("/api/bookings", attToken, Map.of(
                "eventId", eventId,
                "items", new Object[]{Map.of("ticketTypeId", ticketTypeId, "quantity", 2)}));
        assertThat(booking.get("status").asText()).isEqualTo("PENDING");
        assertThat(booking.get("totalCents").asLong()).isEqualTo(10_000);
        Long bookingId = booking.get("id").asLong();

        // Pay it.
        JsonNode paid = post("/api/bookings/" + bookingId + "/pay", attToken, null);
        assertThat(paid.get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("paymentReference").asText()).startsWith("FAKE-");
    }

    // --- helpers ---

    private void register(String email, String password, String name, String role) {
        ResponseEntity<JsonNode> resp = rest.postForEntity("/api/auth/register",
                new HttpEntity<>(Map.of("email", email, "password", password,
                        "displayName", name, "role", role), jsonHeaders()),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String login(String email, String password) {
        ResponseEntity<JsonNode> resp = rest.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()),
                JsonNode.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().get("accessToken").asText();
    }

    private JsonNode post(String path, String token, Object body) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<JsonNode> resp = rest.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(resp.getStatusCode().is2xxSuccessful())
                .as("POST %s -> %s", path, resp.getStatusCode())
                .isTrue();
        return resp.getBody();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
