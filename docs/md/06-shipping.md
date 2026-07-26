# 6. Building, Running & Shipping

Everything you need to build, run, verify, and package the app. The scope boundary from the
brief was respected: **no app Dockerfile, no cloud deploy, no prod hardening beyond actuator
+ profiles.** This doc covers what exists today and what a real deployment would add.

---

## 6.1 Prerequisites

- **JDK** — the build targets **Java 21 bytecode**. It builds fine on the JDK **25** installed
  here thanks to the Lombok pin (see doc 1). A real JDK 21 works too and is the "clean" setup.
- **Maven 3.9+** (present).
- **Docker** — *only* for the Postgres profile and integration tests. Not required for the
  default build or dev run.

---

## 6.2 Build the project (the gate)

```bash
mvn clean install
```
This **compiles**, runs the **unit tests** (Surefire), **packages** the executable jar, and
installs it to your local `~/.m2`. Integration tests are skipped (no Docker needed). Expected
tail:
```
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
Building jar: target/booking-platform-0.0.1-SNAPSHOT.jar
BUILD SUCCESS
```

Artifact: **`../../target/booking-platform-0.0.1-SNAPSHOT.jar`** — a self-contained Spring Boot fat
jar (~62 MB, all dependencies nested). A jar *is* a zip; `unzip -l target/*.jar` lists its contents.

Faster variants:
```bash
mvn package            # build + unit tests + jar, without installing to ~/.m2
mvn -o package         # offline, once deps are cached
mvn clean package -DskipTests   # jar only (skips tests) — not the gate, just for a quick artifact
```

---

## 6.3 Run it

### Dev profile (default) — zero infrastructure
```bash
mvn spring-boot:run
# or, from the packaged jar:
java -jar target/booking-platform-0.0.1-SNAPSHOT.jar
```
Boots on **H2 in-memory** (schema auto-created by Hibernate). Probe:
```bash
curl localhost:8080/api/ping        # {"status":"ok","time":"…"}
curl localhost:8080/actuator/health # {"status":"UP"}
```
H2 console at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:booking`, user `sa`).

### Postgres profile — Flyway-managed schema
```bash
docker compose up -d          # local Postgres on :5432 (booking/booking)
SPRING_PROFILES_ACTIVE=postgres java -jar target/booking-platform-0.0.1-SNAPSHOT.jar
```
On start, Flyway applies `V1`/`V2`, then Hibernate **validates** the entities against the
schema (start fails loudly on drift). Override the datasource with env vars if needed:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db \
SPRING_DATASOURCE_USERNAME=user SPRING_DATASOURCE_PASSWORD=secret \
SPRING_PROFILES_ACTIVE=postgres java -jar target/*.jar
```

---

## 6.4 Run the integration tests (needs Docker)

They're excluded from the default build on purpose (Testcontainers needs a Docker daemon):
```bash
mvn verify -DskipITs=false
```
`BookingFlowIT` starts a `postgres:16-alpine` container, wires the datasource dynamically,
applies Flyway, and drives register → venue → event → publish → book → pay over real HTTP.
Run this on any machine/CI runner that has Docker.

---

## 6.5 Packaging a distributable `.zip` (optional)

The gate produces a jar. If you want a **zip bundle** (jar + README + docs) to hand off:

```bash
# quick, no build change:
mvn clean package
mkdir -p dist/booking-platform
cp target/booking-platform-0.0.1-SNAPSHOT.jar dist/booking-platform/
cp README.md dist/booking-platform/
cp -r docs dist/booking-platform/
( cd dist && zip -r booking-platform.zip booking-platform )
# → dist/booking-platform.zip
```

If you'd rather the **build** produce the zip automatically, I can add a
`maven-assembly-plugin` (or `maven-antrun`) execution bound to the `package` phase with a
small assembly descriptor. Say the word and I'll wire it in and re-verify `mvn install`.

---

## 6.6 Configuration reference

All tunables live under `booking.*` (bound by `BookingProperties`) and standard Spring keys.
Override via env vars or `-D` flags — **never commit real secrets**.

| Key | Default | Meaning |
|---|---|---|
| `booking.jwt.secret` | dev placeholder | HS256 signing secret. **Must** be overridden in any real env; ≥ 32 bytes. |
| `booking.jwt.expiration-minutes` | 120 | Token lifetime. |
| `booking.hold.window-minutes` | 15 | How long a booking holds seats before it can expire. |
| `booking.hold.sweep-interval-ms` | 60000 | How often the expiry job runs. |
| `booking.reminder.interval-ms` | 300000 | How often the reminder job runs. |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (H2) or `postgres`. |
| `SPRING_DATASOURCE_*` | see profile | Postgres URL/user/password. |

Example, production-shaped run:
```bash
BOOKING_JWT_SECRET='<a long random 256-bit secret>' \
SPRING_PROFILES_ACTIVE=postgres \
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/booking \
SPRING_DATASOURCE_USERNAME=booking SPRING_DATASOURCE_PASSWORD='<secret>' \
java -jar booking-platform-0.0.1-SNAPSHOT.jar
```
(Spring relaxed binding maps `BOOKING_JWT_SECRET` → `booking.jwt.secret`.)

---

## 6.7 A minimal CI recipe

```yaml
# e.g. GitHub Actions — the essence
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: '21' }
- run: mvn -B clean install          # compile + unit tests + jar (no Docker)
- run: mvn -B verify -DskipITs=false # integration tests (runner has Docker)
```

---

## 6.8 What "ship it for real" would add (deliberately out of current scope)

The brief drew the line at a working, installable build. To productionise, the next steps
would be — roughly in order:

1. **Secrets & config** — externalise `booking.jwt.secret` and DB creds into a secret manager;
   fail fast if the JWT secret is the dev placeholder.
2. **App container** — a Dockerfile (or Spring Boot's `bootBuildImage`) producing an OCI image;
   wire the existing `../../docker-compose.yml` to run app + db together for local prod-like runs.
3. **DB ops** — run Flyway as a gated migration step in the pipeline; add a rollback/backup story.
4. **Security hardening** — refresh tokens / shorter access-token TTLs, rate-limiting on
   `/api/auth/*`, CORS policy, HTTPS termination, lock down actuator behind auth/network.
5. **Real integrations** — swap `FakePaymentGateway` for a real provider behind the same
   `PaymentGateway` interface; swap `NotificationService` for email/SMS.
6. **Observability** — ship metrics to a backend, structured logging, tracing, alerts on the
   hold-expiry/reminder jobs.
7. **Resilience** — retry policy around optimistic-lock failures on booking creation; make the
   scheduled jobs safe under multiple instances (shed-lock or a single leader).

None of these are needed to build, run, or evaluate the app today — they're the roadmap past
the scope boundary.
