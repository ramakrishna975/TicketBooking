# 2. Architecture

A top-to-bottom explanation of how the system is built and why it hangs together.

---

## 2.1 The shape at a glance

```
                       HTTP (JSON)
                           │
                    ┌──────▼───────┐
                    │  Controllers │  @RestController — thin, no business logic
                    └──────┬───────┘
                           │ DTOs (records) in, DTOs out  (entities never on the wire)
                    ┌──────▼───────┐
                    │   Services   │  @Service — business rules, @Transactional boundaries
                    └──────┬───────┘
             ┌─────────────┼─────────────┬───────────────┐
        ┌────▼────┐  ┌─────▼─────┐  ┌────▼─────┐   ┌──────▼──────┐
        │ Repos   │  │ Security  │  │ Payment  │   │ Events /    │
        │ (JPA)   │  │ (JWT)     │  │ (stub)   │   │ Scheduling  │
        └────┬────┘  └───────────┘  └──────────┘   └─────────────┘
             │
        ┌────▼────────────────────┐
        │ H2 (dev) / Postgres      │  Flyway owns the Postgres schema
        └──────────────────────────┘
```

The layering is conventional (controller → service → repository), but the **packaging is
by feature**, so those layers live *inside* each feature folder, not in global `controllers/`
/ `services/` folders.

---

## 2.2 Package-by-feature layout

```
com.example.booking
├── BookingApplication            ← main class; enables caching, scheduling, config props
│
├── user            ← identity: entity, roles, register/login, /me
│   ├── User, Role, UserRepository, UserService
│   ├── AuthController (register/login), UserController (/me)
│   └── dto: RegisterRequest, LoginRequest, TokenResponse, UserResponse
│
├── security        ← cross-cutting auth (used by every feature)
│   ├── SecurityConfig            ← filter chain, URL rules, method security, beans
│   ├── JwtService                ← issue/parse HS256 tokens
│   ├── JwtAuthenticationFilter   ← reads Bearer token → SecurityContext
│   ├── AppUserDetailsService     ← DB-backed UserDetails for login
│   └── SecurityErrorHandlers     ← 401/403 as ProblemDetail JSON
│
├── venue           ← where events happen
│   └── Venue, VenueRepository, VenueService, VenueController, dto/
│
├── event           ← events + ticket types (the sellable inventory)
│   ├── Event, EventStatus, TicketType
│   ├── EventRepository, TicketTypeRepository (optimistic-lock read)
│   ├── EventService (cached listing), EventController
│   └── dto: CreateEventRequest, EventResponse, TicketTypeRequest, TicketTypeResponse
│
├── booking         ← the core: holds, payment, lifecycle
│   ├── Booking, BookingItem, BookingStatus
│   ├── BookingRepository, BookingService, BookingController
│   ├── BookingPaidEvent + BookingPaidListener  ← after-commit ticket issuance
│   ├── BookingScheduler                         ← hold expiry + reminders
│   └── dto: CreateBookingRequest, BookingResponse
│
├── payment         ← stubbed gateway behind an interface
│   └── PaymentGateway (interface), FakePaymentGateway (impl)
│
├── notification    ← stubbed notifications (log-based)
│   └── NotificationService
│
├── admin           ← moderation surface (ADMIN only)
│   └── AdminController (enable/disable users, force-cancel events)
│
├── common.error    ← RFC 7807 handling + domain exceptions
│   ├── GlobalExceptionHandler (@RestControllerAdvice)
│   ├── NotFoundException (→404), ConflictException (→409)
│
├── config          ← app-wide config
│   ├── BookingProperties (@ConfigurationProperties for booking.*)
│   └── CacheConfig (Caffeine cache manager)
│
└── ping            ← trivial liveness probe (GET /api/ping)
```

**Why feature-first?** A change to "how bookings work" touches one folder. New engineers
find everything about a concept in one place. It also keeps visibility natural — e.g.
`TicketTypeRepository`'s locking sits right next to the entity it protects.

---

## 2.3 The domain model

```
        User (ATTENDEE│ORGANIZER│ADMIN)
          │ 1                        │ 1
          │ organizes                │ books
          ▼ *                        ▼ *
        Event  ── * TicketType     Booking ── * BookingItem
          │ *        (price,          │            (qty, unit price)
          │ at       capacity,        │ for
          ▼ 1        available)       ▼ *
        Venue                       TicketType   ← BookingItem points back at the tier it bought
                                   (via Event)
```

Key facts:
- **`Event → Venue`** (many-to-one): an event happens at one venue.
- **`Event → User` (organizer)**: who owns/created it.
- **`Event → TicketType`** (one-to-many, cascade): tiers like *General* / *VIP*. Each tier
  carries `quantityTotal` and `quantityAvailable` — **this is the seat/capacity count**.
