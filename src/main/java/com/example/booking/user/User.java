package com.example.booking.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

/*
 * ============================================================================
 * FILE ROLE: The User JPA entity (database table "app_user").
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Maps an account to a row: unique email, BCrypt password hash, display name,
 *     role, enabled flag, created-at timestamp, and an optimistic-lock version.
 *   - @PrePersist stamps createdAt automatically on first save.
 *
 * TECHNICAL CONCEPTS
 *   - JPA/HIBERNATE ENTITY: @Entity + @Table map the class to a table; @Id +
 *     @GeneratedValue(IDENTITY) delegate primary-key generation to the DB.
 *   - @Enumerated(STRING) stores the role as readable text ("ADMIN"), not a
 *     fragile ordinal number.
 *   - @Version enables OPTIMISTIC LOCKING (see TicketType/Booking for where it
 *     matters most): concurrent updates that collide are rejected safely.
 *   - Lombok (@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor)
 *     generates boilerplate; JPA requires the no-arg constructor.
 *   - The table is "app_user" because "user" is a reserved word in many databases.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This file describes what an ACCOUNT looks like and how it is saved as one row
 * in the database. Think of it as the blueprint for a "user record."
 *
 * It lists the pieces an account has: email, the scrambled password (never the
 * real one), a display name, a role, whether the account is switched on, and when
 * it was created.
 *
 * The small "version" number is a safety mechanism: if two people try to change
 * the same account at the exact same moment, it prevents them from quietly
 * overwriting each other. The table is named "app_user" because plain "user" is a
 * reserved word in many databases.
 * ============================================================================
 */
