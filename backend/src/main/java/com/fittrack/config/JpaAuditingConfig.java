package com.fittrack.config;

import com.fittrack.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    /** Records the authenticated user's id in created_by / updated_by. */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                return Optional.of(principal.getId());
            }
            return Optional.empty();
        };
    }

    @Bean
    public DateTimeProvider utcDateTimeProvider(java.time.Clock clock) {
        return () -> Optional.of(Instant.now(clock));
    }
}
