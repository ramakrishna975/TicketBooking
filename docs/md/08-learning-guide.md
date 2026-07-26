# 8. The Learning Guide — Every Concept, Explained Simply, With Where to Look

This is the one doc to read if you want to *understand* the project. Each section explains a
concept in plain language, says why it's used here, and ends with **📁 Where to look** — the
exact file(s) to open so you can see it in real code.

Read it top-to-bottom once; after that, use it as a lookup table.

---

## The big picture first (a simple analogy)

Think of a concert-ticket website.

- **Organizers** are the promoters. They pick a **venue** (a hall), create an **event** (the
  concert), and set up **ticket types** (General ₹500, VIP ₹1500) with how many exist.
- **Attendees** are fans. They **book** tickets. The moment they book, those seats are **held**
  for them for a short time — like an "item in your cart reserved for 15 minutes." If they
  don't **pay** in time, the hold **expires** and the seats go back on sale.
- **Payment** is faked (no real bank) — but hidden behind a clean "swap me later" boundary.
- **Admins** are moderators — they can disable a user or cancel an event.

The whole codebase is just this story, told in Java. Everything below is a technique used to
tell that story correctly and safely.

---

## 1. How the app starts up

**Concept:** A Spring Boot app has one "main" class. When it runs, Spring scans your code,
creates objects for you (called *beans*), wires them together, and starts a web server.

**In this project:** the main class also switches on three whole-app features — caching,
scheduled jobs, and typed configuration.

**📁 Where to look:** `../../src/main/java/com/example/booking/BookingApplication.java`
(the `@SpringBootApplication`, `@EnableCaching`, `@EnableScheduling`,
`@EnableConfigurationProperties` lines).

---

## 2. The three layers (Controller → Service → Repository)

**Concept:** Most requests flow through three kinds of objects:

- **Controller** — the "front desk." Receives the HTTP request, hands it inward, returns a
  response. No business rules here.
- **Service** — the "brain." Contains the actual rules ("you can't book an unpublished event").
- **Repository** — the "filing clerk." Talks to the database (save, find, delete).

Data flows in as a request object, and out as a response object; the database entities stay
inside.

**📁 Where to look (one clean example):**
- Controller: `venue/VenueController.java`
- Service: `venue/VenueService.java`
- Repository: `venue/VenueRepository.java`

---

## 3. "Package by feature" (how the folders are organized)

**Concept:** Two ways to organize code:
- *By layer* — all controllers in one folder, all services in another. (Not used here.)
- *By feature* — everything about "bookings" in a `booking/` folder, everything about "events"
  in an `event/` folder. **This is what we use.**

**Why:** when you change how bookings work, you touch one folder. Everything about one idea
lives together.

**📁 Where to look:** the top-level folders under
`../../src/main/java/com/example/booking` — `user/`, `event/`, `venue/`, `booking/`, `payment/`,
`admin/`, `security/`, `common/`, `config/`, `notification/`.

---

## 4. Entities & JPA (turning Java objects into database rows)

**Concept:** An **entity** is a Java class that maps to a database **table**. Each object is a
**row**; each field is a **column**. This mapping is called **JPA** (and Hibernate is the
engine that does it). You annotate the class with `@Entity`, mark the primary key with `@Id`,
and describe relationships between tables.

**Relationships used here:**
- `@ManyToOne` — many events belong to one venue.
- `@OneToMany` — one event has many ticket types.

**In this project:** entities also use **Lombok** (`@Getter/@Setter/@Builder`) so we don't
hand-write boilerplate getters/setters.

**📁 Where to look:**
- Simple entity: `venue/Venue.java`
- Relationships + a list of children: `event/Event.java` (see `@ManyToOne venue`,
  `@ManyToOne organizer`, `@OneToMany ticketTypes`)
- The aggregate with lifecycle: `booking/Booking.java` and `booking/BookingItem.java`

---

## 5. Repositories & Spring Data (free database methods)

**Concept:** You write an **interface** (no code body), extend `JpaRepository`, and Spring
*generates* the implementation. You get `save`, `findById`, `findAll` for free, and you can
add methods just by naming them: `findByEmail`, `existsByEmail`, `findByStatus` — Spring reads
the name and writes the query.

**Extra trick used here:** `@EntityGraph` tells the database "also fetch these related rows in
the same trip" to avoid the slow "N+1 queries" problem.

**📁 Where to look:**
- Basic: `user/UserRepository.java` (`findByEmail`, `existsByEmail`)
- With eager fetching: `event/EventRepository.java` (`@EntityGraph`)

