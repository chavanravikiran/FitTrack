package com.fittrack.auth.service;

import com.fittrack.auth.repository.PasswordResetTokenRepository;
import com.fittrack.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Nightly purge of expired auth tokens (kept 1 day past expiry for reuse detection/auditing). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.fittrack.auth.repository.EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(1));
        int refresh = refreshTokenRepository.deleteExpiredBefore(cutoff);
        int reset = passwordResetTokenRepository.deleteExpiredBefore(cutoff);
        int verify = emailVerificationTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Token cleanup removed {} refresh, {} reset and {} verification tokens", refresh, reset, verify);
    }
}
