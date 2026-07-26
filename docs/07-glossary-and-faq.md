# 7. Glossary & FAQ

---

## 7.1 Glossary

| Term | Meaning in this project |
|---|---|
| **Aggregate / aggregate root** | A cluster of entities treated as one unit with one entry point. `Booking` (with its `BookingItem`s) and `Event` (with its `TicketType`s) are aggregates. |
| **Seat hold** | When a booking is created it decrements a tier's `quantityAvailable`, reserving seats *before* payment. Released on pay-timeout, cancel, or expiry. |
| **Hold window** | `booking.hold.window-minutes` (15) — how long a PENDING booking keeps its seats before the scheduler may expire it. |
| **Optimistic locking** | Concurrency control via a `@Version` column: readers don't lock rows; on write, a stale version loses. Used on the seat/booking aggregate to prevent overselling. |
| **`OPTIMISTIC_FORCE_INCREMENT`** | A JPA lock mode that bumps the version even on a read, so two concurrent seat-holds on the same tier can't both commit. |
| **After-commit event** | A domain event delivered only once the surrounding DB transaction has committed (`@TransactionalEventListener(AFTER_COMMIT)`). Used for ticket issuance so it can't undo a captured payment. |
| **RFC 7807 / ProblemDetail** | The standard JSON error format (`type/title/status/detail`). Spring's `ProblemDetail` type; we never invent a custom error POJO. |
| **DTO** | Data Transfer Object — the request/response shapes on the wire, implemented as Java `record`s. Entities are never serialized directly. |
| **Package-by-feature** | Organising code by domain concept (`booking/`, `event/`) rather than by technical layer (`controllers/`, `services/`). |
| **Profile** | A named config set. `dev` (H2, auto-schema) is the default; `postgres` (real DB, Flyway, validate) is for prod-like runs. |
| **Flyway** | Versioned SQL migrations (`V1__…`, `V2__…`) that build the Postgres schema deterministically. |
| **`ddl-auto=validate`** | Hibernate checks entities against the existing schema at startup and refuses to run on mismatch — instead of altering tables itself. |
| **Surefire / Failsafe** | Maven's unit-test (`*Test`) and integration-test (`*IT`) runners. We split them so the default build needs no Docker. |
| **Testcontainers** | Library that boots a throwaway Postgres in Docker for integration tests. |
| **JWT (HS256)** | JSON Web Token signed with a shared secret; carries the user's email (subject) and role, enabling stateless auth. |

---

## 7.2 FAQ — "why did you do X?"

**Q. Why is the runtime Java 25 but the target Java 21?**
The spec mandates Java 21. The machine only has JDK 25 installed, and you asked me to keep
going rather than hunt for a JDK 21. So I compile to Java 21 bytecode (`<release>21</release>`)
using JDK 25's compiler. See doc 1 for the Lombok pin that this made necessary.

**Q. Why are integration tests skipped in `mvn install`?**
No Docker daemon here, and the ITs use Testcontainers. Per the brief's guidance, I bound them
to Failsafe and excluded them from the default build, so the gate is compile + unit tests +
context load. Run them with `mvn verify -DskipITs=false` where Docker exists.

**Q. Why cache only the event listing?**
It's the one read-mostly, high-traffic, low-churn path. Caching write-heavy, correctness-
sensitive data (seats, bookings) would risk overselling. The three event mutators evict the
cache, and it stores DTOs (not entities) to avoid lazy-loading traps. Full reasoning in doc 1.

**Q. Why `long` cents instead of `BigDecimal`?**
Money in minor units as integers avoids floating-point rounding entirely and is trivial to
sum. A single currency code per booking is enforced so amounts are always comparable.

**Q. Why is ticket issuance an after-commit event instead of inline code in `pay()`?**
So issuance/notification happen only once the payment is durably committed. If issuance were
inline and then something failed, you could roll back a payment you'd already captured, or
issue tickets for a payment that later rolled back. AFTER_COMMIT cleanly separates the two.

**Q. Why does `TicketTypeRepository` override `findById` with a lock?**
That single method is the seat-hold read. Forcing a version increment there is what makes two
racing bookings for the last seats resolve safely (one wins, one retries). Regular reads
elsewhere are unaffected.

**Q. Where is ownership enforced — roles or code?**
Roles (via `@PreAuthorize`) gate *who may call* an endpoint. **Ownership** ("is this *your*
booking/event?") is data-dependent, so it's enforced inside the services, raising 403/409.

**Q. Why records for DTOs but Lombok for entities?**
Records are immutable and perfect for transfer objects. JPA entities need a no-arg constructor,
mutable fields, and builders — exactly what Lombok generates cleanly. Using each where it fits.

**Q. Is the payment real?**
No — it's stubbed behind the `PaymentGateway` interface (`FakePaymentGateway`), as required.
Swapping in a real provider is a one-class change with no impact on `BookingService`.

**Q. What does "package it as a zip" mean here?**
The build produces an executable fat **jar**, which is itself a zip archive. If you want a
named `.zip` bundle (jar + docs), doc 6 §6.5 has a copy-paste recipe, and I can wire it into
the build on request.

---

## 7.3 Known gaps / conscious non-goals

These are intentional, given the "stop at a working installable build" boundary:

- **No app Dockerfile / cloud deploy / prod hardening** beyond actuator + profiles.
- **Notifications and payments are stubs** (log-based / always-approve).
- **No refresh tokens or logout/deny-list** — access tokens are short-lived and stateless.
- **Scheduled jobs assume a single instance** — running multiple app instances would need a
  distributed lock so holds aren't swept twice (harmless here, but noted).
- **No pagination** on list endpoints yet — fine at demo scale; add `Pageable` for large data.
- **Admin bootstrap** — since ADMIN can't self-register, the first admin is provisioned
  out-of-band (e.g. a seed migration or a manual DB insert). Doc 6 can be extended with a seed.

Doc 6 §6.8 lays out the concrete "ship it for real" roadmap for each of these.

---

## 7.4 Where to start reading the code

1. `BookingService.java` — the most interesting logic (holds, payment, lifecycle, events).
2. `SecurityConfig.java` + `JwtAuthenticationFilter.java` — how auth actually works.
3. `EventService.java` — caching + eviction + ownership checks in one place.
4. `GlobalExceptionHandler.java` — how every error becomes a ProblemDetail.
5. The tests under `src/test` — they double as executable documentation of the rules above.
