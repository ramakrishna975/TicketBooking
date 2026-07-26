# Booking Platform — Documentation

This folder is the guided tour of the project. Read it before diving into the code.

## Reading order

| # | Doc | What it covers |
|---|-----|----------------|
| 1 | [01-decisions-and-permissions.md](01-decisions-and-permissions.md) | Every decision I made, the questions/permissions I asked you and how they were resolved, and the environment quirks (Docker, Java 25 vs 21, Lombok) |
| 2 | [02-architecture.md](02-architecture.md) | The whole system explained: layers, package-by-feature layout, the domain model, security model, and how a request flows end-to-end |
| 3 | [03-file-guide.md](03-file-guide.md) | Every file, grouped by feature, with one-line-plus explanations of what it does and why it exists |
| 4 | [04-request-flows.md](04-request-flows.md) | Concrete walkthroughs: register → login → create event → book → pay, plus the background jobs and events |
| 5 | [05-api-reference.md](05-api-reference.md) | Endpoint-by-endpoint reference with roles, request/response shapes, and status codes |
| 6 | [06-shipping.md](06-shipping.md) | How to build, run, test, and ship the app — dev, Postgres, and the packaged jar |
| 7 | [07-glossary-and-faq.md](07-glossary-and-faq.md) | Terms, "why did you do X", and known gaps / next steps |
| 8 | [08-learning-guide.md](08-learning-guide.md) | **Start here to learn the project.** Every technical concept explained in plain language, each with the exact file to open and see it in real code |
| 9 | [09-api-docs-and-postman.md](09-api-docs-and-postman.md) | Swagger UI, the exported OpenAPI document (`openapi.json`/`.yaml`), and the ready-to-run Postman collection |
| 10 | [10-what-is-swagger.md](10-what-is-swagger.md) | **What Swagger/OpenAPI is**, how the dependency auto-generates it from the code, and what it's used for |
| 11 | [11-how-each-part-works.md](11-how-each-part-works.md) | **Feature-by-feature walkthrough** (user, security, venue, event, booking, payment, notification, admin) — client-friendly "plain words" + "under the hood" technical detail + files |

## Plain-text docs (in this same folder)

These are `.txt` versions for quick reading / sharing with non-technical folks:

| File | What it covers |
|------|----------------|
| [SETUP-GUIDE.txt](SETUP-GUIDE.txt) | Step-by-step: set up and run the app in IntelliJ (start here to run it) |
| [PROJECT-DOCUMENTATION.txt](PROJECT-DOCUMENTATION.txt) | The complete single-file document — everything, including config/DB line-by-line |
| [OVERVIEW.txt](OVERVIEW.txt) | Short summary of what the project contains |
| [HOW-IT-WORKS.txt](HOW-IT-WORKS.txt) | Each feature in plain words + technical detail |

## 30-second summary

An **event/ticket booking platform** built with **Spring Boot 3.4 (Java 21)**, organised
**package-by-feature**. Users register and log in with **JWT**; organizers create events at
venues with priced ticket types; attendees create bookings that **hold seats** and **expire
if unpaid**; payment is **stubbed** behind an interface. Errors are **RFC 7807 ProblemDetail**.
There is **Caffeine caching** on the public event listing, **scheduled** hold-expiry and
reminders, **after-commit domain events** for ticket issuance, and **optimistic locking** on
the seat/booking aggregate.

**Build status:** `mvn install` → `BUILD SUCCESS`, 16 unit tests green, executable jar
packaged. Integration tests (Testcontainers Postgres) are wired but skipped by default
because this machine has no Docker daemon — see doc 1.

## A note on code comments

The source already carries explanatory Javadoc/inline comments on every non-trivial class
and method (the *why*, not just the *what*). Doc 3 tells you which file to open for which
concern; the comments inside carry the fine detail. I deliberately did **not** duplicate
those comments here — this folder is the map, the code is the territory.
