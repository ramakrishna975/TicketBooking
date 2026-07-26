# Booking Platform

Event/ticket booking platform built with Spring Boot 3.4 (Java 21), package-by-feature.

## Run

```bash
# Dev (in-memory H2, schema auto-created, zero infra):
mvn spring-boot:run
# -> boots on the `dev` profile; probe: GET http://localhost:8080/api/ping

# Postgres (Flyway migrations, ddl-auto=validate):
docker compose up -d
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

## Build & test

```bash
mvn install                 # compile + unit tests + package jar  (integration tests skipped)
mvn verify -DskipITs=false  # also runs Testcontainers integration tests (requires Docker)
```

Integration tests (`*IT`) are bound to the **Failsafe** plugin and excluded from the
default build because they need a Docker daemon (Testcontainers Postgres). The build
gate is therefore **compile + unit tests + Spring context load**. Unit tests (`*Test`)
run under Surefire.

## Features

| Concept | Where |
|---|---|
| JWT auth, roles (`ATTENDEE`/`ORGANIZER`/`ADMIN`) | `security/`, `user/` |
| Events / venues / ticket types | `event/`, `venue/` |
| Bookings, seat holds, `@Version` optimistic lock, stubbed payment | `booking/`, `payment/` |
| RFC 7807 `ProblemDetail` errors | `common/error/`, `security/SecurityErrorHandlers` |
| Caffeine cache on the published-events read path | `config/CacheConfig`, `event/EventService` |
| Scheduling (hold expiry, reminders) | `booking/BookingScheduler` |
| `@TransactionalEventListener(AFTER_COMMIT)` ticket issuance | `booking/BookingPaidListener` |
| Flyway migrations (postgres) | `resources/db/migration` |
| Actuator: `health,info,metrics` | `application.yml` |

## Key API

```
POST /api/auth/register            {email,password,displayName,role}
POST /api/auth/login               {email,password}            -> { accessToken }
GET  /api/events                   (public, cached)
POST /api/events                   (ORGANIZER)  create + ticket types
POST /api/events/{id}/publish      (ORGANIZER)
POST /api/bookings                 (ATTENDEE)   hold seats  -> PENDING
POST /api/bookings/{id}/pay        (ATTENDEE)   -> PAID
POST /api/admin/events/{id}/cancel (ADMIN)
```
