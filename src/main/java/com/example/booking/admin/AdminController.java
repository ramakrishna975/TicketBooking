package com.example.booking.admin;

import com.example.booking.event.EventService;
import com.example.booking.event.dto.EventResponse;
import com.example.booking.user.UserService;
import com.example.booking.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin moderation surface. Locked to ADMIN by both URL rules and method security. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final EventService eventService;

    public AdminController(UserService userService, EventService eventService) {
        this.userService = userService;
        this.eventService = eventService;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/users/{id}/disable")
    public UserResponse disableUser(@PathVariable Long id) {
        return UserResponse.from(userService.setEnabled(id, false));
    }

    @PostMapping("/users/{id}/enable")
    public UserResponse enableUser(@PathVariable Long id) {
        return UserResponse.from(userService.setEnabled(id, true));
    }

    @PostMapping("/events/{id}/cancel")
    public EventResponse cancelEvent(@PathVariable Long id) {
        return EventResponse.from(eventService.adminCancel(id));
    }
}

/*
 * ============================================================================
 * FILE ROLE: Admin-only moderation endpoints.
 * WHAT IT DOES: List users; disable/enable a user; force-cancel any event.
 * ----------------------------------------------------------------------------
 * TECHNICAL CONCEPTS
 *   - Guarded twice (defence in depth): the URL rule /api/admin/** requires ADMIN
 *     in SecurityConfig, and class-level @PreAuthorize("hasRole('ADMIN')") repeats
 *     it at the method layer.
 *   - Delegates to UserService/EventService; force-cancel bypasses ownership
 *     (adminCancel) because moderators act across all organizers.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The staff-only control panel: see all users, switch a user on or off, and
 * force-cancel any event (even one you did not create). It is double-locked so
 * that only admins can reach it.
 * ============================================================================
 */
