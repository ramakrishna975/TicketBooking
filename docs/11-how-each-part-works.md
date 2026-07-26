# 11. How Each Part of the App Works (for everyone)

This walks through the app **one feature at a time**, in the order they matter to a real user.
Each part is explained twice: first **In plain words** (imagine explaining to a customer), then
**Under the hood** (the technical detail), and finally **Files** (where it lives in the code).

The features: **User → Security → Venue → Event → Booking → Payment → Notification → Admin.**

---

## 1. User — accounts and who you are

**In plain words.** Before anyone can do anything meaningful, they create an account with an
email and password, and they pick what kind of user they are: a fan (**Attendee**), a promoter
(**Organizer**), or a staff moderator (**Admin**). Signing up as an Admin is not allowed from
the public form — admins are set up privately by the business.

**Under the hood.**
- A user is stored as a database row with: email (must be unique), a **hashed** password (never
  the real password), a display name, a role, an `enabled` flag, and a created-at timestamp.
- Registration rejects two things: signing up as `ADMIN`, and using an email that already
  exists — both return a clear "409 Conflict" error.
- The password is scrambled with **BCrypt** before saving, so even if the database leaked, the
  real passwords aren't in it.

**Files.**
- `user/User.java` — the account (database entity).
- `user/Role.java` — the three roles.
- `user/UserService.java` — the sign-up rules (block admin, block duplicates, hash password).
- `user/AuthController.java` — the `/api/auth/register` and `/api/auth/login` endpoints.
- `user/UserController.java` — `/api/users/me` (see your own profile).
- `user/dto/…` — the exact shapes of data going in/out (register, login, responses).

---

## 2. Security — logging in and staying logged in

**In plain words.** When you log in, the system checks your password and hands you a **digital
wristband** (a token). For everything you do afterward, you show that wristband instead of
typing your password again. The wristband also says which role you have, so the system knows
what you're allowed to do. Some pages are public (browsing events), some need a wristband, and
some need a specific role.

**Under the hood.**
- Login verifies the email + password against the stored BCrypt hash. On success the system
  issues a **JWT** — a small, cryptographically **signed** token containing your email and role.
  Because it's signed, it can't be forged or edited.
- The system is **stateless**: it doesn't keep a server-side session. On each request a **filter**
  reads the `Authorization: Bearer <token>` header, verifies the signature, and establishes who
  you are for that one request.
- Access is guarded on two levels:
  - **URL rules** — broad ("only Admin may touch `/api/admin/**`").
  - **Method rules** — precise, sitting right on the action (`@PreAuthorize` — e.g. only an
    Organizer or Admin may create an event).
  - Plus **ownership** checks (is this *your* booking?) enforced in the service code.
- If you're not logged in you get **401**; if you're logged in but lack the role/ownership you
  get **403** — both as clean, consistent JSON.

**Files.**
- `security/JwtService.java` — creates and validates the token.
- `security/JwtAuthenticationFilter.java` — reads the token on every request.
- `security/AppUserDetailsService.java` — looks up the user for password login.
- `security/SecurityConfig.java` — the URL rules, turns on method rules, password encoder.
- `security/SecurityErrorHandlers.java` — turns 401/403 into standard error JSON.

---

## 3. Venue — the place an event happens

**In plain words.** A venue is simply a location — a hall, a stadium — with an address and a
**capacity** (how many people fit). Organizers set these up so they can attach events to them.
Anyone can browse venues; only organizers/admins can create them.

**Under the hood.**
- A venue row holds name, address, city, and capacity.
- Capacity matters later: when an organizer creates an event, the total number of tickets can't
  exceed the venue's capacity (the system enforces this).
- Reading venues is public; creating one is guarded by role (`ORGANIZER`/`ADMIN`).

**Files.**
- `venue/Venue.java` — the venue (database entity).
- `venue/VenueService.java` — create / list / fetch, with "not found" → 404.
- `venue/VenueController.java` — public `GET`, role-guarded `POST`.
- `venue/dto/…` — request/response shapes.

---

## 4. Event — the thing people buy tickets for

**In plain words.** An organizer creates an **event** (a concert, a match) at a venue, with a
start and end time, and one or more **ticket types** — tiers like *General* and *VIP*, each with
its own price and how many exist. A new event starts as a private **draft**. When the organizer
is ready, they **publish** it, and only then can the public see it and buy. They can also
**cancel** it. Organizers can only touch their own events.

**Under the hood.**
- An event links to a venue and to its organizer, and owns a list of ticket types (each tier
  tracks `quantityTotal` and the live `quantityAvailable` — this is the seat count).
- Creation validates that the end time is after the start, and that the total tickets fit the
  venue capacity — otherwise a 409.
- The public "list events" page is **cached** for speed (it's read constantly and changes
  rarely); whenever an organizer creates/publishes/cancels, the cache is cleared so nobody sees
  stale data.
- Publishing/cancelling is **ownership-checked** — you can't publish someone else's event.

**Files.**
- `event/Event.java`, `event/EventStatus.java` — the event and its draft/published/cancelled states.
- `event/TicketType.java` — a priced tier that holds the seat count.
- `event/EventService.java` — create/publish/cancel rules + the cached listing.
- `event/EventController.java` — public browse; role-guarded create/publish/cancel.
- `event/dto/…` — request/response shapes.

