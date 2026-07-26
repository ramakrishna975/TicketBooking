# 3. File Guide

Every file in the project, grouped by feature, with what it does and why it's there. The
source itself carries fuller Javadoc/inline comments — this is the index that tells you
which file to open.

> Legend: **C** = controller, **S** = service, **R** = repository, **E** = JPA entity,
> **D** = DTO (record), **cfg** = configuration, **sec** = security.

---

## Root & build

| File | What it does |
|---|---|
| `pom.xml` | Maven build. Declares the stack, pins **Lombok 1.18.38** (JDK-25 fix), binds `*Test`→Surefire and `*IT`→Failsafe with `skipITs=true` default, configures the Spring Boot fat-jar repackage. |
| `docker-compose.yml` | **Local** Postgres for the `postgres` profile. Dev infra only — not a deployment artifact. |
| `README.md` | Quick-start and feature map. |
| `.gitignore` | Ignores `target/`, IDE files, `.DS_Store`. |

---

## Bootstrap

| File | Kind | What it does |
|---|---|---|
| `BookingApplication.java` | main | App entrypoint. Enables caching, scheduling, and `BookingProperties` binding. |
| `ping/PingController.java` | C | `GET /api/ping` — trivial liveness probe used to confirm the app boots and routes. |

---

## `config` — app-wide configuration

| File | Kind | What it does |
|---|---|---|
| `config/BookingProperties.java` | cfg | Typed binding for `booking.*`: JWT secret/expiry, hold window + sweep interval, reminder interval. A record with nested records. |
| `config/CacheConfig.java` | cfg | The Caffeine `CacheManager` with exactly one cache, `publishedEvents` (max 500, expire-after-write 5 min). Its Javadoc justifies *why only here*. |

---

## `common.error` — errors as RFC 7807

| File | Kind | What it does |
|---|---|---|
| `common/error/GlobalExceptionHandler.java` | — | `@RestControllerAdvice` translating exceptions → `ProblemDetail`: `NotFoundException`→404, `ConflictException`→409, validation→400 (with a field-error map), auth→401/403, and a last-resort 500. **No custom error POJO** — the spec's requirement. |
| `common/error/NotFoundException.java` | — | Domain exception for "resource doesn't exist" (→404). `of(what, id)` factory for tidy messages. |
| `common/error/ConflictException.java` | — | Domain exception for state conflicts — duplicate email, sold-out tier, illegal transition (→409). |

---

## `security` — JWT auth (cross-cutting)

| File | Kind | What it does |
|---|---|---|
| `security/SecurityConfig.java` | sec | The stateless filter chain: CSRF off, URL authorization rules, `@EnableMethodSecurity`, registers the JWT filter, wires the 401/403 handlers, and defines `PasswordEncoder` (BCrypt) + `AuthenticationManager` (DAO provider). |
| `security/JwtService.java` | sec | Issues and validates **HS256** JWTs (subject = email, `role` claim). Reads secret/expiry from `BookingProperties`. |
| `security/JwtAuthenticationFilter.java` | sec | `OncePerRequestFilter` that reads the `Bearer` token, validates it, and populates the `SecurityContext`. Invalid tokens → request proceeds unauthenticated (rejected downstream). |
| `security/AppUserDetailsService.java` | sec | Bridges our `User` aggregate to Spring Security's `UserDetails` (used by the login `AuthenticationManager`), mapping `Role` → `ROLE_*` authority. |
| `security/SecurityErrorHandlers.java` | sec | Renders authentication (401) and access-denied (403) failures *from the filter chain* as ProblemDetail JSON, matching the advice format. |

---

## `user` — identity

| File | Kind | What it does |
|---|---|---|
| `user/User.java` | E | User aggregate: email (unique), BCrypt `passwordHash`, `displayName`, `Role`, `enabled`, `createdAt`, `@Version`. |
| `user/Role.java` | — | `ATTENDEE` / `ORGANIZER` / `ADMIN`. |
| `user/UserRepository.java` | R | `findByEmail`, `existsByEmail`. |
| `user/UserService.java` | S | Register (rejects self-registering as ADMIN, dup-email → conflict, BCrypt-encodes), lookups, and admin enable/disable. |
| `user/AuthController.java` | C | `POST /api/auth/register` (201) and `POST /api/auth/login` (→ JWT). |
| `user/UserController.java` | C | `GET /api/users/me` — the caller's own profile. |
| `user/dto/RegisterRequest.java` | D | Validated: email, password (≥8), displayName, role. |
| `user/dto/LoginRequest.java` | D | Validated: email, password. |
| `user/dto/TokenResponse.java` | D | `{accessToken, tokenType=Bearer, expiresInSeconds}`. |
| `user/dto/UserResponse.java` | D | Public projection — **never** exposes the hash. |

---

## `venue` — where events happen

| File | Kind | What it does |
|---|---|---|
| `venue/Venue.java` | E | Name, address, city, `capacity`, `@Version`. |
| `venue/VenueRepository.java` | R | Standard CRUD. |
| `venue/VenueService.java` | S | List, get-by-id (→404 if missing), create. |
| `venue/VenueController.java` | C | Public `GET`; `POST` restricted to ORGANIZER/ADMIN. |
| `venue/dto/VenueRequest.java` | D | Validated create payload. |
| `venue/dto/VenueResponse.java` | D | Output projection. |

---

## `event` — sellable inventory

