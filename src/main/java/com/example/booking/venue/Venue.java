package com.example.booking.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "venue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    /** Physical seat/standing capacity of the venue. */
    @Column(nullable = false)
    private int capacity;

    @Version
    private long version;
}

/*
 * ============================================================================
 * FILE ROLE: The Venue JPA entity (table "venue").
 * WHAT IT DOES: Stores a location (name, address, city) and its CAPACITY plus a
 *   @Version column.
 * TECHNICAL CONCEPTS: Standard JPA entity. Capacity is the business ceiling used
 *   by EventService: an event's total ticket quantity may not exceed it. @Version
 *   gives optimistic locking for safe concurrent edits.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The blueprint for a "place" record: its name, address, city, and how many
 * people it can hold (capacity). Capacity matters later - an event cannot sell
 * more tickets than the room actually fits.
 * ============================================================================
 */