---

## 5. Booking — reserving and holding seats

**In plain words.** This is the heart of the app. When an attendee books, the seats are
**immediately held for them** — like putting items in a cart that's reserved for a short time
(15 minutes). The booking is "pending" until they pay. If two people try to grab the last seats
at the exact same moment, the system makes sure **only one succeeds** — it never sells the same
seat twice. If the attendee doesn't pay in time, the hold is automatically released and the
seats go back on sale.

**Under the hood.**
- Booking an event that isn't **published** is refused (409). For each requested tier the system
  checks availability, then **decrements `quantityAvailable`** — the seats are now held — and
  records the price at that moment. The booking gets an `expiresAt` 15 minutes out and status
  `PENDING`.
- The "no double-selling" guarantee uses **optimistic locking**: each ticket tier carries a
  hidden **version number** (`@Version`). Two simultaneous bookings both try to update it, but
  the database only lets one win; the loser fails safely and no seat is lost or oversold.
- The whole booking is one **transaction** (all-or-nothing), so a half-finished booking can
  never be left behind.
- The booking itself also has a version number, so the "pay it" and "expire it" actions can't
  collide and corrupt its state.

**Files.**
- `booking/Booking.java`, `booking/BookingItem.java`, `booking/BookingStatus.java` — the booking,
  its line items, and its lifecycle (pending/paid/expired/cancelled).
- `booking/BookingService.java` — the core logic: create (hold seats), pay, cancel, expire.
- `booking/BookingController.java` — the attendee endpoints.
- `event/TicketTypeRepository.java` — the special "lock the tier while holding seats" read.

---

## 6. Payment — charging for the booking (faked, but realistic)

**In plain words.** When the attendee pays, the booking becomes **paid** and their tickets are
issued. There's **no real bank** connected in this build — payment is **simulated** — but it's
built so a real payment provider (Stripe, Razorpay, etc.) can be dropped in later **without
changing any of the booking logic**.

**Under the hood.**
- Payment sits behind an **interface** (a contract that says "charge this amount, tell me
  success/failure"). The current implementation is a **fake** that approves any valid amount and
  returns a reference number; it declines a zero/negative amount so the failure path stays tested.
- Paying checks the booking is still `PENDING` and **not expired** — if the hold lapsed, payment
  isn't even attempted (409). On success the booking becomes `PAID`, stores the payment
  reference, and clears its expiry.
- Swapping in a real gateway means writing one new class that implements the same interface;
  `BookingService` never knows the difference. This is the "program to an interface" principle.

**Files.**
- `payment/PaymentGateway.java` — the contract (interface).
- `payment/FakePaymentGateway.java` — the simulated implementation.
- `booking/BookingService.java` — the `pay(...)` method that uses the gateway.

---

## 7. Notification — telling people what happened

**In plain words.** After a successful payment, the customer should get their tickets and a
confirmation; before an event, they get a reminder. In this build those messages are **simulated**
(written to the app's log) rather than emailed/texted — but again, wired so real email/SMS can be
added later. Crucially, tickets are only issued **after the payment is truly finalized**, never
before.

**Under the hood.**
- Instead of issuing tickets inline during payment, paying **publishes an internal "BookingPaid"
  event**. A listener handles it, but it's configured to run **only after the payment transaction
  has fully committed** (`AFTER_COMMIT`). This guarantees you can never issue tickets for a
  payment that later fails to save, or accidentally undo a captured payment.
- Reminders are sent by a **scheduled background job** that periodically finds paid bookings for
  soon-to-start events.
- The notification service itself is a simple component that (for now) logs the action — the one
  place you'd later plug in an email/SMS provider.

**Files.**
- `booking/BookingPaidEvent.java` — the internal "it's paid" signal.
- `booking/BookingPaidListener.java` — issues tickets, but only after commit.
- `booking/BookingScheduler.java` — the timed jobs (expire holds, send reminders).
- `notification/NotificationService.java` — where tickets/reminders are "sent" (logged today).

---

## 8. Admin — moderation

**In plain words.** Admins are trusted staff. They can list users, **disable or re-enable** an
account (e.g. a bad actor), and **force-cancel** any event regardless of who created it. These
powers are locked to the Admin role.

**Under the hood.**
- Every admin endpoint is guarded both by a URL rule (`/api/admin/**` needs Admin) and a method
  rule (`hasRole('ADMIN')`) — belt and braces.
- Disabling a user flips their `enabled` flag so they can no longer authenticate; force-cancel
  sets an event to `CANCELLED` and clears the public listing cache.

**Files.**
- `admin/AdminController.java` — the admin-only endpoints.
- `user/UserService.java` — enable/disable logic.
- `event/EventService.java` — the admin force-cancel.

---

## How it all connects (one sentence per step)

1. **User** signs up and **Security** gives them a token.
2. An organizer sets up a **Venue** and an **Event** with ticket tiers, then publishes it.
3. An attendee makes a **Booking**, which **holds seats** safely.
4. **Payment** turns the pending booking into a paid one.
5. **Notification** issues the tickets — only after the payment is committed.
6. **Admin** watches over users and events.

If you want the same story with clickable, code-level detail per concept, see
`08-learning-guide.md`; for the request-by-request API view, see `04-request-flows.md`.
