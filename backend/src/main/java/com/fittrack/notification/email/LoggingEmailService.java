package com.fittrack.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Development fallback when no SMTP server is configured: writes the email to the log. */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ---------- EMAIL (mail disabled, logged only) ----------
                To: {}
                Subject: {}
                {}
                --------------------------------------------------------""", to, subject, body);
    }
}
