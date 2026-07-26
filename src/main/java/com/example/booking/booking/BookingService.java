package com.example.booking.booking;

import com.example.booking.booking.dto.CreateBookingRequest;
import com.example.booking.common.error.ConflictException;
import com.example.booking.common.error.NotFoundException;
import com.example.booking.config.BookingProperties;
import com.example.booking.event.Event;
import com.example.booking.event.EventRepository;
import com.example.booking.event.EventStatus;
import com.example.booking.event.TicketType;
import com.example.booking.event.TicketTypeRepository;
import com.example.booking.notification.NotificationService;
import com.example.booking.payment.PaymentGateway;
import com.example.booking.user.User;
import com.example.booking.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookings;
    private final EventRepository events;
    private final TicketTypeRepository ticketTypes;
    private final UserService userService;
    private final PaymentGateway paymentGateway;
    private final NotificationService notifications;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration holdWindow;

    public BookingService(BookingRepository bookings,
                          EventRepository events,
                          TicketTypeRepository ticketTypes,
                          UserService userService,
                          PaymentGateway paymentGateway,
                          NotificationService notifications,
                          ApplicationEventPublisher eventPublisher,
                          BookingProperties props) {
        this.bookings = bookings;
        this.events = events;
        this.ticketTypes = ticketTypes;
        this.userService = userService;
        this.paymentGateway = paymentGateway;
        this.notifications = notifications;
        this.eventPublisher = eventPublisher;
        this.holdWindow = Duration.ofMinutes(props.hold().windowMinutes());
    }

    /**
     * Create a PENDING booking that HOLDS seats. Availability is decremented on
     * TicketType under an OPTIMISTIC_FORCE_INCREMENT lock, so two attendees racing for
     * the last seats can't both succeed — the loser fails on commit.
     */
    @Transactional
    public Booking createBooking(CreateBookingRequest req, String attendeeEmail) {
        User attendee = userService.getByEmail(attendeeEmail);
        Event event = events.findWithDetailsById(req.eventId())
                .orElseThrow(() -> NotFoundException.of("Event", req.eventId()));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ConflictException("Event is not open for booking");
        }

        Booking booking = Booking.builder()
                .event(event)
                .attendee(attendee)
                .status(BookingStatus.PENDING)
                .expiresAt(Instant.now().plus(holdWindow))
                .currency(null)
                .build();

        long total = 0;
        for (CreateBookingRequest.Line line : req.items()) {
            TicketType tt = ticketTypes.findById(line.ticketTypeId())
                    .orElseThrow(() -> NotFoundException.of("TicketType", line.ticketTypeId()));
            if (!tt.getEvent().getId().equals(event.getId())) {
                throw new ConflictException(
                        "Ticket type " + tt.getId() + " does not belong to event " + event.getId());
            }
            if (booking.getCurrency() == null) {
                booking.setCurrency(tt.getCurrency());
            } else if (!booking.getCurrency().equals(tt.getCurrency())) {
                throw new ConflictException("All ticket types in a booking must share a currency");
            }
            if (tt.getQuantityAvailable() < line.quantity()) {
                throw new ConflictException("Not enough seats for ticket type '" + tt.getName()
                        + "': requested " + line.quantity() + ", available " + tt.getQuantityAvailable());
            }
            // Hold the seats.
            tt.setQuantityAvailable(tt.getQuantityAvailable() - line.quantity());

            booking.addItem(BookingItem.builder()
                    .ticketType(tt)
                    .quantity(line.quantity())
                    .unitPriceCents(tt.getPriceCents())
                    .build());
            total += (long) tt.getPriceCents() * line.quantity();
        }
        booking.setTotalCents(total);
        Booking saved = bookings.save(booking);
        log.info("Created booking {} holding {} seats-worth for {}",
                saved.getId(), saved.getItems().size(), attendeeEmail);
        return saved;
    }

    /**
     * Pay a PENDING booking. On success the booking is PAID and a {@link BookingPaidEvent}
     * is published — its listener runs only AFTER this transaction commits.
     */
    @Transactional
    public Booking pay(Long bookingId, String attendeeEmail) {
        Booking booking = requireOwnedBooking(bookingId, attendeeEmail);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Only PENDING bookings can be paid (was " + booking.getStatus() + ")");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Booking hold has expired");
        }

        var result = paymentGateway.charge(new PaymentGateway.PaymentRequest(
                booking.getId(), booking.getTotalCents(), booking.getCurrency(), attendeeEmail));
        if (!result.success()) {
            throw new ConflictException("Payment declined: " + result.message());
        }

        booking.setStatus(BookingStatus.PAID);
        booking.setPaymentReference(result.reference());
        booking.setExpiresAt(null);

        eventPublisher.publishEvent(new BookingPaidEvent(booking.getId()));
        return booking;
    }

    /** Attendee cancels an unpaid booking; held seats are released. */
    @Transactional
    public Booking cancel(Long bookingId, String attendeeEmail) {
        Booking booking = requireOwnedBooking(bookingId, attendeeEmail);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Only PENDING bookings can be cancelled (was " + booking.getStatus() + ")");
        }
        releaseSeats(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setExpiresAt(null);
        return booking;
    }

    @Transactional(readOnly = true)
    public Booking getOwned(Long bookingId, String attendeeEmail) {
        return requireOwnedBooking(bookingId, attendeeEmail);
    }

    @Transactional(readOnly = true)
    public List<Booking> listMine(String attendeeEmail) {
        User attendee = userService.getByEmail(attendeeEmail);
        return bookings.findByAttendeeId(attendee.getId());
    }

    /**
     * Sweep expired holds: release seats and mark EXPIRED. Called by the scheduler.
     * Returns the number of bookings expired.
     */
    @Transactional
    public int expireStaleHolds() {
        List<Booking> stale = bookings.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, Instant.now());
        for (Booking booking : stale) {
            releaseSeats(booking);
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setExpiresAt(null);
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} stale booking hold(s)", stale.size());
        }
        return stale.size();
    }

    /**
     * Remind attendees of PAID bookings whose event starts within {@code within}.
     * Called by the reminder scheduler. Returns the number of reminders sent.
     */
    @Transactional(readOnly = true)
    public int sendUpcomingReminders(Duration within) {
        Instant now = Instant.now();
        Instant cutoff = now.plus(within);
        int sent = 0;
        for (Booking booking : bookings.findByStatus(BookingStatus.PAID)) {
            Instant startsAt = booking.getEvent().getStartsAt();
            if (startsAt.isAfter(now) && startsAt.isBefore(cutoff)) {
                notifications.sendBookingReminder(booking.getAttendee().getEmail(), booking.getId());
                sent++;
            }
        }
        return sent;
    }

    private void releaseSeats(Booking booking) {
        for (BookingItem item : booking.getItems()) {
            TicketType tt = item.getTicketType();
            tt.setQuantityAvailable(tt.getQuantityAvailable() + item.getQuantity());
        }
    }

    private Booking requireOwnedBooking(Long bookingId, String attendeeEmail) {
        Booking booking = bookings.findWithItemsById(bookingId)
                .orElseThrow(() -> NotFoundException.of("Booking", bookingId));
        if (!booking.getAttendee().getEmail().equals(attendeeEmail)) {
            throw new AccessDeniedException("This booking belongs to another user");
        }
        return booking;
    }
}

