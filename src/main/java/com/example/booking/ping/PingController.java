package com.example.booking.ping;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trivial liveness probe used to confirm the app boots and routes requests. */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("status", "ok", "time", Instant.now().toString());
    }
}

/*
 * ============================================================================
 * FILE ROLE: A trivial liveness probe.
 * WHAT IT DOES: GET /api/ping -> { status:"ok", time:... }.
 * TECHNICAL CONCEPTS: A public endpoint (permitAll) used to confirm the app boots
 *   and routes HTTP; complements Actuator's richer /actuator/health.
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * The simplest possible check: visit /api/ping and it replies "ok" with the
 * current time. It is just a heartbeat to confirm the app is alive and answering
 * requests - handy when setting things up.
 * ============================================================================
 */
