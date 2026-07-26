# @SRIJA - IGNORE THIS FILE, this is for Claude.

# PROJECT CONTEXT — read this first (for future work / AI assistance)

> **Purpose of this file.** This is a self-contained context brief. If you come back
> later to extend the project or ask questions (e.g. "how do we deploy this to
> production?", "add a real payment gateway", "add a front-end"), share or point to
> **this file** at the start of the conversation. It captures what the project is, how
> it's built, the decisions and their reasons, the current state, what's intentionally
> unfinished, and the likely next steps — so nothing has to be re-derived.

---

## 0. How to use this file in a future chat

Start the conversation with something like:

> "Here's my project context: `docs/PROJECT-CONTEXT.md`. Read it, then help me with
> <your question>."

Then ask anything — deployment, a new feature, scaling, security hardening, etc.
Section 12 lists topics already anticipated.

---

## 1. What the project is (one paragraph)

An **event/ticket booking platform** — a backend REST API (no UI of its own). Organizers
create **venues** and **events** with priced **ticket types**; attendees create **bookings**
that **hold seats** for 15 minutes and **expire if unpaid**; **payment is stubbed** behind an
interface; **admins** moderate users/events. Auth is **JWT** with roles ATTENDEE / ORGANIZER /
ADMIN. It's built as a realistic, well-documented reference app (partly a learning artifact).

---

## 2. Repo, location, and identity

- **GitHub:** https://github.com/ramakrishna975/TicketBooking  (branch `main`)
- **Local dev path (author's machine):** `/Users/ram-13951/Personal/project`
- **Maven coordinates:** groupId `com.example`, artifactId `booking-platform`, version
  `0.0.1-SNAPSHOT`
- **Base package:** `com.example.booking`
- **Runnable jar:** `target/booking-platform-0.0.1-SNAPSHOT.jar`

---

## 3. Tech stack and exact versions

| Area | Choice / version |
|---|---|
| Language | Java **21** (bytecode target) |
| Framework | Spring Boot **3.4.2** (Web, Data JPA, Security, Validation, Actuator, Cache) |
| Build | Maven (no wrapper — use installed `mvn`, or IntelliJ's bundled Maven) |
| DB (dev) | **H2** in-memory (default `dev` profile) |
| DB (prod-like) | **PostgreSQL** (`postgres` profile) |
| Schema | **Flyway** (postgres only): `db/migration/V1__core_schema.sql`, `V2__booking_schema.sql` |
| Auth | **JWT HS256** via `io.jsonwebtoken:jjwt` **0.12.6** |
| Cache | **Caffeine** |
| API docs | **springdoc-openapi** **2.7.0** (Swagger UI) |
| Boilerplate | **Lombok** — pinned to **1.18.38** (see §5), records for DTOs |
| Tests | JUnit 5, Mockito (unit), **Testcontainers 1.20.4** (integration) |

---

## 4. Architecture & conventions (follow these when extending)

- **Package-by-feature.** Each feature owns its controller/service/repo/entities/dto under
  `com.example.booking.<feature>` (`user`, `security`, `venue`, `event`, `booking`, `payment`,
  `notification`, `admin`, plus `common/error`, `config`, `ping`).
- **Layers:** Controller (thin, no business logic) → Service (`@Transactional`, business rules,
  ownership checks) → Repository (Spring Data JPA).
- **DTOs on the wire, never entities.** Request/response types are Java `record`s with Bean
  Validation annotations; entities never get serialized. Add a `from(entity)` static mapper.
- **Errors = RFC 7807 `ProblemDetail`** via one `@RestControllerAdvice`
  (`common/error/GlobalExceptionHandler`). Domain exceptions: `NotFoundException` (404),
  `ConflictException` (409). No custom error POJO.
- **Config** is bound via `@ConfigurationProperties` (`config/BookingProperties`) for the
  `booking.*` tree; avoid scattering `@Value`.
- **Money** is `long` cents; timestamps are `Instant` stored in UTC.
- **Repositories that feed DTO mapping use `@EntityGraph`** to fetch associations up front
  (open-in-view is OFF), to avoid `LazyInitializationException`.
- **Security:** stateless JWT; URL rules in `SecurityConfig` + method `@PreAuthorize` +
  data-level ownership checks in services.
- **Naming for tests:** unit tests `*Test` (Surefire), integration tests `*IT` (Failsafe).

---

## 5. Key decisions and WHY (don't re-litigate without reason)

1. **Docker not used in the build.** The author's machine has no Docker daemon. Integration
   tests (`*IT`, Testcontainers) are bound to **Failsafe** and **excluded from `mvn install`**
   via `skipITs=true`. So the build gate = compile + unit tests + Spring context load. Run ITs
   with `mvn verify -DskipITs=false` where Docker exists.
2. **Java 21 target on a JDK 25 machine.** Compiles with `--release 21`. Because Spring Boot's
   managed Lombok (1.18.36) can't parse JDK 25's compiler, **Lombok is pinned to 1.18.38** and
   `-Dnet.bytebuddy.experimental=true` is set for Surefire/Failsafe. On a real JDK 21 these are
   harmless and could be reverted.
3. **H2 for dev, PostgreSQL for prod-like.** `dev` is the default profile (zero setup). Only
   dev uses `ddl-auto=create-drop`; postgres uses `ddl-auto=validate` with Flyway owning the
   schema. See `docs/PROJECT-DOCUMENTATION.txt` §6 for line-by-line config.
4. **Payment is stubbed behind `PaymentGateway`** (`FakePaymentGateway`) — swap-in a real
   provider without touching booking logic.
5. **Notifications are stubbed** (`NotificationService` logs) — one seam to plug in email/SMS.
6. **Ticket issuance is an AFTER_COMMIT event** (`BookingPaidEvent` + `BookingPaidListener`)
   so tickets are only issued once payment is durably committed.
7. **Overselling prevented via optimistic locking** — `@Version` on `TicketType`/`Booking`,
   and `TicketTypeRepository.findById` uses `OPTIMISTIC_FORCE_INCREMENT`.
8. **Caching only the public event listing** (Caffeine `publishedEvents`), evicted on
   create/publish/cancel; DTOs cached (not entities).
9. **Dev-only admin seeder** (`config/DevDataInitializer`, `@Profile("dev")`) creates
   `admin@x.com` / `password1` because ADMIN can't self-register. NOT active on postgres.

---

## 6. Domain model (quick reference)

- `User` (email, passwordHash BCrypt, displayName, role, enabled, version) — roles
  ATTENDEE/ORGANIZER/ADMIN.
- `Venue` (name, address, city, capacity, version).
- `Event` (title, description, venue, organizer, startsAt, endsAt, status, ticketTypes, version)
  — status DRAFT/PUBLISHED/CANCELLED.
- `TicketType` (event, name, priceCents, currency, quantityTotal, quantityAvailable, version).
- `Booking` (event, attendee, status, items, totalCents, currency, paymentReference, createdAt,
  expiresAt, version) — status PENDING/PAID/EXPIRED/CANCELLED.
- `BookingItem` (booking, ticketType, quantity, unitPriceCents).

Tables: `app_user`, `venue`, `event`, `ticket_type`, `booking`, `booking_item`.

---

## 7. Current status (as of this writing)

- **Build:** `mvn install` → **BUILD SUCCESS**, **18 unit tests pass**, jar packaged.
- **All REST endpoints manually verified** end-to-end (happy paths + auth/role failures +
  business-rule errors) against a running instance.
- **Two bugs found and fixed during testing:**
  (a) `POST /api/auth/register` returned 500 on an invalid `role`/malformed body → now 400
  (added `HttpMessageNotReadableException`/`MethodArgumentTypeMismatchException` handlers);
  (b) `GET /api/events/mine` returned 500 (lazy load) → fixed with `@EntityGraph` on
  `findByOrganizerId`; the catch-all handler now also logs the real cause.
- **Docs:** extensive `docs/` folder (see `docs/FILE-INDEX.txt`), Swagger UI, exported OpenAPI
  (`docs/openapi.json`/`.yaml`), Postman collection, and per-file bottom-of-file comments
  (technical + "in simple words").

---

## 8. What is intentionally NOT built (scope boundary)

- No real payment provider (stub only).
- No real email/SMS (logs only).
- No front-end / UI (backend API only).
- No deployment artifacts (no app Dockerfile, no cloud config, no CI pipeline yet).
- No refresh tokens / token revocation; access tokens only (120 min).
- No pagination on list endpoints; no rate limiting; no CORS config for a browser front-end.
- Scheduled jobs assume a single instance (no distributed lock).

---

## 9. How to run / build (recap)

```bash
# Dev (H2, zero setup):
mvn spring-boot:run                     # http://localhost:8080/swagger-ui.html
# Build + unit tests + jar:
mvn install
# Integration tests (needs Docker):
mvn verify -DskipITs=false
# Postgres profile:
docker compose up -d
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```
Dev admin: `admin@x.com` / `password1`.

---

## 10. Extension points — how to add common things

- **Real payment gateway:** implement `payment/PaymentGateway` in a new class (e.g.
  `StripePaymentGateway`), mark it the primary bean (or profile-select it), keep
  `FakePaymentGateway` for tests. `BookingService.pay(...)` needs no change.
- **Real notifications:** replace/extend `notification/NotificationService` with an email/SMS
  implementation; it's already called from the AFTER_COMMIT listener and the scheduler.
- **A new feature/entity:** create a new package `com.example.booking.<feature>` with
  entity + repository + service + controller + dtos; add a Flyway migration `V3__...sql` for
  the postgres schema (and rely on `create-drop` in dev). Keep DTOs/records + ProblemDetail
  conventions.
- **Pagination:** switch list endpoints/repositories to `Pageable`/`Page<>`.
- **Refresh tokens:** add a refresh-token store + endpoint; shorten access-token TTL.
- **New role/permission:** extend `Role` and the `@PreAuthorize`/URL rules in `SecurityConfig`.

---

## 11. PRODUCTION DEPLOYMENT — starting brief (to expand later)

This is the anticipated big future topic. Nothing here is built yet; this is the checklist to
work through when the time comes (ask for any of these in detail):

1. **Config & secrets:** externalize `booking.jwt.secret` and DB credentials into a secret
   manager / env vars; fail fast if the JWT secret is the dev placeholder. Use the `postgres`
   profile in production.
2. **Database:** provision a managed PostgreSQL; run **Flyway** migrations as a gated pipeline
   step; plan backups and a rollback strategy. `ddl-auto=validate` stays on.
3. **Containerization:** add a Dockerfile (or `spring-boot:build-image`/buildpacks) to produce
   an OCI image; wire the existing `docker-compose.yml` (or a new one) for app+db locally;
   real orchestration via Kubernetes/ECS/Cloud Run as chosen.
4. **Security hardening:** HTTPS/TLS termination; shorter access-token TTL + refresh tokens;
   rate-limit `/api/auth/*`; configure CORS for the front-end origin; lock down Actuator
   (auth + network); consider secrets rotation.
5. **Observability:** ship metrics (Micrometer → Prometheus/Grafana or vendor), structured
   logging, tracing; alerts on the scheduled jobs and error rates.
6. **Scaling & resilience:** make the scheduled jobs safe under multiple instances (ShedLock or
   a leader election); add retry around optimistic-lock failures on booking creation; tune the
   connection pool.
7. **CI/CD:** pipeline running `mvn install` (unit) then `mvn verify -DskipITs=false` (ITs, on a
   Docker-capable runner), build image, deploy per environment.
8. **Operational endpoints:** health/readiness/liveness probes (Actuator already exposes
   `health`), graceful shutdown.

When you ask "how do we deploy to production", start from this list and we'll go deep on the
chosen platform (e.g. AWS, GCP, Azure, or a PaaS like Render/Railway/Fly.io).

---

## 12. Likely future questions (already anticipated)

- Deploy to production (see §11) — on a specific cloud/PaaS.
- Add a real payment provider (Stripe/Razorpay) behind `PaymentGateway`.
- Add email/SMS notifications.
- Add a front-end (React/Angular/mobile) consuming this API + CORS.
- Add pagination, search/filtering on events.
- Refresh tokens / logout / token revocation.
- Multi-instance safety for scheduled jobs.
- Performance/load testing and tuning.
- More tests (a real concurrency test proving the anti-oversell lock end-to-end).
- Multi-tenancy, discounts/promo codes, refunds, waitlists (new features).

---

## 13. Where the detail lives

- `docs/FILE-INDEX.txt` — map of all docs + a 7-step reading path.
- `docs/PROJECT-DOCUMENTATION.txt` — everything in one file (incl. config/DB line-by-line).
- `docs/md/02-architecture.md`, `docs/md/03-file-guide.md` — architecture & per-file guide.
- `docs/md/08-learning-guide.md` — concept-by-concept with file pointers.
- Bottom of every source file — technical + plain-language comments.
