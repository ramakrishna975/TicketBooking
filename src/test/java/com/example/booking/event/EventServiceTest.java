package com.example.booking.event;

import com.example.booking.common.error.ConflictException;
import com.example.booking.event.dto.CreateEventRequest;
import com.example.booking.event.dto.TicketTypeRequest;
import com.example.booking.user.User;
import com.example.booking.user.UserService;
import com.example.booking.venue.Venue;
import com.example.booking.venue.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository events;
    @Mock VenueService venueService;
    @Mock UserService userService;

    EventService service;
    Venue venue;
    User organizer;

    @BeforeEach
    void setUp() {
        service = new EventService(events, venueService, userService);
        venue = Venue.builder().id(1L).name("Hall").capacity(100).build();
        organizer = User.builder().id(2L).email("org@x.com").build();
        lenient().when(userService.getByEmail("org@x.com")).thenReturn(organizer);
        lenient().when(venueService.getById(1L)).thenReturn(venue);
        lenient().when(events.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_buildsDraftEvent_withTicketTypes() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        var req = new CreateEventRequest("Show", "desc", 1L, start, start.plus(2, ChronoUnit.HOURS),
                List.of(new TicketTypeRequest("General", 5_000, "USD", 50)));

        Event event = service.create(req, "org@x.com");

        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getTicketTypes()).hasSize(1);
        assertThat(event.getTicketTypes().get(0).getQuantityAvailable()).isEqualTo(50);
        assertThat(event.getOrganizer()).isSameAs(organizer);
    }

    @Test
    void create_endBeforeStart_throwsConflict() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        var req = new CreateEventRequest("Show", "desc", 1L, start, start.minus(1, ChronoUnit.HOURS),
                List.of(new TicketTypeRequest("General", 5_000, "USD", 10)));

        assertThatThrownBy(() -> service.create(req, "org@x.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("endsAt must be after startsAt");
    }

    @Test
    void create_exceedingVenueCapacity_throwsConflict() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        var req = new CreateEventRequest("Show", "desc", 1L, start, start.plus(2, ChronoUnit.HOURS),
                List.of(new TicketTypeRequest("General", 5_000, "USD", 150))); // > capacity 100

        assertThatThrownBy(() -> service.create(req, "org@x.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("exceeds venue capacity");
    }
}
