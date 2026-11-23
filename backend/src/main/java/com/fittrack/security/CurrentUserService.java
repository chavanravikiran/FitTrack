package com.fittrack.security;

import com.fittrack.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Single place to resolve "who is calling" so every service scopes data to the current user. */
@Component
public class CurrentUserService {

    public UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("Authentication required");
    }

    public Long userId() {
        return principal().getId();
    }
}
