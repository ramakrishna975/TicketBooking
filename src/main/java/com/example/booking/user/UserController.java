package com.example.booking.user;

import com.example.booking.user.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Profile of the currently authenticated user (any role). */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        return UserResponse.from(user);
    }
}

/*
 * ============================================================================
 * FILE ROLE: REST endpoint for the current user's profile.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - GET /api/users/me: returns the authenticated caller's profile.
 *
 * TECHNICAL CONCEPTS
 *   - The Authentication argument is injected by Spring Security from the
 *     SecurityContext (populated by JwtAuthenticationFilter); getName() is the
 *     email stored as the JWT subject.
 *   - Returns a UserResponse DTO (never the entity), so the password hash and
 *     internal fields are never exposed.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * One small window: "who am I?" You send your pass (token), and it returns your
 * own profile. Apps use this to show things like "Hi, Ann" right after you log
 * in. It only ever tells you about yourself.
 * ============================================================================
 */
