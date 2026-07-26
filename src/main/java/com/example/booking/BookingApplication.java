package com.example.booking;

import com.example.booking.config.BookingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(BookingProperties.class)
public class BookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}

/*
 * ============================================================================
 * FILE ROLE: Application entry point / bootstrap.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - Declares the Spring Boot application and starts the embedded web server
 *     via SpringApplication.run(...).
 *   - Turns on three cross-cutting, app-wide capabilities:
 *       @EnableCaching   -> activates the Caffeine @Cacheable/@CacheEvict on the
 *                           event-listing read path (see config/CacheConfig).
 *       @EnableScheduling-> activates the @Scheduled background jobs
 *                           (booking/BookingScheduler: hold expiry + reminders).
 *       @EnableConfigurationProperties(BookingProperties) -> binds the booking.*
 *                           config tree into a typed object.
 *
 * TECHNICAL CONCEPTS
 *   - @SpringBootApplication is a meta-annotation = @Configuration +
 *     @EnableAutoConfiguration + @ComponentScan. Component scanning starts at
 *     THIS package (com.example.booking) and discovers every @Component/@Service/
 *     @Repository/@RestController below it. That is why package-by-feature works
 *     with zero extra wiring.
 *   - Auto-configuration inspects the classpath and configures sensible defaults
 *     (Tomcat, Jackson, DataSource, JPA, Security) which we then override in the
 *     application-*.yml profiles.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * Think of this file as the POWER BUTTON of the whole app.
 *
 * When you run the program, Java looks for a starting point called "main" - it
 * is right here. That one line inside main() starts everything:
 *   1) it switches on a small built-in web server so the app can receive
 *      requests from browsers/apps, and
 *   2) it automatically finds all the other code you wrote (the controllers,
 *      services, database helpers) and connects them together for you. You do
 *      not wire them by hand.
 *
 * The small labels on top (@EnableCaching, @EnableScheduling, ...) are like ON
 * switches:
 *   - caching     = "remember common answers so we are faster",
 *   - scheduling  = "run some jobs automatically on a timer",
 *   - config      = "read settings from our settings file".
 *
 * You almost never edit this file. It just turns the machine on.
 * ============================================================================
 */