| File | Kind | What it does |
|---|---|---|
| `event/Event.java` | E | Title, description, `venue`, `organizer`, start/end, `EventStatus`, cascade `ticketTypes`, `createdAt`, `@Version`. `addTicketType` keeps both sides in sync. |
| `event/EventStatus.java` | — | `DRAFT` / `PUBLISHED` / `CANCELLED`. |
| `event/TicketType.java` | E | A priced tier holding the **seat count**: `priceCents`, `currency`, `quantityTotal`, `quantityAvailable`, `@Version` (the field bookings race on). |
| `event/EventRepository.java` | R | `findByStatus` and `findWithDetailsById` use `@EntityGraph` to fetch venue/ticketTypes and avoid N+1. |
| `event/TicketTypeRepository.java` | R | `findById` overridden with `@Lock(OPTIMISTIC_FORCE_INCREMENT)` — the seat-hold guard. |
| `event/EventService.java` | S | Create (validates dates + capacity), publish/cancel (ownership-checked), admin force-cancel, and the **cached** `listPublishedResponses()` with `@CacheEvict` on every mutator. |
| `event/EventController.java` | C | Public listing/detail; ORGANIZER/ADMIN create/publish/cancel/mine. |
| `event/dto/CreateEventRequest.java` | D | Nested, validated: event fields + non-empty ticket-type list. |
| `event/dto/TicketTypeRequest.java` | D | Validated tier: name, price, ISO currency, quantity. |
| `event/dto/EventResponse.java` | D | Output incl. venue name + ticket types. |
| `event/dto/TicketTypeResponse.java` | D | Output tier incl. live availability. |

---

## `booking` — the core flow

| File | Kind | What it does |
|---|---|---|
| `booking/Booking.java` | E | Aggregate root: `event`, `attendee`, `BookingStatus`, cascade `items`, `totalCents`, `currency`, `paymentReference`, `createdAt`, `expiresAt`, `@Version`. |
| `booking/BookingItem.java` | E | A line: `ticketType`, `quantity`, `unitPriceCents` (captured at hold time). |
| `booking/BookingStatus.java` | — | `PENDING` / `PAID` / `EXPIRED` / `CANCELLED`, documented lifecycle. |
| `booking/BookingRepository.java` | R | Owner lookups (with graphs to avoid lazy issues), the stale-hold query, and status queries. |
| `booking/BookingService.java` | S | **The heart.** `createBooking` (holds seats under optimistic lock, computes total, sets expiry), `pay` (charges the stub, marks PAID, publishes the after-commit event), `cancel`/`expireStaleHolds` (release seats), `sendUpcomingReminders`. |
| `booking/BookingController.java` | C | Attendee-facing create/list/get/pay/cancel; class-level `@PreAuthorize` for ATTENDEE/ADMIN. |
| `booking/BookingPaidEvent.java` | — | The domain event `record(bookingId)` published after payment commits. |
| `booking/BookingPaidListener.java` | — | `@TransactionalEventListener(AFTER_COMMIT)` in a new tx — issues tickets + notifies, only post-commit. |
| `booking/BookingScheduler.java` | — | Two `@Scheduled` jobs: expire stale holds; send reminders. Intervals from config. |
| `booking/dto/CreateBookingRequest.java` | D | `{eventId, items:[{ticketTypeId, quantity≥1}]}`, validated. |
| `booking/dto/BookingResponse.java` | D | Full booking projection incl. line items. |

---

## `payment` — stubbed gateway

| File | Kind | What it does |
|---|---|---|
| `payment/PaymentGateway.java` | — | The abstraction: `charge(PaymentRequest) → PaymentResult`, with `ok`/`declined` factories. Real gateways are out of scope by design. |
| `payment/FakePaymentGateway.java` | — | Deterministic fake: approves positive charges (mints a `FAKE-…` reference), declines non-positive so the failure path stays tested. |

---

## `notification` — stubbed notifications

| File | Kind | What it does |
|---|---|---|
| `notification/NotificationService.java` | S | Logs "issued tickets" / "reminder" — enough to demonstrate ticket issuance and reminders end-to-end without email/SMS infra. |

---

## `admin` — moderation

| File | Kind | What it does |
|---|---|---|
| `admin/AdminController.java` | C | `hasRole('ADMIN')`: list users, enable/disable a user, force-cancel any event. |

---

## Resources

| File | What it does |
|---|---|
| `resources/application.yml` | Base config: app name, default profile `dev`, JPA `open-in-view:false`, UTC, actuator `health,info,metrics`, and the `booking.*` knobs. |
| `resources/application-dev.yml` | H2 in-memory, Hibernate `create-drop`, Flyway off, H2 console on. |
| `resources/application-postgres.yml` | Postgres datasource (env-overridable), `ddl-auto=validate`, Flyway on. |
| `resources/db/migration/V1__core_schema.sql` | Flyway: `app_user`, `venue`, `event`, `ticket_type` + indexes. |
| `resources/db/migration/V2__booking_schema.sql` | Flyway: `booking`, `booking_item` + indexes (incl. the composite index the hold-sweep query uses). |

---

## Tests

| File | Runner | What it covers |
|---|---|---|
| `test/.../BookingApplicationTests.java` | Surefire | Spring context loads on H2 (the boot smoke test). |
| `test/.../booking/BookingServiceTest.java` | Surefire | 6 tests: seat-hold + total, sold-out, unpublished event, pay-success + event published, expired-hold no-charge, cancel-releases-seats. |
| `test/.../user/UserServiceTest.java` | Surefire | 4 tests: register encodes + persists, dup-email, admin-self-register blocked, missing user. |
| `test/.../event/EventServiceTest.java` | Surefire | 3 tests: create builds draft + tiers, end-before-start, capacity exceeded. |
| `test/.../security/JwtServiceTest.java` | Surefire | 2 tests: issue↔parse round-trip, expired token rejected. |
| `test/.../BookingFlowIT.java` | **Failsafe** | Full HTTP flow against **Testcontainers Postgres** (Flyway + validate). Skipped by default; `mvn verify -DskipITs=false`. |
