package com.fittrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittrack.common.api.ApiResponse;
import com.fittrack.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, per-IP limiter for credential and email-sending endpoints (brute force / spam protection).
 * In-memory: correct for a single instance. Behind a load balancer use a shared store (e.g. Redis) and make sure
 * the proxy sets X-Forwarded-For with {@code server.forward-headers-strategy=native}.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification");

    private record Window(long startMillis, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AppProperties.RateLimit config;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuthRateLimitFilter(AppProperties props, ObjectMapper objectMapper, Clock clock) {
        this.config = props.getRateLimit();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !config.isEnabled() || !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        long now = clock.instant().toEpochMilli(); // not clock.millis(): Clock.tick(<1ms).millis() divides by zero
        long windowMs = config.getWindow().toMillis();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();

        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.startMillis() >= windowMs ? new Window(now, 1) : new Window(old.startMillis(), old.count() + 1));

        if (w.count() > config.getAuthRequests()) {
            long retryAfter = Math.max(1, (w.startMillis() + windowMs - now) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(
                    "Too many attempts. Please wait a few minutes and try again.", null, request.getRequestURI()));
            return;
        }
        if (windows.size() > 50_000) {
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= windowMs);
        }
        chain.doFilter(request, response);
    }
}
