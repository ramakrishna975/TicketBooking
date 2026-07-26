package com.example.booking.event;

import com.example.booking.user.User;
import com.example.booking.venue.Venue;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TicketType> ticketTypes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    public void addTicketType(TicketType tt) {
        tt.setEvent(this);
        ticketTypes.add(tt);
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = EventStatus.DRAFT;
        }
    }
}

/*
 * ============================================================================
 * FILE ROLE: The Event aggregate root (table "event").
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Links to a Venue and an organizer (User), has start/end times, a status,
 *     and OWNS a list of TicketTypes (cascade). addTicketType keeps both sides of
 *     the relationship consistent.
 *
 * TECHNICAL CONCEPTS
 *   - AGGREGATE ROOT (DDD): the Event and its TicketTypes are saved/removed
 *     together via cascade + orphanRemoval.
 *   - @ManyToOne(LAZY) for venue/organizer; @OneToMany(mappedBy="event") for tiers
 *     - "mappedBy" means TicketType owns the foreign key.
 *   - @Version for optimistic locking; @PrePersist defaults status to DRAFT and
 *     stamps createdAt.
 *   - Instants are stored in UTC (see application.yml hibernate.jdbc.time_zone).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The blueprint for an event, and the "hub" that ties everything together: which
 * venue it is at, which organizer owns it, when it starts and ends, its status,
 * and the list of ticket tiers it sells.
 *
 * Because the event "owns" its tiers, saving the event saves its tiers too - you
 * do not have to save each one separately.
 * ============================================================================
 */
