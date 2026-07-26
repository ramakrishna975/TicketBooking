package com.example.booking.venue;

import com.example.booking.common.error.NotFoundException;
import com.example.booking.venue.dto.VenueRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VenueService {

    private final VenueRepository venues;

    public VenueService(VenueRepository venues) {
        this.venues = venues;
    }

    @Transactional(readOnly = true)
    public List<Venue> findAll() {
        return venues.findAll();
    }

    @Transactional(readOnly = true)
    public Venue getById(Long id) {
        return venues.findById(id).orElseThrow(() -> NotFoundException.of("Venue", id));
    }

    @Transactional
    public Venue create(VenueRequest req) {
        Venue venue = Venue.builder()
                .name(req.name())
                .address(req.address())
                .city(req.city())
                .capacity(req.capacity())
                .build();
        return venues.save(venue);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Business logic for venues (list, get-by-id, create).
 * WHAT IT DOES: getById throws NotFoundException (-> 404) when absent; create
 *   builds and saves a Venue from the request.
 * TECHNICAL CONCEPTS: @Transactional boundaries; read-only for queries. Keeps
 *   persistence details out of the controller.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "rules keeper" for venues: fetch one (and say "not found" nicely if it is
 * missing), list them all, or create a new one. It keeps the database details
 * away from the front desk (the controller).
 * ============================================================================
 */
