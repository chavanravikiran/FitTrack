package com.fittrack.auth.service;

import com.fittrack.auth.entity.PasswordResetToken;
import com.fittrack.auth.repository.PasswordResetTokenRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.util.TokenUtils;
import com.fittrack.config.AppProperties;
import com.fittrack.notification.email.EmailService;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties props;
    private final Clock clock;

    /**
     * Always completes silently so the response never reveals whether an email is registered.
     */
    @Transactional
    public void requestReset(String rawEmail) {
        Optional<User> userOpt = userRepository.findByEmail(AuthService.normalizeEmail(rawEmail));
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            log.debug("Password reset requested for unknown or disabled account");
            return;
        }
        User user = userOpt.get();
        Instant now = clock.instant();
        tokenRepository.invalidateAllForUser(user.getId(), now);

        String raw = TokenUtils.generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(TokenUtils.sha256(raw));
        token.setExpiresAt(now.plus(props.getPasswordReset().getTokenTtl()));
        tokenRepository.save(token);

        String link = props.getFrontendUrl() + "/auth/reset-password?token="
                + URLEncoder.encode(raw, StandardCharsets.UTF_8);
        long minutes = props.getPasswordReset().getTokenTtl().toMinutes();
        emailService.send(user.getEmail(), "Reset your FitTrack password", """
                Hi,

                We received a request to reset your FitTrack password.
                Open the link below to choose a new password (valid for %d minutes):

                %s

                If you did not request this, you can safely ignore this email.

                - The FitTrack team""".formatted(minutes, link));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessRuleException("This reset link is invalid or has expired. Please request a new one."));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);
        refreshTokenService.revokeAllForUser(user.getId());
    }
}
