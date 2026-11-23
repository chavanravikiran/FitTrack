package com.fittrack.security;

import com.fittrack.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("unit-test-secret-key-that-is-long-enough-32b".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(props(SECRET, Duration.ofMinutes(15)));
    }

    @Test
    void generatesTokenThatRoundTrips() {
        UserPrincipal principal = new UserPrincipal(42L, "jane@example.com", "x", true, List.of("ROLE_USER"));

        String token = jwtService.generateAccessToken(principal);
        UserPrincipal parsed = jwtService.parse(token).orElseThrow();

        assertThat(parsed.getId()).isEqualTo(42L);
        assertThat(parsed.getEmail()).isEqualTo("jane@example.com");
        assertThat(parsed.roleNames()).containsExactly("ROLE_USER");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(
                new UserPrincipal(1L, "a@b.com", "x", true, List.of("ROLE_USER")));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        String other = Base64.getEncoder().encodeToString("another-secret-key-which-is-also-32-bytes!!".getBytes());
        String token = new JwtService(props(other, Duration.ofMinutes(15)))
                .generateAccessToken(new UserPrincipal(1L, "a@b.com", "x", true, List.of()));

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(props(SECRET, Duration.ofSeconds(-1)));
        String token = shortLived.generateAccessToken(new UserPrincipal(1L, "a@b.com", "x", true, List.of()));

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void refusesToStartWithMissingOrShortSecret() {
        assertThatThrownBy(() -> new JwtService(props("", Duration.ofMinutes(1))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(Base64.getEncoder().encodeToString("short".getBytes()), Duration.ofMinutes(1))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AppProperties props(String secret, Duration ttl) {
        AppProperties p = new AppProperties();
        p.getJwt().setSecret(secret);
        p.getJwt().setAccessTokenTtl(ttl);
        return p;
    }
}
