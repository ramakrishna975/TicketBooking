package com.example.booking.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}

/*
 * ============================================================================
 * FILE ROLE: Data-access interface for User.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Extends JpaRepository<User, Long> (free CRUD) and adds findByEmail and
 *     existsByEmail.
 *
 * TECHNICAL CONCEPTS
 *   - SPRING DATA JPA generates the implementation at runtime; you only declare
 *     the interface. Method names are parsed into queries ("findByEmail" ->
 *     WHERE email = ?), so no SQL is written for these.
 *   - Returning Optional<User> makes "might not exist" explicit and avoids nulls.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * This is the "librarian" for user records. You ask it things like "find the
 * user with this email" or "does this email already exist?", and it fetches the
 * answer from the database.
 *
 * The neat part: you do not write any database code. You just declare the method
 * with a clear name, and the framework figures out the query for you. Less code,
 * fewer mistakes.
 * ============================================================================
 */
