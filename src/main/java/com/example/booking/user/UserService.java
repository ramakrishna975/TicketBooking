package com.example.booking.user;

import com.example.booking.common.error.ConflictException;
import com.example.booking.common.error.NotFoundException;
import com.example.booking.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            // Admins are provisioned out-of-band, never via open self-registration.
            throw new ConflictException("Cannot self-register as ADMIN");
        }
        if (users.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .displayName(req.displayName())
                .role(req.role())
                .enabled(true)
                .build();
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> NotFoundException.of("User", email));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    @Transactional(readOnly = true)
    public java.util.List<User> findAll() {
        return users.findAll();
    }

    /** Admin moderation: enable/disable a user account. */
    @Transactional
    public User setEnabled(Long id, boolean enabled) {
        User user = getById(id);
        user.setEnabled(enabled);
        return user;
    }
}

/*
 * ============================================================================
 * FILE ROLE: Business logic for user accounts.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - register(): blocks self-registering as ADMIN, blocks duplicate emails,
 *     BCrypt-hashes the password, saves the user.
 *   - getByEmail/getById: lookups that throw NotFoundException (-> 404) if absent.
 *   - findAll / setEnabled: used by admin moderation.
 *
 * TECHNICAL CONCEPTS
 *   - @Service marks a business-logic bean; @Transactional wraps each method in a
 *     database transaction (read-only where nothing is written, which lets the DB
 *     optimise).
 *   - Passwords are never stored in plain text - the injected PasswordEncoder
 *     (BCrypt) hashes them before persistence.
 *   - Enforcing "no ADMIN via public signup" here keeps a security rule in the
 *     domain layer, independent of the HTTP surface.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is the "rules keeper" for accounts - the place where the real thinking
 * about users happens.
 *
 * When someone signs up, it decides:
 *   - you cannot sign up as an Admin (that is done privately, for safety),
 *   - you cannot reuse an email that already exists,
 *   - your password is scrambled before it is ever saved.
 *
 * It also has helpers to fetch a user, and to switch an account on or off (used
 * by admins). The controllers stay short and simple because the important rules
 * live here.
 * ============================================================================
 */
