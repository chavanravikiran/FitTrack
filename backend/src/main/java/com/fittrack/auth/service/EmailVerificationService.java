package com.fittrack.auth.service;

import com.fittrack.auth.entity.EmailVerificationToken;
import com.fittrack.auth.repository.EmailVerificationTokenRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.common.util.TokenUtils;
import com.fittrack.config.AppProperties;
import com.fittrack.notification.email.EmailService;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

/** Email ownership check. Unverified users can use the app; verification is required before paying. */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CurrentUserService currentUser;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public void sendVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        Instant now = clock.instant();
        tokenRepository.invalidateAllForUser(user.getId(), now);

        String raw = TokenUtils.generateToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(TokenUtils.sha256(raw));
        token.setExpiresAt(now.plus(props.getEmailVerification().getTokenTtl()));
        tokenRepository.save(token);

        String link = props.getFrontendUrl() + "/verify-email?token=" + URLEncoder.encode(raw, StandardCharsets.UTF_8);
        emailService.send(user.getEmail(), "Confirm your FitTrack email", """
                Welcome to FitTrack!

                Please confirm your email address by opening this link:

                %s

                The link is valid for %d hours. If you didn't create a FitTrack account, you can ignore this email.

                - The FitTrack team""".formatted(link, props.getEmailVerification().getTokenTtl().toHours()));
    }

    @Transactional
    public void resendForCurrentUser() {
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUser.userId()));
        if (user.isEmailVerified()) {
            throw new BusinessRuleException("Your email is already verified");
        }
        sendVerification(user);
    }

    @Transactional
    public void verify(String rawToken) {
        Instant now = clock.instant();
        EmailVerificationToken token = tokenRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessRuleException("This verification link is invalid or has expired. Request a new one from the app."));
        token.setUsedAt(now);
        token.getUser().setEmailVerified(true);
    }
}
