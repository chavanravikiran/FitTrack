package com.fittrack.security;

import com.fittrack.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Issues and validates short-lived HS256 access tokens. */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey key;
    private final Duration accessTtl;
    private final String issuer;

    public JwtService(AppProperties props) {
        String secret = props.getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must decode to at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtl = props.getJwt().getAccessTokenTtl();
        this.issuer = props.getJwt().getIssuer();
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(principal.getId()))
                .claim(CLAIM_EMAIL, principal.getEmail())
                .claim(CLAIM_ROLES, principal.roleNames())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /** Returns the principal encoded in a valid token, or empty if the token is invalid or expired. */
    public Optional<UserPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(CLAIM_ROLES, List.class);
            return Optional.of(new UserPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    "",
                    true,
                    roles == null ? List.of() : roles));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
