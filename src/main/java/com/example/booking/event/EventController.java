package com.example.booking.event;

import com.example.booking.event.dto.CreateEventRequest;
import com.example.booking.event.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** Public listing of PUBLISHED events (cached read path). */
    @GetMapping
    public List<EventResponse> listPublished() {
        return eventService.listPublishedResponses();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return EventResponse.from(eventService.getDetails(id));
    }

    /** Events owned by the calling organizer (any status). */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public List<EventResponse> mine(Authentication auth) {
        return eventService.listByOrganizer(auth.getName()).stream().map(EventResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest req,
                                               Authentication auth) {
        Event event = eventService.create(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse publish(@PathVariable Long id, Authentication auth) {
        return EventResponse.from(eventService.publish(id, auth.getName()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse cancel(@PathVariable Long id, Authentication auth) {
        return EventResponse.from(eventService.cancel(id, auth.getName()));
    }
}

/*
 * ============================================================================
 * FILE ROLE: REST endpoints for events.
 * WHAT IT DOES: Public listing (cached) + detail; organizer-only create, mine,
 *   publish, cancel.
 * TECHNICAL CONCEPTS: Public GETs (permitAll) vs @PreAuthorize('ORGANIZER','ADMIN')
 *   writes. The Authentication parameter supplies the caller's email so the
 *   service can enforce ownership. Returns EventResponse DTOs only.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "front desk" for events. Anyone can browse the on-sale list and view a
 * single event. Only organizers and admins can create an event, publish it,
 * cancel it, or see their own "my events" list.
 * ============================================================================
 */
