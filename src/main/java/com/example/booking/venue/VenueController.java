package com.example.booking.venue;

import com.example.booking.venue.dto.VenueRequest;
import com.example.booking.venue.dto.VenueResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    /** Public listing — anyone can browse venues. */
    @GetMapping
    public List<VenueResponse> list() {
        return venueService.findAll().stream().map(VenueResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VenueResponse get(@PathVariable Long id) {
        return VenueResponse.from(venueService.getById(id));
    }

    /** Only organizers and admins create venues. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest req) {
        Venue venue = venueService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(VenueResponse.from(venue));
    }
}

/*
 * ============================================================================
 * FILE ROLE: REST endpoints for venues.
 * WHAT IT DOES: Public GET list + get-by-id; POST create returns 201.
 * TECHNICAL CONCEPTS: Reads are public (permitAll GET in SecurityConfig); create
 *   is guarded by @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')") - METHOD
 *   SECURITY that runs before the method body. DTOs in/out (no entity exposure).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "front desk" for venues: anyone can look at the list of venues, but only
 * organizers and admins are allowed to add a new one.
 * ============================================================================
 */
