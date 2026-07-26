package com.example.booking.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine cache manager. We cache exactly ONE thing: the public published-event
 * listing ({@code publishedEvents}). That endpoint is the highest-traffic, read-mostly
 * path (every anonymous visitor hits it) and its data changes only when an organizer
 * publishes/cancels/creates — at which point the service evicts the cache. Everything
 * else (bookings, seat counts) is write-heavy and correctness-sensitive, so it is
 * deliberately NOT cached.
 */
@Configuration
public class CacheConfig {

    public static final String PUBLISHED_EVENTS = "publishedEvents";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(PUBLISHED_EVENTS);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5)));
        return manager;
    }
}

/*
 * ============================================================================
 * FILE ROLE: Defines the ONE cache used by the app (Caffeine).
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Creates a CaffeineCacheManager with a single cache named "publishedEvents",
 *     capped at 500 entries and expiring 5 minutes after write.
 *
 * TECHNICAL CONCEPTS
 *   - CACHING is "remember an expensive/frequent answer to avoid recomputing it."
 *     Spring's caching abstraction (@Cacheable/@CacheEvict) sits on top of a
 *     CacheManager; here the implementation is Caffeine, a fast in-memory cache.
 *   - We deliberately cache ONLY the public published-event listing: it is read
 *     constantly (every anonymous visitor) and changes rarely. Write-heavy,
 *     correctness-sensitive data (seat counts, bookings) is intentionally NOT
 *     cached to avoid overselling.
 *   - EVICTION: EventService clears this cache on create/publish/cancel so stale
 *     data is never served; expire-after-write is a secondary safety net.
 *   - We cache DTOs (not JPA entities) to avoid LazyInitializationException on
 *     detached cached objects.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * Imagine a question lots of visitors ask again and again, and the answer barely
 * changes - like "which events are on sale right now?". Asking the database every
 * single time is wasteful.
 *
 * So we keep the answer on a "sticky note" for a few minutes and hand that out
 * instead. That sticky-note system is called a CACHE. This file sets it up and
 * says: keep just ONE note (the events list), throw it away after 5 minutes, and
 * never keep more than 500 notes.
 *
 * We are careful to do this for ONLY that one list. We do NOT cache things that
 * change often (like how many seats are left), because a stale sticky note there
 * could show wrong numbers and let two people buy the same seat.
 * ============================================================================
 */
