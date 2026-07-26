package com.example.booking.config;

import com.example.booking.user.Role;
import com.example.booking.user.User;
import com.example.booking.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dev-only convenience: seed a default ADMIN account (admin@x.com / password1) on startup,
 * because ADMIN cannot be created via public self-registration. This makes the admin
 * endpoints usable immediately from Swagger/Postman on the dev (H2) profile.
 * NOT active on the postgres profile — real admins are provisioned deliberately there.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String ADMIN_EMAIL = "admin@x.com";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DevDataInitializer(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        users.save(User.builder()
                .email(ADMIN_EMAIL)
                .passwordHash(encoder.encode("password1"))
                .displayName("Default Admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        log.info("[dev] Seeded default admin account: {} / password1", ADMIN_EMAIL);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Dev-only startup seeder for a default ADMIN account.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - On the dev (H2) profile only, creates admin@x.com / password1 (role ADMIN)
 *     at startup if it does not already exist, so admin endpoints are usable
 *     immediately from Swagger/Postman.
 *
 * TECHNICAL CONCEPTS
 *   - ADMIN cannot be created via public self-registration (UserService blocks
 *     it), so an admin must be provisioned out-of-band. This seeder is that
 *     mechanism for local development.
 *   - CommandLineRunner.run(...) executes once after the context is ready - the
 *     standard Spring hook for startup tasks.
 *   - @Profile("dev") means the bean only exists on the dev profile; the postgres
 *     profile creates no admin automatically (real admins are provisioned
 *     deliberately). The password is BCrypt-hashed via the shared PasswordEncoder.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * For safety, an ADMIN account cannot be created from the public sign-up form.
 * But when you are testing on your own laptop, you still need one to try the
 * admin features.
 *
 * So this file quietly creates a ready-made admin (email: admin@x.com,
 * password: password1) the moment the app starts - but ONLY in local/testing
 * mode (the "dev" profile). On the real server it does nothing, because there an
 * admin should be created carefully by hand, not automatically.
 *
 * Result: on your machine you can log in as admin immediately and try everything.
 * ============================================================================
 */