---

## 6. DTOs and `record`s (what actually goes over the internet)

**Concept:** A **DTO** (Data Transfer Object) is the exact shape of JSON that comes in or goes
out. We **never** send entities directly — that would leak internals (like a password hash) and
tie the API to the database. Instead we use Java **`record`s**, which are short, immutable
data holders.

**Example rule enforced by this:** `UserResponse` has no password field, so a hash can never
accidentally be returned.

**📁 Where to look:**
- Incoming: `user/dto/RegisterRequest.java`, `booking/dto/CreateBookingRequest.java`
- Outgoing: `user/dto/UserResponse.java`, `event/dto/EventResponse.java` (see the `from(...)`
  method that copies an entity into a safe DTO)

---

## 7. Bean Validation (rejecting bad input automatically)

**Concept:** Instead of writing `if (email == null) ...` everywhere, you put annotations on
the request fields — `@NotBlank`, `@Email`, `@Min(1)`, `@Future` — and Spring checks them
before your code runs. Invalid input becomes an automatic **400 Bad Request** with a list of
what's wrong.

**📁 Where to look:**
- The rules: `user/dto/RegisterRequest.java`, `event/dto/CreateEventRequest.java`
- The trigger: any controller method with `@Valid` (e.g. `user/AuthController.java`)
- The nice error output: `common/error/GlobalExceptionHandler.java`
  (`handleValidation` builds the field→message map)

---

## 8. REST controllers & HTTP status codes (speaking HTTP correctly)

**Concept:** A good API uses the right **status codes**: `201 Created` when you make something,
`200 OK` for a normal read, `404` when it's missing, `409` for a conflict, `401/403` for auth.
Controllers map URLs (`GET /api/events`) to methods.

**📁 Where to look:**
- `event/EventController.java` (note `ResponseEntity.status(HttpStatus.CREATED)` on create,
  plain returns for reads, and the `@GetMapping`/`@PostMapping` URL mappings)

---

## 9. Passwords & hashing (never store the real password)

**Concept:** We never store a password as text. We store a **BCrypt hash** — a one-way
scramble. At login we hash what the user typed and compare hashes. Even if the database leaks,
the real passwords aren't in it.

**📁 Where to look:**
- Encoding on register: `user/UserService.java` (`encoder.encode(...)`)
- The encoder bean: `security/SecurityConfig.java` (`BCryptPasswordEncoder`)

---

## 10. JWT authentication (staying logged in without sessions)

**Concept:** After login, the server hands the client a **JWT** — a signed token (like a
tamper-proof wristband) that says "this is user X with role Y." The client sends it on every
later request in the `Authorization: Bearer …` header. The server just checks the signature —
it doesn't need to remember anything (this is called **stateless**).

**How it flows here:**
1. Login verifies the password and **issues** a token.
2. A **filter** runs on every request, reads the token, and tells Spring who you are.

**📁 Where to look:**
- Make/verify tokens: `security/JwtService.java` (`issue` and `parse`)
- Login endpoint: `user/AuthController.java` (`login` → returns `TokenResponse`)
- The per-request check: `security/JwtAuthenticationFilter.java`
- Loading a user for password login: `security/AppUserDetailsService.java`

---

## 11. Roles & two kinds of authorization

**Concept:** Three roles: `ATTENDEE`, `ORGANIZER`, `ADMIN`. We guard access **two** ways:

- **URL rules** — broad strokes ("only ADMIN can touch `/api/admin/**`").
- **Method security** — precise rules right next to the code
  (`@PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")` on "create event").

**And a third, subtler one — ownership:** "is this *your* booking?" isn't about your role, it's
about the data. That check lives inside the service.

**📁 Where to look:**
- URL rules + turning on method security: `security/SecurityConfig.java`
- Method rule example: `event/EventController.java` (`@PreAuthorize`)
- Ownership check: `booking/BookingService.java` (`requireOwnedBooking`)

---

## 12. Error handling the standard way (RFC 7807 `ProblemDetail`)

**Concept:** When something goes wrong, the API should return a **consistent** JSON error, not
a random stack trace. There's an official format called **RFC 7807 ProblemDetail**
(`type`, `title`, `status`, `detail`). We define our own exceptions (`NotFoundException`,
`ConflictException`) and one central place turns any exception into that format.

**📁 Where to look:**
- The central translator: `common/error/GlobalExceptionHandler.java` (`@RestControllerAdvice`)
- Our exceptions: `common/error/NotFoundException.java` (→404),
  `common/error/ConflictException.java` (→409)
