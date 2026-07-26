package com.example.booking.security;

import com.example.booking.user.User;
import com.example.booking.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Bridges our {@link User} aggregate to Spring Security's {@link UserDetails}. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));
        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getEmail())
                .password(u.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                .disabled(!u.isEnabled())
                .build();
    }
}

/*
 * ============================================================================
 * FILE ROLE: Adapts our User entity to Spring Security's UserDetails.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - loadUserByUsername(email): loads the User from the database and returns a
 *     Spring Security user carrying the BCrypt password hash and a ROLE_<role>
 *     authority; disabled accounts are flagged.
 *
 * TECHNICAL CONCEPTS
 *   - UserDetailsService is the SPI Spring Security uses to look up accounts. It
 *     is only used at LOGIN time by the DaoAuthenticationProvider (which compares
 *     the submitted password against the stored hash). After login, requests use
 *     the JWT filter instead, so this is not called per-request.
 *   - Spring maps roles to authorities named "ROLE_*"; hasRole('ADMIN') checks
 *     for authority "ROLE_ADMIN". That prefixing is why we store "ROLE_"+role.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This little helper is used only at the MOMENT OF LOGIN.
 *
 * When you type your email and password, the app must fetch your stored account
 * to compare passwords. This file is the helper that looks you up by email in the
 * database and hands over your details (your scrambled password and your role) to
 * the part of the framework that actually checks the password.
 *
 * After you are logged in, this is not used again - from then on your pass (the
 * token) does the talking.
 * ============================================================================
 */
