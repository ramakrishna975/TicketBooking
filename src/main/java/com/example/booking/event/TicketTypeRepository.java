package com.example.booking.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    /**
     * Load a ticket type for update within a booking transaction. OPTIMISTIC_FORCE_INCREMENT
     * bumps @Version so two concurrent seat-holds on the same tier can't both succeed —
     * the loser gets an OptimisticLockException and retries/fails cleanly.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<TicketType> findById(Long id);
}

/*
 * ============================================================================
 * FILE ROLE: Data-access for ticket tiers - the OPTIMISTIC LOCK read.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Overrides findById with @Lock(OPTIMISTIC_FORCE_INCREMENT) so that reading a
 *     tier during a booking transaction bumps its @Version.
 *
 * TECHNICAL CONCEPTS
 *   - OPTIMISTIC LOCKING assumes conflicts are rare: instead of locking rows,
 *     each row has a version that must match on write. FORCE_INCREMENT bumps the
 *     version even on a read, so two simultaneous seat-holds on the same tier both
 *     try to move the version from N to N+1 - the database lets only ONE commit,
 *     the other fails with an OptimisticLockException. This is what guarantees no
 *     double-selling, WITHOUT expensive pessimistic table locks.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "librarian" for ticket tiers, with a special rule used while booking.
 *
 * The problem it solves: two people click "book the last 2 seats" at the exact
 * same moment. We must not sell those seats twice.
 *
 * The trick: when this loads a tier to hold seats, it also "bumps a hidden
 * counter" on that tier. If two bookings happen at once, they both try to bump
 * the same counter - but the database only lets ONE of them succeed. The other
 * is safely told "sorry, try again," so no seat is ever sold twice. This is much
 * cheaper than locking the whole table.
 * ============================================================================
 */