- Auth errors in the same format: `security/SecurityErrorHandlers.java` (401/403)

---

## 13. Profiles (same app, two setups: `dev` vs `postgres`)

**Concept:** A **profile** is a named configuration. The app can run in `dev` mode (a fake
in-memory database, zero setup) or `postgres` mode (a real database). You pick one at startup;
the code doesn't change.

**📁 Where to look:**
- Shared config + default profile: `../../src/main/resources/application.yml`
- Dev (H2, auto-created schema): `application-dev.yml`
- Postgres (real DB, strict schema): `application-postgres.yml`

---

## 14. Flyway migrations (versioned database setup)

**Concept:** For the real database, we don't let the app guess the schema. We write numbered
SQL files (`V1__…`, `V2__…`). Flyway runs them in order, once, and records which have run. This
makes the schema **reproducible** on every machine.

**📁 Where to look:**
- `../../src/main/resources/db/migration/V1__core_schema.sql` (users, venues, events, ticket types)
- `../../src/main/resources/db/migration/V2__booking_schema.sql` (bookings, booking items)

---

## 15. `ddl-auto=validate` (a safety net against schema drift)

**Concept:** On the real database, Hibernate is set to **validate**, meaning: "check that my
Java entities match the actual tables, and if they don't, refuse to start." This catches the
bug where someone changes an entity but forgets to write the migration.

