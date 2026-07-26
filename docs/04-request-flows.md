# 4. Request Flows

Concrete, step-by-step walkthroughs so you can trace the code with intent. Every path shown
is exercised by the tests.

---

## 4.1 The happy path: from zero to a paid ticket

### Step 1 — Organizer registers
```
POST /api/auth/register
{ "email":"org@x.com", "password":"password1", "displayName":"Org", "role":"ORGANIZER" }
→ 201 Created  (UserResponse)
```
`AuthController.register` → `UserService.register`: rejects `role=ADMIN`, rejects duplicate
email (`ConflictException`→409), BCrypt-hashes the password, saves `enabled=true`.

### Step 2 — Organizer logs in
```
POST /api/auth/login  { "email":"org@x.com", "password":"password1" }
→ 200 OK  { "accessToken":"eyJ…", "tokenType":"Bearer", "expiresInSeconds":7200 }
```
`AuthController.login` → `AuthenticationManager.authenticate` (DAO provider verifies BCrypt
via `AppUserDetailsService`). On success `JwtService.issue` mints an HS256 token with
`sub=email`, `role=ORGANIZER`. Bad credentials → `BadCredentialsException` → **401** ProblemDetail.

### Step 3 — Organizer creates a venue
```
POST /api/venues   (Authorization: Bearer <orgToken>)
{ "name":"Main Hall", "address":"1 Rd", "city":"Metropolis", "capacity":100 }
→ 201 Created  (VenueResponse with id)
```
`@PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")` gate. An ATTENDEE token here → **403**.

### Step 4 — Organizer creates an event with a ticket tier
```
POST /api/events   (Bearer <orgToken>)
{ "title":"Concert", "venueId":1, "startsAt":"…+7d", "endsAt":"…+7d3h",
  "ticketTypes":[ {"name":"General","priceCents":5000,"currency":"USD","quantityTotal":10} ] }
→ 201 Created  (EventResponse, status DRAFT, ticketTypes[0].id present)
```
`EventService.create`: validates `endsAt > startsAt` (else 409), loads venue+organizer,
checks total requested quantity ≤ venue capacity (else 409), builds the event **DRAFT** with
each tier's `quantityAvailable = quantityTotal`.

### Step 5 — Organizer publishes
```
POST /api/events/{id}/publish   (Bearer <orgToken>)
→ 200 OK  (EventResponse, status PUBLISHED)
```
`EventService.publish`: ownership-checked (not your event → 409), can't publish a CANCELLED
event, flips to **PUBLISHED**, and **evicts the `publishedEvents` cache**.

### Step 6 — Anyone browses (cached)
```
GET /api/events   (no auth)
→ 200 OK  [ EventResponse, … ]
```
First call runs the query and maps to DTOs; subsequent calls are served from Caffeine until
a create/publish/cancel evicts, or 5 minutes pass.

### Step 7 — Attendee registers, logs in, books (holds seats)
```
POST /api/bookings   (Bearer <attToken>)
{ "eventId":1, "items":[ {"ticketTypeId":100, "quantity":2} ] }
→ 201 Created  (BookingResponse, status PENDING, totalCents 10000, expiresAt set)
```
`BookingService.createBooking`:
1. loads attendee + event; event must be **PUBLISHED** (else 409).
2. for each line, loads the tier **under `OPTIMISTIC_FORCE_INCREMENT`**; verifies it belongs
   to the event; enforces single currency; checks availability (else "Not enough seats" 409).
3. **decrements `quantityAvailable` — the seats are now held.**
4. captures `unitPriceCents`, sums the total, sets `expiresAt = now + holdWindow` (15 min).

### Step 8 — Attendee pays
```
POST /api/bookings/{id}/pay   (Bearer <attToken>)
→ 200 OK  (BookingResponse, status PAID, paymentReference "FAKE-…")
```
`BookingService.pay`: ownership-checked; must be **PENDING**; must not be expired (else 409);
calls `PaymentGateway.charge` (declined → 409); sets **PAID**, stores the reference, clears
`expiresAt`; **publishes `BookingPaidEvent`**.

### Step 9 — Tickets issued (after commit)
`BookingPaidListener.onBookingPaid` fires **AFTER_COMMIT** in a new transaction, reloads the
booking, and calls `NotificationService.issueTickets`. Logs:
```
[TICKETS] Issued tickets for booking 1 to att@x.com
```
This is the whole `BookingFlowIT` end-to-end against real Postgres.

---

## 4.2 The unhappy paths (and their status codes)

| Situation | Where enforced | Result |
|---|---|---|
| Register as ADMIN | `UserService.register` | 409 Conflict |
| Duplicate email | `UserService.register` | 409 Conflict |
| Bad login | `AuthenticationManager` | 401 (ProblemDetail) |
| No/expired token on a protected route | `SecurityErrorHandlers` | 401 (ProblemDetail) |
| Wrong role (e.g. attendee creates event) | `@PreAuthorize` | 403 (ProblemDetail) |
| Book an unpublished event | `BookingService.createBooking` | 409 Conflict |
| Not enough seats | `BookingService.createBooking` | 409 Conflict |
| Pay an expired hold | `BookingService.pay` | 409 (payment **not** attempted) |
| Pay someone else's booking | `BookingService.requireOwnedBooking` | 403 (AccessDenied) |
| Unknown id | `NotFoundException` | 404 |
| Invalid body (blank email, qty 0, …) | Bean Validation | 400 with field-error map |

---

## 4.3 Concurrency: two people, the last two seats

```
Tier "General": quantityAvailable = 2

  Attendee A ──▶ read tier (v=5) ─▶ decrement to 0 ─▶ commit  ✅ (version → 6)
  Attendee B ──▶ read tier (v=5) ─▶ decrement to 0 ─▶ commit  ✖ OptimisticLockException
```
Because `findById` uses `OPTIMISTIC_FORCE_INCREMENT`, both transactions try to move the
version from 5; the DB lets only one succeed. B's transaction rolls back — **no seat is
double-sold and none is lost**. B simply retries and sees "Not enough seats".

`Booking` carries its own `@Version` too, so the "pay vs. scheduler-expire the same booking"
race is resolved the same way.

---

## 4.4 The background jobs

`BookingScheduler` (enabled by `@EnableScheduling`):

- **Hold expiry** — every `booking.hold.sweep-interval-ms` (default 60s):
  `expireStaleHolds()` finds `PENDING` bookings whose `expiresAt` has passed, **releases
  their held seats** (increments `quantityAvailable`), and marks them **EXPIRED**. This is
  what makes "expires if not paid within a window" real.

- **Reminders** — every `booking.reminder.interval-ms` (default 5 min):
  `sendUpcomingReminders(24h)` finds `PAID` bookings whose event starts within 24 hours and
  sends a (stubbed) reminder.

Both are ordinary Spring beans, so in the ITs you could drive them directly; in production
they run on the scheduler thread pool.
