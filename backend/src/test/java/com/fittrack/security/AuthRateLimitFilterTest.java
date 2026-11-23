package com.fittrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittrack.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    @Test
    void blocksAfterLimitPerIpAndPath() throws Exception {
        AppProperties props = new AppProperties();
        props.getRateLimit().setAuthRequests(3);
        props.getRateLimit().setWindow(Duration.ofMinutes(5));
        AuthRateLimitFilter filter = new AuthRateLimitFilter(props, new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        for (int i = 0; i < 3; i++) {
            assertThat(call(filter, "/api/v1/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call(filter, "/api/v1/auth/login", "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();

        // Other IPs and other endpoints have their own budget; unrelated paths are never limited.
        assertThat(call(filter, "/api/v1/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/api/v1/auth/register", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/api/v1/me/profile", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse call(AuthRateLimitFilter filter, String path, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }
}