(In `dev` only, we relax this to auto-create the schema so there's zero setup.)

**📁 Where to look:**
- Strict: `application-postgres.yml` (`ddl-auto: validate`)
- Relaxed (dev only): `application-dev.yml` (`ddl-auto: create-drop`)

---

## 16. Seat holds + optimistic locking (the trickiest, most important bit)

**Concept — the problem:** Two people try to grab the **last two seats** at the same instant.
Naively, both could succeed and you'd **oversell**.

**Concept — the fix (optimistic locking):** Each ticket type has a hidden **version number**
(`@Version`). When someone books, we read the row, and on saving, the database checks "is the
version still what I read?" If two people race, only the first save wins; the second sees the
version changed and **fails safely** (no seat lost, none double-sold). We even force the version
to bump on read (`OPTIMISTIC_FORCE_INCREMENT`) so the race is caught every time.

**How a "hold" works:** booking **decrements** `quantityAvailable` immediately (seats reserved)
and sets an `expiresAt`. Pay in time → kept. Don't → a scheduled job returns the seats.

**📁 Where to look:**
- The version fields: `event/TicketType.java` and `booking/Booking.java` (`@Version`)
- The forced-increment read: `event/TicketTypeRepository.java` (`@Lock(OPTIMISTIC_FORCE_INCREMENT)`)
- Holding/releasing seats: `booking/BookingService.java` (`createBooking`, `releaseSeats`)

---

## 17. Transactions (all-or-nothing database work)

**Concept:** A **transaction** groups database changes so they all succeed or all roll back —
never half. Booking touches several rows (the booking, its items, the seat counts); `@Transactional`
makes that one atomic unit.

**📁 Where to look:** `booking/BookingService.java` — the `@Transactional` on `createBooking`,
`pay`, `cancel`. (Also on `EventService`, `UserService`, etc.)

---

## 18. The stubbed payment (hidden behind an interface)

**Concept:** We don't integrate a real bank. But we don't scatter fake code either — we define
an **interface** `PaymentGateway` (the contract: "charge this amount") and a fake implementation.
Later, swapping in a real provider is a one-class change; `BookingService` never knows the
difference. This is "program to an interface."

**📁 Where to look:**
- The contract: `payment/PaymentGateway.java`
- The fake: `payment/FakePaymentGateway.java`
- The user of it: `booking/BookingService.java` (`pay` calls `paymentGateway.charge(...)`)

---

## 19. Caching with Caffeine (make the busy read fast)

**Concept:** A **cache** remembers the answer to an expensive/frequent question so you don't
redo the work. But caching the *wrong* thing causes stale/incorrect data. So we cache **exactly
one** thing: the public "list of published events" (read constantly, changes rarely). When an
organizer creates/publishes/cancels, we **evict** (clear) the cache so it's never stale.

**📁 Where to look:**
- The cache setup + the "why only here" note: `config/CacheConfig.java`
- `@Cacheable` on the read, `@CacheEvict` on the writes: `event/EventService.java`

---

## 20. Scheduling (background jobs that run on a timer)

**Concept:** Some work isn't triggered by a user — it runs on a clock. Two jobs here:
- **Expire stale holds** — every minute, find bookings whose hold lapsed and free the seats.
- **Send reminders** — periodically nudge people about upcoming paid bookings.

**📁 Where to look:**
- The timed jobs: `booking/BookingScheduler.java` (`@Scheduled`)
- The work they call: `booking/BookingService.java` (`expireStaleHolds`, `sendUpcomingReminders`)
- Turned on in: `BookingApplication.java` (`@EnableScheduling`)

---

## 21. In-process events + "after commit" (do follow-up work safely)

**Concept:** When a booking is paid, we want to "issue tickets / notify." But we must do that
**only after the payment is truly saved**. So instead of calling it directly, the service
**publishes an event** ("BookingPaid"), and a **listener** handles it — but the listener is set
to run **AFTER_COMMIT** (only once the database transaction has committed). This means a hiccup
in ticket-issuing can never undo a captured payment.

**📁 Where to look:**
- Publishing the event: `booking/BookingService.java` (`eventPublisher.publishEvent(...)` in `pay`)
- The event object: `booking/BookingPaidEvent.java`
- The after-commit handler: `booking/BookingPaidListener.java`
  (`@TransactionalEventListener(AFTER_COMMIT)`)

---

## 22. Actuator (health & metrics endpoints)

**Concept:** Spring Boot can expose "ops" endpoints — is the app healthy? what are its metrics?
We expose **only** `health`, `info`, and `metrics` (nothing sensitive).

**📁 Where to look:** `../../src/main/resources/application.yml` (the `management.endpoints` section).
Try `GET /actuator/health`.

---

## 23. Typed configuration (`@ConfigurationProperties`)

**Concept:** Rather than sprinkling `@Value("${...}")` strings around, we bind the whole
`booking.*` config block (JWT secret, hold window, reminder interval) into one typed object.
Cleaner and checked at startup.

**📁 Where to look:**
- The typed holder: `config/BookingProperties.java`
- The values it reads: `application.yml` (the `booking:` block)
- Used by: `security/JwtService.java`, `booking/BookingService.java`

---

## 24. Testing (proving it works, two levels)

**Concept — two kinds of tests:**
- **Unit tests** — fast, test one class in isolation using *mocks* (fake collaborators). They
  run in the normal build. Tool: Mockito.
- **Integration tests** — slow, test the whole app against a **real Postgres** (started in
  Docker by Testcontainers). We keep these separate so the normal build needs no Docker.

**Why the split matters here:** this machine has no Docker, so the build runs the 16 unit tests
and skips the integration test. On a machine with Docker: `mvn verify -DskipITs=false`.

**📁 Where to look:**
- Unit (the best example — reads like a spec): `src/test/java/.../booking/BookingServiceTest.java`
- More unit tests: `.../user/UserServiceTest.java`, `.../event/EventServiceTest.java`,
  `.../security/JwtServiceTest.java`
- Context boots: `.../BookingApplicationTests.java`
- Integration (real Postgres, full HTTP flow): `.../BookingFlowIT.java`

---

## 25. Putting it all together (one paid ticket, end to end)

Follow this and you've touched every concept above:

1. Organizer **registers** (§9 hashing) and **logs in** (§10 JWT).
2. Creates a **venue** and an **event** with **ticket types** (§4 entities, §7 validation),
   then **publishes** it (§11 roles, §19 cache evicts).
3. Anyone **lists events** — served from the **cache** (§19).
4. Attendee **books** 2 seats → seats **held** via **optimistic locking** inside a
   **transaction** (§16, §17). Response is a **DTO** with `PENDING` (§6, §8).
5. Attendee **pays** → **stub gateway** charges (§18); booking becomes `PAID`; a
   **BookingPaid event** fires **after commit** and **issues tickets** (§21).
6. If they *hadn't* paid in time, the **scheduler** would expire the hold and free the seats (§20).
7. Any mistake along the way comes back as a clean **ProblemDetail** error (§12).

The test `BookingFlowIT.java` literally performs steps 1–5 against a real database — so if you
want to *watch* it happen, read that test.

---

### Suggested path through the actual code (in order)

1. `BookingApplication.java` — where it all starts (§1)
2. `booking/BookingService.java` — the heart; holds, payment, events (§16–21)
3. `security/SecurityConfig.java` + `security/JwtAuthenticationFilter.java` — how auth works (§10–11)
4. `event/EventService.java` — caching + ownership in one place (§11, §19)
5. `common/error/GlobalExceptionHandler.java` — how errors become clean JSON (§12)
6. `src/test/java/.../booking/BookingServiceTest.java` — the rules, as runnable examples (§24)
