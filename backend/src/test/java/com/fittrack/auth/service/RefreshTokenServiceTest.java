package com.fittrack.auth.service;

import com.fittrack.auth.entity.RefreshToken;
import com.fittrack.auth.repository.RefreshTokenRepository;
import com.fittrack.common.exception.UnauthorizedException;
import com.fittrack.common.util.TokenUtils;
import com.fittrack.config.AppProperties;
import com.fittrack.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setRefreshTokenTtl(Duration.ofDays(7));
        service = new RefreshTokenService(repository, props, Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        user.setId(5L);
        user.setEnabled(true);
    }

    @Test
    void issueStoresOnlyTheHash() {
        String raw = service.issue(user, "JUnit");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(TokenUtils.sha256(raw)).isNotEqualTo(raw);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void rotateRevokesCurrentAndIssuesNewTokenInSameFamily() {
        RefreshToken current = token("raw-1", null, NOW.plusSeconds(3600));
        when(repository.findByTokenHash(TokenUtils.sha256("raw-1"))).thenReturn(Optional.of(current));

        RefreshTokenService.Rotation rotation = service.rotate("raw-1", "JUnit");

        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(rotation.rawToken()).isNotEqualTo("raw-1");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo(current.getFamilyId());
    }

    @Test
    void reusingRevokedTokenRevokesWholeFamily() {
        RefreshToken stolen = token("raw-old", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(repository.findByTokenHash(TokenUtils.sha256("raw-old"))).thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> service.rotate("raw-old", "attacker"))
                .isInstanceOf(UnauthorizedException.class);
        verify(repository).revokeFamily(stolen.getFamilyId(), NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void expiredTokenIsRejected() {
        RefreshToken expired = token("raw-exp", null, NOW.minusSeconds(1));
        when(repository.findByTokenHash(TokenUtils.sha256("raw-exp"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("raw-exp", null)).isInstanceOf(UnauthorizedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("nope", null)).isInstanceOf(UnauthorizedException.class);
    }

    private RefreshToken token(String raw, Instant revokedAt, Instant expiresAt) {
        RefreshToken t = new RefreshToken();
        t.setUser(user);
        t.setTokenHash(TokenUtils.sha256(raw));
        t.setFamilyId(UUID.randomUUID());
        t.setRevokedAt(revokedAt);
        t.setExpiresAt(expiresAt);
        return t;
    }
}
