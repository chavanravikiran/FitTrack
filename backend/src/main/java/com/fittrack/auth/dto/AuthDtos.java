package com.fittrack.auth.dto;

import com.fittrack.common.validation.StrongPassword;
import com.fittrack.common.validation.ValidTimeZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request/response contracts for the authentication endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @StrongPassword String password,
            @ValidTimeZone @Schema(example = "Asia/Kolkata") String timeZone,
            @NotNull @AssertTrue(message = "You must accept the Terms of Service and Privacy Policy") Boolean acceptTerms) {
    }

    public record VerifyEmailRequest(@NotBlank @Size(max = 100) String token) {
    }

    public record DeleteAccountRequest(
            @NotBlank @Size(max = 64) String password,
            @NotBlank @Pattern(regexp = "DELETE", message = "type DELETE to confirm") String confirmation) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 64) String password) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 100) String token,
            @NotBlank @StrongPassword String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 64) String currentPassword,
            @NotBlank @StrongPassword String newPassword) {
    }

    public record UserSummary(Long id, String email, String fullName, List<String> roles,
                              boolean onboardingCompleted, boolean emailVerified, String planCode, boolean premium) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {
    }

    /** Internal result: the refresh token travels in an httpOnly cookie, never in the JSON body. */
    public record AuthResult(AuthResponse response, String refreshToken) {
    }
}
