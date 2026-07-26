package com.example.booking.event;

import com.example.booking.common.error.ConflictException;
import com.example.booking.common.error.NotFoundException;
import com.example.booking.config.CacheConfig;
import com.example.booking.event.dto.CreateEventRequest;
import com.example.booking.event.dto.EventResponse;
import com.example.booking.event.dto.TicketTypeRequest;
import com.example.booking.user.User;
import com.example.booking.user.UserService;
import com.example.booking.venue.Venue;
import com.example.booking.venue.VenueService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private final EventRepository events;
    private final VenueService venueService;
    private final UserService userService;

    public EventService(EventRepository events, VenueService venueService, UserService userService) {
        this.events = events;
        this.venueService = venueService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Event> listPublished() {
        return events.findByStatus(EventStatus.PUBLISHED);
    }

    /**
     * Cached public listing. Returns fully-mapped DTOs (not entities) so nothing lazy
     * is read off a detached cached object. Evicted whenever the published set can change.
     */
    @Cacheable(CacheConfig.PUBLISHED_EVENTS)
    @Transactional(readOnly = true)
    public List<EventResponse> listPublishedResponses() {
        return listPublished().stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Event getDetails(Long id) {
        return events.findWithDetailsById(id).orElseThrow(() -> NotFoundException.of("Event", id));
    }

    @Transactional(readOnly = true)
    public List<Event> listByOrganizer(String organizerEmail) {
        User organizer = userService.getByEmail(organizerEmail);
        return events.findByOrganizerId(organizer.getId());
    }

    @CacheEvict(value = CacheConfig.PUBLISHED_EVENTS, allEntries = true)
    @Transactional
    public Event create(CreateEventRequest req, String organizerEmail) {
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw new ConflictException("Event endsAt must be after startsAt");
        }
        User organizer = userService.getByEmail(organizerEmail);
        Venue venue = venueService.getById(req.venueId());

        Event event = Event.builder()
                .title(req.title())
                .description(req.description())
                .venue(venue)
                .organizer(organizer)
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .status(EventStatus.DRAFT)
                .build();

        int requested = 0;
        for (TicketTypeRequest ttr : req.ticketTypes()) {
            requested += ttr.quantityTotal();
            event.addTicketType(TicketType.builder()
                    .name(ttr.name())
                    .priceCents(ttr.priceCents())
                    .currency(ttr.currency())
                    .quantityTotal(ttr.quantityTotal())
                    .quantityAvailable(ttr.quantityTotal())
                    .build());
        }
        if (requested > venue.getCapacity()) {
            throw new ConflictException(
                    "Total ticket quantity (" + requested + ") exceeds venue capacity (" + venue.getCapacity() + ")");
        }
        return events.save(event);
    }

    @CacheEvict(value = CacheConfig.PUBLISHED_EVENTS, allEntries = true)
    @Transactional
    public Event publish(Long id, String organizerEmail) {
        Event event = requireOwnedEvent(id, organizerEmail);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("Cannot publish a cancelled event");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    @CacheEvict(value = CacheConfig.PUBLISHED_EVENTS, allEntries = true)
    @Transactional
    public Event cancel(Long id, String organizerEmail) {
        Event event = requireOwnedEvent(id, organizerEmail);
        event.setStatus(EventStatus.CANCELLED);
        return event;
    }

    /** Admin moderation: force-cancel any event regardless of ownership. */
    @CacheEvict(value = CacheConfig.PUBLISHED_EVENTS, allEntries = true)
    @Transactional
    public Event adminCancel(Long id) {
        Event event = getDetails(id);
        event.setStatus(EventStatus.CANCELLED);
        return event;
    }

    private Event requireOwnedEvent(Long id, String organizerEmail) {
        Event event = getDetails(id);
        User organizer = userService.getByEmail(organizerEmail);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new ConflictException("You do not own this event");
        }
        return event;
    }
}

/*
 * ============================================================================
 * FILE ROLE: Business logic for events - creation, publish/cancel, cached listing.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - create(): validates end>start and that total tickets fit venue capacity,
 *     then builds a DRAFT event with its tiers (available = total).
 *   - publish()/cancel(): ownership-checked transitions; adminCancel() bypasses
 *     ownership for moderators.
 *   - listPublishedResponses(): the cached public listing (returns DTOs).
 *
 * TECHNICAL CONCEPTS
 *   - CACHING: @Cacheable(PUBLISHED_EVENTS) stores the listing; @CacheEvict on
 *     create/publish/cancel/adminCancel clears it so data is never stale.
 *   - It caches DTOs, not entities, to avoid lazy-loading issues on cached objects.
 *   - OWNERSHIP is a data-level check (does this organizer own this event?) done
 *     here in the service, complementing the role checks on the controller.
 *   - Business invariants (dates, capacity) raise ConflictException -> 409.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "rules keeper" for events. It handles:
 *   - CREATE: makes a new event, but first checks the dates make sense (it ends
 *     after it starts) and that you are not trying to sell more tickets than the
 *     venue can hold.
 *   - PUBLISH / CANCEL: put the event on sale or call it off - but only for YOUR
 *     own events (an organizer cannot touch someone else's).
 *   - LIST: gives the fast, "sticky note" (cached) list of on-sale events.
 *
 * Whenever an event changes (created, published, cancelled), it throws away that
 * cached list so visitors never see out-of-date information.
 * ============================================================================
 */
