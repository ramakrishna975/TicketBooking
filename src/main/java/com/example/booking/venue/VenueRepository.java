package com.example.booking.venue;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
}

/*
 * ============================================================================
 * FILE ROLE: Data-access interface for Venue (plain JpaRepository CRUD).
 * TECHNICAL CONCEPTS: Spring Data provides save/findById/findAll/delete with no
 *   implementation code; the interface is enough.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "librarian" for venues - save, find, and list them - with no database code
 * to write. Just declaring this interface is enough.
 * ============================================================================
 */
