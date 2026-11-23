package com.fittrack.auth.service;

import com.fittrack.auth.entity.RefreshToken;
import com.fittrack.auth.repository.RefreshTokenRepository;
import com.fittrack.common.exception.UnauthorizedException;
import com.fittrack.common.util.TokenUtils;
import com.fittrack.config.AppProperties;
import com.fittrack.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AppProperties props;
    private final Clock clock;

    /** Result of a successful rotation: the owning user plus the new raw token. */
    public record Rotation(User user, String rawToken) {
    }

    @Transactional
    public String issue(User user, String userAgent) {
        return create(user, UUID.randomUUID(), userAgent);
    }

    /**
     * Validates and rotates a refresh token. Presenting an already-revoked token is treated as
     * theft: the entire token family is revoked and the caller must log in again.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String rawToken, String userAgent) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(TokenUtils.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Session expired. Please log in again."));

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected for user {} - revoking family {}",
                    current.getUser().getId(), current.getFamilyId());
            repository.revokeFamily(current.getFamilyId(), now);
            throw new UnauthorizedException("Session expired. Please log in again.");
        }
        if (current.isExpired(now) || !current.getUser().isEnabled()) {
            current.setRevokedAt(now);
            throw new UnauthorizedException("Session expired. Please log in again.");
        }

        current.setRevokedAt(now);
        String next = create(current.getUser(), current.getFamilyId(), userAgent);
        return new Rotation(current.getUser(), next);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(TokenUtils.sha256(rawToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    private String create(User user, UUID familyId, String userAgent) {
        String raw = TokenUtils.generateToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenUtils.sha256(raw));
        token.setFamilyId(familyId);
        token.setExpiresAt(clock.instant().plus(props.getJwt().getRefreshTokenTtl()));
        token.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length())));
        repository.save(token);
        return raw;
    }
}
