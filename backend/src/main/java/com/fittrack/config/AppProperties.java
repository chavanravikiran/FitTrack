package com.fittrack.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Strongly-typed application settings bound from the {@code app.*} namespace. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank
    private String frontendUrl;

    @Valid
    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();
    private final PasswordReset passwordReset = new PasswordReset();
    private final Cors cors = new Cors();
    private final Storage storage = new Storage();
    private final Mail mail = new Mail();
    private final Admin admin = new Admin();
    private final Billing billing = new Billing();
    private final EmailVerification emailVerification = new EmailVerification();
    private final Legal legal = new Legal();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Billing {
        /** Days of Premium given to every new account; 0 disables the trial. */
        private int trialDays = 7;
        private String trialPlanCode = "PREMIUM";
        private String productName = "FitTrack";
        private final Razorpay razorpay = new Razorpay();
    }

    @Getter
    @Setter
    public static class Razorpay {
        private String keyId;
        private String keySecret;
        private String webhookSecret;
        private String apiBaseUrl = "https://api.razorpay.com/v1";

        public boolean isConfigured() {
            return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        }
    }

    @Getter
    @Setter
    public static class EmailVerification {
        private Duration tokenTtl = Duration.ofHours(48);
    }

    @Getter
    @Setter
    public static class Legal {
        /** Bump when Terms/Privacy change materially; stored with each acceptance. */
        private String termsVersion = "2026-11";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        /** Max requests per client IP per window on sensitive auth endpoints. */
        private int authRequests = 10;
        private Duration window = Duration.ofMinutes(5);
    }

    @Getter
    @Setter
    public static class Jwt {
        /** Base64 encoded HMAC-SHA key, at least 256 bits. */
        private String secret;
        @NotNull
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        @NotNull
        private Duration refreshTokenTtl = Duration.ofDays(7);
        private String issuer = "fittrack";
    }

    @Getter
    @Setter
    public static class Cookie {
        private boolean secure = true;
        private String sameSite = "Strict";
    }

    @Getter
    @Setter
    public static class PasswordReset {
        private Duration tokenTtl = Duration.ofMinutes(30);
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Storage {
        /** LOCAL (disk, development) or S3. */
        private String type = "LOCAL";
        private String root = "./data/uploads";
        /** Optional CDN/public base URL for catalog media (e.g. a CloudFront distribution in front of the bucket). */
        private String publicBaseUrl;
        private Duration presignTtl = Duration.ofHours(1);
        private long maxVideoBytes = 500L * 1024 * 1024;
        private final S3 s3 = new S3();
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region = "ap-south-1";
        /** Optional key prefix, e.g. "prod/". */
        private String prefix = "";
        /** Optional custom endpoint (LocalStack/MinIO). Leave empty for AWS. */
        private String endpoint;
        private boolean pathStyle = false;
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = false;
        private String from = "no-reply@fittrack.local";
    }

    @Getter
    @Setter
    public static class Admin {
        private String email;
        private String password;
    }
}
