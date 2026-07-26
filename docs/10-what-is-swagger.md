# 10. What Is Swagger / OpenAPI — and How Is It Generated Here?

A plain-language explainer of what Swagger is, whether the dependency creates it "directly"
(yes), how that generation actually works, and why it's useful.

---

## 10.1 First, the short answer to your question

**Yes.** The Swagger documentation is created **directly and automatically by the dependency**
I added (`springdoc-openapi`). I did **not** write the API document by hand. When the app runs,
the library **reads your code** and **generates** the document live. If you change a controller,
the docs change on the next restart — they can't fall out of date.

The only things I wrote by hand were tiny *hints* to make the output nicer:
- `config/OpenApiConfig.java` — the title/description and the "Authorize with JWT" button.
- Two permit rules in `security/SecurityConfig.java` so the docs pages open without a login.

Everything else — every endpoint, parameter, request body, response shape — is discovered
automatically from the existing controllers and DTOs.

---

## 10.2 What is "Swagger" vs "OpenAPI"? (they get used interchangeably)

- **OpenAPI** is a **standard format** — a language-agnostic way to describe a REST API in a
  single structured file (JSON or YAML): what endpoints exist, what each accepts, what it
  returns, what the errors look like, how you authenticate. Think of it as a **contract** or a
  **menu** for your API that both humans and machines can read. The current version is
  *OpenAPI 3*.

- **Swagger** is the **older name** for that same thing, and today refers to a **family of
  tools** built around it — most famously **Swagger UI**, the web page that turns an OpenAPI
  document into an interactive, clickable API explorer.

So: **OpenAPI = the document/standard. Swagger UI = a tool that renders that document.**
People say "the Swagger docs" to mean "the OpenAPI document + the Swagger UI page."

A tiny slice of what an OpenAPI document looks like (this is generated for us):

```yaml
paths:
  /api/auth/login:
    post:
      requestBody:
        content:
          application/json:
            schema: { $ref: '#/components/schemas/LoginRequest' }
      responses:
        '200':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/TokenResponse' }
```

You never write that — the library produces it from your `AuthController.login(...)` method
and the `LoginRequest` / `TokenResponse` records.

---

## 10.3 How is it generated in THIS project? (step by step)

1. **You add one dependency** — `springdoc-openapi-starter-webmvc-ui` in `pom.xml`. That's the
   whole install.

2. **At startup, springdoc scans the application.** It asks Spring for every
   `@RestController` and inspects each `@GetMapping`/`@PostMapping` method using **reflection**
   (Java's ability to look at its own classes at runtime). For each method it reads:
   - the **URL and HTTP verb** (from the mapping annotation),
   - the **path/query params** and the **request body** type,
   - the **response** type,
   - **validation** annotations (`@NotBlank`, `@Min`…) → these become documented constraints.

3. **It turns your DTO records into "schemas."** `RegisterRequest`, `EventResponse`, etc.
   become JSON schemas describing each field and type — the `components/schemas` section.

4. **It merges your hints.** The title, description, and the `bearerAuth` JWT scheme from
   `config/OpenApiConfig.java` are folded in.

5. **It publishes the result at two live URLs** (no file on disk needed):
   - `GET /v3/api-docs` → the OpenAPI document as **JSON**
   - `GET /v3/api-docs.yaml` → the same as **YAML**

6. **Swagger UI reads that document and draws the page** at `/swagger-ui.html` — the
   interactive explorer with "Try it out" buttons.

```
 Your code (controllers + DTOs + validation)
        │  springdoc scans it at startup (reflection)
        ▼
 OpenAPI document  ── served at /v3/api-docs(.yaml)
        │  Swagger UI renders it
        ▼
 Interactive docs page  ── /swagger-ui.html
```

> **"Directly" means runtime, not build time.** The document isn't baked into the jar as a
> file; it's produced in memory each time the app boots and served over HTTP. (In §9.2 I also
> *exported* a snapshot to `docs/openapi.json` / `.yaml` so you have a file copy, but that's an
> optional convenience — the source of truth is the running app.)

---

## 10.4 Where each generated piece comes from (trace it yourself)

| In the Swagger page you'll see… | …which is generated from |
|---|---|
| The endpoint `POST /api/bookings` | `booking/BookingController.java` `@PostMapping` |
| Its request body fields + rules | `booking/dto/CreateBookingRequest.java` (record + `@NotNull/@Min`) |
| Its response shape | `booking/dto/BookingResponse.java` |
| The lock icon / "Authorize" button | `config/OpenApiConfig.java` (`bearerAuth` scheme) |
| Title "Booking Platform API" | `config/OpenApiConfig.java` (`@OpenAPIDefinition`) |
| Why the docs page loads without login | `security/SecurityConfig.java` (permit `/v3/api-docs/**`, `/swagger-ui/**`) |

Nothing in that table is a document I typed — it's your existing code, read by the library.

---

## 10.5 What is it useful for? (why bother)

- **Always-accurate documentation.** Because it's generated from the code, it can't silently
  drift from reality the way a hand-written wiki page does. Change the code → docs update.

- **A try-it-out console.** Swagger UI lets you (or QA, or frontend devs) call endpoints from
  the browser — fill the body, hit Execute, see the response — without Postman or curl.

- **Frontend/mobile teams work in parallel.** They read the OpenAPI document to know exactly
  what to send and expect, before the backend is even finished.

- **Client code generation.** Tools (OpenAPI Generator, etc.) can read `openapi.json` and
  auto-produce a typed client library in TypeScript, Java, Python, and more — so callers don't
  hand-write HTTP code.

- **Import into tools.** Postman, Insomnia, and API gateways can import the OpenAPI file to
  instantly build request collections or configure routing/validation.

- **Contract & testing.** The document acts as the API contract; contract-testing tools can
  verify the running app still matches it.

- **Onboarding.** A new engineer opens `/swagger-ui.html` and sees the entire API at a glance —
  every endpoint, role, and payload — in one place.

---

## 10.6 A note on safety (production)

Swagger UI and the raw API document are handy in development but expose your full API surface.
In a real deployment you'd typically **restrict or disable** them in production (e.g. lock the
paths behind auth or a network rule, or turn springdoc off with a profile flag). In this
project they're open because the scope is a local, evaluable build — see the "ship it for real"
list in `06-shipping.md`.

---

## 10.7 TL;DR

- **Swagger/OpenAPI** = a standard, machine-readable description of your REST API, plus tools
  (Swagger UI) that make it interactive.
- **It's generated automatically** by the `springdoc-openapi` dependency, by scanning your
  controllers and DTOs at startup — I only added small metadata hints.
- **It's served live** at `/v3/api-docs` (document) and `/swagger-ui.html` (interactive page);
  I also exported a file copy into `docs/`.
- **It's useful** for accurate docs, a try-it console, parallel frontend work, client
  code-gen, tool imports, and fast onboarding.
