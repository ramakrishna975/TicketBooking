# 5. API Reference

Base URL (dev): `http://localhost:8080`. All bodies are JSON. Authenticated endpoints expect
`Authorization: Bearer <accessToken>` from `/api/auth/login`.

Roles: **ATTENDEE**, **ORGANIZER**, **ADMIN**. "Public" = no token required.

Errors are RFC 7807 `application/problem+json`, e.g.:
```json
{ "type":"https://booking.example.com/problems/conflict",
  "title":"Conflict", "status":409, "detail":"Email already registered: a@x.com" }
```
Validation errors add an `errors` map: `{ "email":"must not be blank", "password":"size must be…" }`.

---

## Auth — `/api/auth` (public)

### POST `/api/auth/register` → 201
```json
// request
{ "email":"a@x.com", "password":"password1", "displayName":"Ann", "role":"ATTENDEE" }
// response (UserResponse)
{ "id":1, "email":"a@x.com", "displayName":"Ann", "role":"ATTENDEE", "enabled":true, "createdAt":"…" }
```
`role` may be `ATTENDEE` or `ORGANIZER`. `ADMIN` self-registration → **409**. Duplicate email → **409**.

### POST `/api/auth/login` → 200
```json
// request
{ "email":"a@x.com", "password":"password1" }
// response (TokenResponse)
{ "accessToken":"eyJhbGciOiJIUzI1NiJ9…", "tokenType":"Bearer", "expiresInSeconds":7200 }
```
Bad credentials → **401**.

---

## Users — `/api/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users/me` | any authenticated | The caller's own profile (`UserResponse`). |

---

## Venues — `/api/venues`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/venues` | public | List all venues. |
| GET | `/api/venues/{id}` | public | One venue; unknown id → **404**. |
| POST | `/api/venues` | ORGANIZER/ADMIN | Create → **201**. |

```json
// POST /api/venues request (VenueRequest)
{ "name":"Main Hall", "address":"1 Road", "city":"Metropolis", "capacity":100 }
```

---

## Events — `/api/events`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/events` | public | **Cached** list of PUBLISHED events. |
| GET | `/api/events/{id}` | public | Event detail (incl. ticket types); unknown → **404**. |
| GET | `/api/events/mine` | ORGANIZER/ADMIN | Events owned by the caller (any status). |
| POST | `/api/events` | ORGANIZER/ADMIN | Create (DRAFT) → **201**. |
| POST | `/api/events/{id}/publish` | ORGANIZER/ADMIN (owner) | DRAFT → PUBLISHED. |
| POST | `/api/events/{id}/cancel` | ORGANIZER/ADMIN (owner) | → CANCELLED. |

```json
// POST /api/events request (CreateEventRequest)
{ "title":"Concert", "description":"Live show", "venueId":1,
  "startsAt":"2026-08-01T19:00:00Z", "endsAt":"2026-08-01T22:00:00Z",
  "ticketTypes":[ {"name":"General","priceCents":5000,"currency":"USD","quantityTotal":80},
                  {"name":"VIP","priceCents":15000,"currency":"USD","quantityTotal":20} ] }
```
Rules: `startsAt`/`endsAt` must be in the future; `endsAt > startsAt` (else **409**); the sum
of `quantityTotal` must not exceed the venue's `capacity` (else **409**). Publishing/cancelling
an event you don't own → **409**.

```json
// EventResponse (abridged)
{ "id":1, "title":"Concert", "venueId":1, "venueName":"Main Hall", "organizerId":2,
  "startsAt":"…", "endsAt":"…", "status":"PUBLISHED",
  "ticketTypes":[ {"id":100,"name":"General","priceCents":5000,"currency":"USD",
                   "quantityTotal":80,"quantityAvailable":78} ] }
```

---

## Bookings — `/api/bookings` (ATTENDEE/ADMIN)

| Method | Path | Description |
|---|---|---|
| POST | `/api/bookings` | Create a PENDING booking that **holds seats** → **201**. |
| GET | `/api/bookings` | The caller's bookings. |
| GET | `/api/bookings/{id}` | One of the caller's bookings; someone else's → **403**. |
| POST | `/api/bookings/{id}/pay` | Charge (stub) and mark PAID. |
| POST | `/api/bookings/{id}/cancel` | Cancel a PENDING booking; releases held seats. |

```json
// POST /api/bookings request (CreateBookingRequest)
{ "eventId":1, "items":[ {"ticketTypeId":100, "quantity":2} ] }
```
Rules: event must be PUBLISHED (else **409**); each tier must belong to the event; all tiers
in one booking share a currency; `quantity` ≥ available (else **409**, "Not enough seats").

```json
// BookingResponse
{ "id":1, "eventId":1, "attendeeId":3, "status":"PENDING",
  "totalCents":10000, "currency":"USD", "paymentReference":null,
  "createdAt":"…", "expiresAt":"…+15m",
  "items":[ {"ticketTypeId":100,"ticketTypeName":"General","quantity":2,"unitPriceCents":5000} ] }
```
Pay rules: booking must be PENDING and not expired (else **409**, payment not attempted); a
declined charge → **409**. On success `status=PAID`, `paymentReference="FAKE-…"`, `expiresAt=null`.

---

## Admin — `/api/admin` (ADMIN only)

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/users` | List all users. |
| POST | `/api/admin/users/{id}/disable` | Disable an account. |
| POST | `/api/admin/users/{id}/enable` | Re-enable an account. |
| POST | `/api/admin/events/{id}/cancel` | Force-cancel any event (ignores ownership). |

---

## Actuator — `/actuator`

| Path | Auth | Notes |
|---|---|---|
| `/actuator/health` | public | Liveness/readiness. |
| `/actuator/info` | public | App info. |
| `/actuator/metrics` | ADMIN | Metrics (only `health,info,metrics` are exposed at all). |

---

## Ping — `/api/ping` (public)

`GET /api/ping` → `{ "status":"ok", "time":"…" }`. Boot/liveness probe.

---

## Quick cURL smoke test (dev profile)

```bash
# 1. register + login an organizer
curl -sX POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"org@x.com","password":"password1","displayName":"Org","role":"ORGANIZER"}'
ORG=$(curl -sX POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"org@x.com","password":"password1"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

# 2. venue + event + publish
VID=$(curl -sX POST localhost:8080/api/venues -H "Authorization: Bearer $ORG" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Hall","address":"1 Rd","city":"Metro","capacity":100}' | sed 's/.*"id":\([0-9]*\).*/\1/')
# …create the event with venueId=$VID, then POST /api/events/{id}/publish

# 3. public listing (cached)
curl -s localhost:8080/api/events
```