/*
 * ============================================================================
 * FILE ROLE: THE HEART OF THE APP - booking creation, payment, cancellation,
 *            hold-expiry, and reminders.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - createBooking(): event must be PUBLISHED; for each line it loads the tier
 *     under an optimistic lock, checks availability, DECREMENTS it (holds seats),
 *     captures the price, sums the total, sets a 15-minute expiry, saves PENDING.
 *   - pay(): owner-only; must be PENDING and not expired; charges via the payment
 *     gateway; on success -> PAID and PUBLISHES a BookingPaidEvent.
 *   - cancel()/expireStaleHolds(): RELEASE held seats and set CANCELLED/EXPIRED.
 *   - sendUpcomingReminders(): notify attendees of soon-starting paid bookings.
 *
 * TECHNICAL CONCEPTS
 *   - @Transactional makes each operation atomic (all-or-nothing). Combined with
 *     the tier's OPTIMISTIC_FORCE_INCREMENT lock, this is what PREVENTS OVERSELLING
 *     under concurrency.
 *   - SEAT HOLD = decrement now, release later if unpaid; the expiresAt timestamp
 *     plus the scheduler implement the "expires if not paid" rule.
 *   - AFTER-COMMIT EVENT: ticket issuance is not done inline; publishing
 *     BookingPaidEvent lets a listener run only once the payment commits.
 *   - OWNERSHIP: requireOwnedBooking throws AccessDeniedException (403) if the
 *     booking belongs to someone else - a data-level check beyond role checks.
 *   - Payment is used via the PaymentGateway INTERFACE, so the real provider is a
 *     drop-in replacement.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is the MOST IMPORTANT file - the brain of buying tickets. Read it slowly.
 *
 * When you BOOK:
 *   1) it checks the event is actually on sale,
 *   2) it reserves ("holds") your seats by lowering the "available" count,
 *   3) it remembers the price right now,
 *   4) it adds up your total,
 *   5) it starts a 15-minute timer (pay before it runs out).
 *
 * When you PAY:
 *   - it checks the booking is really yours, is not already paid, and the timer
 *     has not run out,
 *   - it "charges" you (a pretend charge for now),
 *   - it marks the booking PAID,
 *   - and it quietly announces "this booking is paid" so tickets get issued AFTER
 *     everything is safely saved.
 *
 * If you CANCEL, or the timer runs out, the held seats are given back so someone
 * else can buy them.
 *
 * The whole thing is careful so two people can never buy the very same seat.
 * ============================================================================
 */
