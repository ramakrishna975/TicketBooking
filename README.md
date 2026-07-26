# Booking Platform

Event/ticket booking platform built with Spring Boot 3.4 (Java 21), package-by-feature.

## 🚀 Quick Start (first-time setup)

```bash
git clone https://github.com/ramakrishna975/TicketBooking.git
```

Then open the project's **`pom.xml`** in IntelliJ IDEA and run it. Full step-by-step
instructions are in **[`docs/SETUP-GUIDE.txt`](docs/SETUP-GUIDE.txt)**.

- **Runs at:** http://localhost:8080/swagger-ui.html (interactive API page)
- **Set once (both explained in the guide):** use **JDK 21** as the project SDK, and
  enable **Lombok** (install the plugin + tick *Build → Compiler → Annotation Processors →
  Enable annotation processing*) — otherwise the code shows red errors.
- **Seeded admin login:** `admin@x.com` / `password1`
- No database or Docker needed for the default run — an in-memory H2 database handles it.
  Running from IntelliJ (the green Run button or the Maven panel) uses IntelliJ's bundled
  Maven, so nothing extra is required. For terminal commands (`mvn ...`) install Maven:
  https://maven.apache.org/install.html

New to the code? Start with **[`docs/README.md`](docs/README.md)** → then
`docs/md/08-learning-guide.md`. Every source file also has plain-language comments at the bottom.

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
