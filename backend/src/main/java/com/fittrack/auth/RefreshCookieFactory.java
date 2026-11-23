package com.fittrack.auth;

import com.fittrack.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Builds the httpOnly refresh-token cookie, scoped to the auth endpoints only. */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "ft_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AppProperties props;

    public ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(props.getJwt().getRefreshTokenTtl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .sameSite(props.getCookie().getSameSite())
                .path(COOKIE_PATH);
    }
}
