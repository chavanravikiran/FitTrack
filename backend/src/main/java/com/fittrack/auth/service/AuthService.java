package com.fittrack.auth.service;

import com.fittrack.auth.dto.AuthDtos.AuthResponse;
import com.fittrack.auth.dto.AuthDtos.AuthResult;
import com.fittrack.auth.dto.AuthDtos.ChangePasswordRequest;
import com.fittrack.auth.dto.AuthDtos.LoginRequest;
import com.fittrack.auth.dto.AuthDtos.RegisterRequest;
import com.fittrack.auth.dto.AuthDtos.UserSummary;
import com.fittrack.billing.entity.Subscription;
import com.fittrack.billing.service.SubscriptionService;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.DuplicateResourceException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.common.exception.UnauthorizedException;
import com.fittrack.config.AppProperties;
import com.fittrack.security.CurrentUserService;
import com.fittrack.security.JwtService;
import com.fittrack.security.UserPrincipal;
import com.fittrack.user.entity.RoleName;
import com.fittrack.user.entity.User;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.RoleRepository;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserService currentUserService;
    private final SubscriptionService subscriptionService;
    private final EmailVerificationService emailVerificationService;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public AuthResult register(RegisterRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.getRoles().add(roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing - check migrations")));
        user.setTermsAcceptedAt(clock.instant());
        user.setTermsVersion(props.getLegal().getTermsVersion());

        UserProfile profile = new UserProfile();
        profile.setFullName(request.fullName().trim());
        profile.setTimeZone(StringUtils.hasText(request.timeZone()) ? request.timeZone() : "UTC");
        user.setProfile(profile);

        user.setLastLoginAt(clock.instant());
        userRepository.save(user);
        subscriptionService.startForNewUser(user);
        emailVerificationService.sendVerification(user);
        return issueTokens(user, userAgent);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String userAgent) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        user.setLastLoginAt(clock.instant());
        return issueTokens(user, userAgent);
    }

    /** noRollbackFor keeps the family revocation done on token reuse even though we then return 401. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String rawRefreshToken, String userAgent) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken, userAgent);
        return new AuthResult(buildResponse(rotation.user()), rotation.rawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (StringUtils.hasText(rawRefreshToken)) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(currentUserService.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUserService.userId()));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Sign out every other device.
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public UserSummary currentUser() {
        User user = userRepository.findById(currentUserService.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUserService.userId()));
        return toSummary(user);
    }

    private AuthResult issueTokens(User user, String userAgent) {
        String refresh = refreshTokenService.issue(user, userAgent);
        return new AuthResult(buildResponse(user), refresh);
    }

    private AuthResponse buildResponse(User user) {
        String access = jwtService.generateAccessToken(UserPrincipal.from(user));
        return new AuthResponse(access, "Bearer", jwtService.accessTokenTtlSeconds(), toSummary(user));
    }

    UserSummary toSummary(User user) {
        UserProfile p = user.getProfile();
        Subscription sub = subscriptionService.current(user.getId());
        return new UserSummary(user.getId(), user.getEmail(),
                p == null ? null : p.getFullName(),
                user.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
                p != null && p.isOnboardingCompleted(),
                user.isEmailVerified(),
                sub.getPlan().getCode(),
                !sub.getPlan().isFree());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
