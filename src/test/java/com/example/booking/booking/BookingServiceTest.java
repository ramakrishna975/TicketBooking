package com.example.booking.booking;

import com.example.booking.booking.dto.CreateBookingRequest;
import com.example.booking.common.error.ConflictException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookings;
    @Mock EventRepository events;
    @Mock TicketTypeRepository ticketTypes;
    @Mock UserService userService;
    @Mock PaymentGateway paymentGateway;
    @Mock NotificationService notifications;
    @Mock ApplicationEventPublisher eventPublisher;

    BookingService service;

    User attendee;
    Event event;
    TicketType general;

    @BeforeEach
    void setUp() {
        BookingProperties props = new BookingProperties(
                new BookingProperties.Jwt("secret", 120),
                new BookingProperties.Hold(15, 60_000),
                new BookingProperties.Reminder(300_000));
        service = new BookingService(bookings, events, ticketTypes, userService,
                paymentGateway, notifications, eventPublisher, props);

        attendee = User.builder().id(1L).email("a@x.com").build();

        event = Event.builder().id(10L).status(EventStatus.PUBLISHED).build();
        general = TicketType.builder()
                .id(100L).event(event).name("General")
                .priceCents(5_000).currency("USD")
                .quantityTotal(10).quantityAvailable(10)
                .build();
    }

    @Test
    void createBooking_holdsSeats_andComputesTotal() {
        when(userService.getByEmail("a@x.com")).thenReturn(attendee);
        when(events.findWithDetailsById(10L)).thenReturn(Optional.of(event));
        when(ticketTypes.findById(100L)).thenReturn(Optional.of(general));
        when(bookings.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new CreateBookingRequest(10L, List.of(new CreateBookingRequest.Line(100L, 2)));
        Booking booking = service.createBooking(req, "a@x.com");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalCents()).isEqualTo(10_000);
        assertThat(booking.getCurrency()).isEqualTo("USD");
        assertThat(booking.getExpiresAt()).isAfter(Instant.now());
        // Two seats are now held.
        assertThat(general.getQuantityAvailable()).isEqualTo(8);
    }

    @Test
    void createBooking_soldOut_throwsConflict() {
        general.setQuantityAvailable(1);
        when(userService.getByEmail("a@x.com")).thenReturn(attendee);
        when(events.findWithDetailsById(10L)).thenReturn(Optional.of(event));
        when(ticketTypes.findById(100L)).thenReturn(Optional.of(general));

        var req = new CreateBookingRequest(10L, List.of(new CreateBookingRequest.Line(100L, 2)));
        assertThatThrownBy(() -> service.createBooking(req, "a@x.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Not enough seats");
        // No seats consumed on failure.
        assertThat(general.getQuantityAvailable()).isEqualTo(1);
    }

    @Test
    void createBooking_onUnpublishedEvent_throwsConflict() {
        event.setStatus(EventStatus.DRAFT);
        when(userService.getByEmail("a@x.com")).thenReturn(attendee);
        when(events.findWithDetailsById(10L)).thenReturn(Optional.of(event));

        var req = new CreateBookingRequest(10L, List.of(new CreateBookingRequest.Line(100L, 1)));
        assertThatThrownBy(() -> service.createBooking(req, "a@x.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not open for booking");
    }

    @Test
    void pay_success_marksPaid_andPublishesEvent() {
        Booking booking = pendingBooking(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(bookings.findWithItemsById(5L)).thenReturn(Optional.of(booking));
        when(paymentGateway.charge(any()))
                .thenReturn(PaymentGateway.PaymentResult.ok("REF-1"));

        Booking paid = service.pay(5L, "a@x.com");

        assertThat(paid.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(paid.getPaymentReference()).isEqualTo("REF-1");
        assertThat(paid.getExpiresAt()).isNull();
        verify(eventPublisher).publishEvent(new BookingPaidEvent(5L));
    }

    @Test
    void pay_expiredHold_throwsConflict_andDoesNotCharge() {
        Booking booking = pendingBooking(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(bookings.findWithItemsById(5L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.pay(5L, "a@x.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expired");
        verify(paymentGateway, never()).charge(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cancel_releasesHeldSeats() {
        general.setQuantityAvailable(8); // 2 currently held
        Booking booking = pendingBooking(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(bookings.findWithItemsById(5L)).thenReturn(Optional.of(booking));

        Booking cancelled = service.cancel(5L, "a@x.com");

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(general.getQuantityAvailable()).isEqualTo(10); // seats returned
    }

    private Booking pendingBooking(Instant expiresAt) {
        Booking booking = Booking.builder()
                .id(5L).event(event).attendee(attendee)
                .status(BookingStatus.PENDING)
                .currency("USD").totalCents(10_000)
                .expiresAt(expiresAt)
                .build();
        booking.addItem(BookingItem.builder()
                .ticketType(general).quantity(2).unitPriceCents(5_000).build());
        return booking;
    }
}