- **`Booking → Event`, `Booking → User` (attendee)**: who booked what.
- **`Booking → BookingItem`** (one-to-many, cascade): each line is N tickets of one tier at
  the price captured *at hold time* (so later price changes don't rewrite history).
- **Money** is `long` cents everywhere; currency is a 3-letter code.

### Lifecycles (state machines)

```
Event:    DRAFT ──publish──▶ PUBLISHED ──cancel──▶ CANCELLED
                                 ▲                     ▲
                          only PUBLISHED         admin can force
                          accepts bookings

Booking:  PENDING ──pay──▶ PAID
             │  (holds seats)     └─ issues tickets (after commit)
             ├──cancel──▶ CANCELLED   (releases seats)
             └──expire──▶ EXPIRED     (releases seats, by scheduler)
```

---

## 2.4 Security model

Two layers of defence, on purpose:

1. **URL rules** (`SecurityConfig`) — a coarse first pass:
   - Public: `GET /api/ping`, `/actuator/health`, `/actuator/info`, `/api/auth/**`,
     `GET /api/events/**`, `GET /api/venues/**`.
   - `ADMIN` only: `/actuator/**` (beyond health/info), `/api/admin/**`.
   - Everything else: authenticated.

2. **Method security** (`@PreAuthorize`) — fine-grained, next to the logic:
   - `@PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")` on event/venue writes.
   - `@PreAuthorize("hasAnyRole('ATTENDEE','ADMIN')")` on the booking controller.
   - `@PreAuthorize("hasRole('ADMIN')")` on the admin controller.
   - **Ownership** (which is data-dependent, not role-dependent) is enforced *inside*
     services: an attendee can only pay/cancel/read *their own* booking; an organizer can
     only publish/cancel *their own* event. Violations raise `AccessDeniedException` (403)
     or `ConflictException` (409).

**Token flow:** `POST /api/auth/login` verifies credentials via `AuthenticationManager`
(DAO provider + BCrypt) and returns a signed **JWT** whose subject is the email and which
carries a `role` claim. On every later request, `JwtAuthenticationFilter` validates the
`Bearer` token and populates the `SecurityContext`. The chain is **stateless** — no session.

**Auth failures as ProblemDetail:** 401 (no/invalid token) and 403 (wrong role) are emitted
as RFC 7807 JSON by `SecurityErrorHandlers`, matching the controller-advice format so clients
see one consistent error shape.

---

## 2.5 Transactions, events, and the seat-hold guarantee

The trickiest correctness property is **not overselling seats** under concurrency. It's
handled with **optimistic locking**, not table locks:

- `TicketType` has `@Version`. When a booking holds seats, `TicketTypeRepository.findById`
  loads the tier under `LockModeType.OPTIMISTIC_FORCE_INCREMENT`, which bumps the version.
- If two attendees race for the last seats, both read, both decrement, but only one commit
  wins; the other fails with an optimistic-lock exception and no seats are lost or
  double-sold.
- `Booking` *also* has `@Version`, so the "attendee pays while the scheduler expires the
  same booking" race can't corrupt state either.

**After-commit events:** paying a booking publishes a `BookingPaidEvent`. Its listener runs
with `@TransactionalEventListener(AFTER_COMMIT)` in a **new** transaction, so ticket issuance
/ notification happen **only once the payment is durably committed** — a post-commit hiccup
can never roll back a captured payment. This is the in-process
`ApplicationEventPublisher` + `@TransactionalEventListener` pattern the brief asked for.

---

## 2.6 Profiles and schema

| Concern | `dev` (default) | `postgres` |
|---|---|---|
| Database | H2 in-memory (`PostgreSQL` compat mode) | real PostgreSQL |
| Schema source | Hibernate `create-drop` | **Flyway** (`V1`, `V2`) |
| `ddl-auto` | `create-drop` (the only non-`validate` place) | **`validate`** |
| Flyway | disabled | enabled |
| Infra needed | none | Postgres (docker-compose provided) |

Keeping `ddl-auto=validate` on Postgres means Hibernate will **refuse to start** if the
entities and the Flyway-built schema drift apart — a cheap, strong guardrail.

---

## 2.7 Cross-cutting concerns wired at startup

`BookingApplication` turns on three things app-wide:
- `@EnableCaching` — activates the Caffeine `@Cacheable`/`@CacheEvict` on the listing path.
- `@EnableScheduling` — activates `BookingScheduler`'s periodic jobs.
- `@EnableConfigurationProperties(BookingProperties.class)` — binds the `booking.*` config
  tree (JWT secret/expiry, hold window & sweep interval, reminder interval) into a typed record.

Actuator exposes exactly `health,info,metrics`, as required — no more.
