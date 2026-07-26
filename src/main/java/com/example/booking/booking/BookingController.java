package com.example.booking.booking;

import com.example.booking.booking.dto.BookingResponse;
import com.example.booking.booking.dto.CreateBookingRequest;
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

/** Attendee-facing booking flow. All endpoints require an authenticated ATTENDEE/ADMIN. */
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest req,
                                                  Authentication auth) {
        Booking booking = bookingService.createBooking(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }

    @GetMapping
    public List<BookingResponse> mine(Authentication auth) {
        return bookingService.listMine(auth.getName()).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id, Authentication auth) {
        return BookingResponse.from(bookingService.getOwned(id, auth.getName()));
    }

    @PostMapping("/{id}/pay")
    public BookingResponse pay(@PathVariable Long id, Authentication auth) {
        return BookingResponse.from(bookingService.pay(id, auth.getName()));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Authentication auth) {
        return BookingResponse.from(bookingService.cancel(id, auth.getName()));
    }
}

/*
 * ============================================================================
 * FILE ROLE: REST endpoints for the attendee booking flow.
 * WHAT IT DOES: create (201), list mine, get one, pay, cancel.
 * TECHNICAL CONCEPTS: Class-level @PreAuthorize("hasAnyRole('ATTENDEE','ADMIN')")
 *   guards every method. The Authentication parameter provides the caller's email
 *   for ownership checks in the service. Entities never leave the controller -
 *   only BookingResponse DTOs.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The "front desk" for attendees: make a booking, see your bookings, pay for one,
 * or cancel one. Only attendees (and admins) can use these - an organizer cannot
 * book through here.
 * ============================================================================
 */
