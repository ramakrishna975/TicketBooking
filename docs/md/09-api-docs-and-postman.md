# 9. Swagger / OpenAPI & Postman

Two ways to explore and call the API: interactive **Swagger UI** (live, always in sync with
the code) and a ready-to-run **Postman collection**.

---

## 9.1 Swagger UI (interactive, live)

Added via **springdoc-openapi** (`../../pom.xml`). Start the app and open the UI:

```bash
mvn spring-boot:run           # dev profile, http://localhost:8080
```

| What | URL |
|---|---|
| Swagger UI (try-it-out) | http://localhost:8080/swagger-ui.html |
| OpenAPI document (JSON) | http://localhost:8080/v3/api-docs |
| OpenAPI document (YAML) | http://localhost:8080/v3/api-docs.yaml |

**Calling secured endpoints from Swagger UI:**
1. Run `POST /api/auth/register`, then `POST /api/auth/login` and copy the `accessToken`.
2. Click **Authorize** (top-right), paste the token (just the token — the `bearerAuth` scheme
   adds `Bearer ` for you), and confirm.
3. Now the locked endpoints (events/bookings/admin) send your JWT automatically.

The docs endpoints and Swagger UI are open without a token — see the permit rules in
`security/SecurityConfig.java`. The JWT "Authorize" button is configured in
`config/OpenApiConfig.java`.

---

## 9.2 The exported OpenAPI document (offline file)

I ran the app and exported the live spec so you have it as a file without starting anything:

| File | Format | Use |
|---|---|---|
| [`../openapi.json`](../openapi.json) | OpenAPI 3.0.1 JSON | Import into Postman/Insomnia, codegen, API tooling |
| [`../openapi.yaml`](../openapi.yaml) | OpenAPI 3.0.1 YAML | Human-readable, diff-friendly, docs pipelines |

It describes **22 operations** across **19 paths**, all request/response **schemas** (14), and
the `bearerAuth` JWT security scheme. Regenerate anytime by hitting `/v3/api-docs(.yaml)` while
the app runs — it's generated from the code, so it can't drift.

You can also import `../openapi.json` straight into Postman (**Import → File**) if you'd rather
Postman build requests from the spec than use the curated collection below.

---

## 9.3 Postman collection (curated, chained flow)

File: [`../booking-platform.postman_collection.json`](../booking-platform.postman_collection.json)

**Import:** Postman → **Import** → select the file. It appears as *Booking Platform API* with
folders: Auth, Users, Venues, Events, Bookings, Admin, Ops.

**What makes it "just work":**
- A collection variable **`baseUrl`** = `http://localhost:8080` (change once to point elsewhere).
- **Login requests auto-save the JWT.** `Login Organizer` / `Login Attendee` store the token
  into `organizerToken` / `attendeeToken` via a test script — no copy-paste.
- **Create requests auto-save ids.** Create Venue → `venueId`; Create Event → `eventId` +
  `ticketTypeId`; Create Booking → `bookingId`. Later requests reference these variables.
- **Future dates are auto-filled.** A collection pre-request script sets `startsAt`/`endsAt`
  to ~7 days ahead so Create Event always passes `@Future` validation.
- Each request already carries the correct **Bearer token** for its role.

**Recommended run order (top to bottom):**
1. Auth → *Register Organizer* → *Login Organizer*
2. Venues → *Create Venue*
3. Events → *Create Event* → *Publish Event*
4. Auth → *Register Attendee* → *Login Attendee*
5. Bookings → *Create Booking* → *Pay Booking*

You can also use Postman's **Collection Runner** to fire that whole sequence in one click.

**Admin requests** need an ADMIN token. Since ADMIN can't self-register (by design), the **dev
profile auto-seeds** a default admin on startup — **`admin@x.com` / `password1`**
(see `config/DevDataInitializer.java`). Just run Auth → *Login Admin* (it already uses those
credentials) and the admin requests work. On the postgres profile no admin is auto-created;
provision one deliberately.

---

## 9.4 Files added for this

| File | What |
|---|---|
| `../../pom.xml` | Added `springdoc-openapi-starter-webmvc-ui`. |
| `config/OpenApiConfig.java` | API metadata + `bearerAuth` JWT scheme (the Authorize button). |
| `security/SecurityConfig.java` | Permits `/v3/api-docs/**` and `/swagger-ui/**` without auth. |
| `../openapi.json`, `../openapi.yaml` | Exported OpenAPI document. |
| `../booking-platform.postman_collection.json` | The Postman collection. |
