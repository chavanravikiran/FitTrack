package com.fittrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Injectable clock so time-dependent logic (tokens, "today", subscriptions) is unit-testable.
 * Ticks in microseconds to match PostgreSQL TIMESTAMPTZ precision, so values returned right after a write
 * are identical to what a later read returns.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
